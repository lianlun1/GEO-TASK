package com.lianlun.android.geotask

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.text.Editable
import android.text.SpannableString
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse
import com.google.android.libraries.places.api.net.PlacesClient
import java.io.IOException
import java.lang.Exception
import java.util.*

interface InitializeHelperInterface {

    var placesClient: PlacesClient
    var mFusedLocationProviderClient: FusedLocationProviderClient

    fun init(autoCompleteTextView: AutoCompleteTextView,
             context: Context,
             gps: ImageView,
             DEFAULT_ZOOM: Float){

        placesClient = Places.createClient(context)

        autoCompleteTextView.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                if(s.length > 2) {
                    autocompleteHelper(autoCompleteTextView, context, DEFAULT_ZOOM)
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        })

        autoCompleteTextView.setOnEditorActionListener(TextView.OnEditorActionListener { textView, actionId, keyEvent ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH
                || actionId == EditorInfo.IME_ACTION_DONE
                || keyEvent.action == KeyEvent.ACTION_DOWN
                || keyEvent.action == KeyEvent.KEYCODE_ENTER
            ) {

                geoLocate(autoCompleteTextView, context, DEFAULT_ZOOM)
            }
            false
        })

        gps.setOnClickListener(View.OnClickListener {
            getDeviceLocation()
        })
        hideSoftKeyboard()
    }

    private fun autocompleteHelper(
        autoCompleteTextView: AutoCompleteTextView,
        context: Context,
        DEFAULT_ZOOM: Float
    ){
        var places = emptyArray<SpannableString>()

        var token: AutocompleteSessionToken = AutocompleteSessionToken.newInstance()
        var request: FindAutocompletePredictionsRequest = FindAutocompletePredictionsRequest
            .builder()
            .setTypeFilter(TypeFilter.ADDRESS)
            .setSessionToken(token)
            .setQuery(autoCompleteTextView.text.toString())
            .build()

        placesClient.findAutocompletePredictions(request)
            .addOnSuccessListener { response: FindAutocompletePredictionsResponse ->
                for(prediction in response.autocompletePredictions){
                    places += prediction.getPrimaryText(null)
                }

                var adapter = ArrayAdapter<SpannableString>(
                    context, R.layout.support_simple_spinner_dropdown_item, places)
                autoCompleteTextView.setAdapter(adapter)

                autoCompleteTextView.onItemClickListener = AdapterView.OnItemClickListener { parent,
                                                                                             _,
                                                                                             position,
                                                                                             id ->
//                    val selectedItem = parent.getItemAtPosition(position) as SpannableString
                    geoLocate(autoCompleteTextView, context, DEFAULT_ZOOM)
                }

//                geoLocate(autoCompleteTextView, context, DEFAULT_ZOOM)
            }.addOnFailureListener {exception: Exception? ->
                if (exception is ApiException){ }
            }
    }

    private fun geoLocate(
        autoCompleteTextView: AutoCompleteTextView,
        context: Context,
        DEFAULT_ZOOM: Float
    ){

        var searchString = autoCompleteTextView.text.toString()

        var geocoder = Geocoder(context)
        var list: List<Address> = ArrayList()
        try {
            list = geocoder.getFromLocationName(searchString, 1)
        } catch (e: IOException){ }
        if (list.isNotEmpty()){
            var address: Address = list[0]

            moveCamera(
                LatLng(address.latitude, address.longitude), DEFAULT_ZOOM,
                address.getAddressLine(0)
            )
        }
    }

    fun getDeviceLocation(){}

    fun moveCamera(latLng: LatLng, zoom: Float, title: String){}

    fun hideSoftKeyboard(){}
}