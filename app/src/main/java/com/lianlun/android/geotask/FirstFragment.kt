package com.lianlun.android.geotask

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import android.widget.*
import android.widget.TextView.OnEditorActionListener
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices.getFusedLocationProviderClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.Task
import com.lianlun.android.geotask.R.layout.fragment_first
import kotlinx.android.synthetic.main.fragment_first.*
import java.io.IOException
import java.util.*
import android.text.Editable
import android.text.SpannableString

import android.text.TextWatcher
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse
import com.google.android.libraries.places.api.net.PlacesClient
import java.lang.Exception
import java.lang.StringBuilder

class FirstFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap

    private lateinit var mContext: Context

    private val FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION
    private val COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION
    private val LOCATION_PERMISSION_REQUEST_CODE = 1234
    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient
    private val DEFAULT_ZOOM = 15f
    private val TAG = "FirstFragment"
    private var mLocationPermissionGranted = false
    private lateinit var enteredText: String
    private lateinit var placesClient: PlacesClient

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(fragment_first, container, false)

        getLocationPermission()

        val apiKey = mContext.getString(R.string.google_maps_key)

        if (!Places.isInitialized()){
            Places.initialize(mContext, apiKey)
        }
        return view
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        Toast.makeText(mContext, "Map is ready", Toast.LENGTH_SHORT).show()

        if(mLocationPermissionGranted){
            getDeviceLocation()

            if(ActivityCompat.checkSelfPermission(mContext as Activity, FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    mContext as Activity, COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED){
                return
            }
            mMap.isMyLocationEnabled = true
            mMap.uiSettings.isMyLocationButtonEnabled = false

            init()
        }
    }

    private fun initMap(){
        val mapFragment = childFragmentManager
            .findFragmentById(R.id.first_map) as? SupportMapFragment
        Log.d(TAG, "initMap: initialising map")
        
        mapFragment?.getMapAsync(this)
    }
    
    private fun init(){
        Log.d(TAG, "init: initialising")

        placesClient = Places.createClient(mContext)

        from_input_search.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                if(s.length > 2) {
                    enteredText = s.toString()
                    autocompleteHelper()
                    Log.d(TAG, "onTextChanged: передача $enteredText")
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        })

        from_input_search.setOnEditorActionListener(OnEditorActionListener {
                textView, actionId, keyEvent ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH
                || actionId == EditorInfo.IME_ACTION_DONE
                || keyEvent.action == KeyEvent.ACTION_DOWN
                || keyEvent.action == KeyEvent.KEYCODE_ENTER) {

                geoLocate()
            }
            false
        })

        ic_gps.setOnClickListener(View.OnClickListener {
            Log.d(TAG, "onClick: clicked gps icon")
            getDeviceLocation()
        })
        hideSoftKeyboard()
    }

    private fun autocompleteHelper(){
        var places = emptyArray<SpannableString>()

        var token: AutocompleteSessionToken = AutocompleteSessionToken.newInstance()
        var request: FindAutocompletePredictionsRequest = FindAutocompletePredictionsRequest
            .builder()
            .setTypeFilter(TypeFilter.ADDRESS)
            .setSessionToken(token)
            .setQuery(from_input_search.text.toString())
            .build()

        placesClient.findAutocompletePredictions(request)
            .addOnSuccessListener { response: FindAutocompletePredictionsResponse ->
                var mResult = StringBuilder()
                for(prediction in response.autocompletePredictions){
                    mResult.append(" ").append(prediction.getFullText(null))
                    places += prediction.getPrimaryText(null)
                    Log.d(TAG, "autocompleteHelper: ID: ${prediction.placeId}")
                    Log.d(TAG, "autocompleteHelper: Место: ${prediction
                        .getPrimaryText(null)}")
                }
                for(place in places.indices){
                    Log.d(TAG, "autocompleteHelper: Places: ${places[place]}")
                }

                var adapter = ArrayAdapter<SpannableString>(
                    mContext, R.layout.support_simple_spinner_dropdown_item, places)
                from_input_search.setAdapter(adapter)

                geoLocate()
            }.addOnFailureListener {exception: Exception? ->
                if (exception is ApiException){
                    Log.e(TAG, "autocompleteHelper: PlaceNotFound: ${exception.statusCode}")
                }
            }
    }

    private fun getLocationPermission(){
        Log.d(TAG, "getLocationPermission: getting location permissions")
        
        val permissions = arrayOf(FINE_LOCATION, COARSE_LOCATION)
        if (ContextCompat.checkSelfPermission(activity!!, FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(activity!!, COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                //Permission is granted
                mLocationPermissionGranted = true
                initMap()
            } else {
                ActivityCompat.requestPermissions(
                    mContext as Activity, permissions,
                    LOCATION_PERMISSION_REQUEST_CODE
                )
            }
        } else {
            ActivityCompat.requestPermissions(mContext as Activity, permissions,
            LOCATION_PERMISSION_REQUEST_CODE)
        }
    }

    private fun geoLocate(){
        Log.d(TAG, "geoLocate: geolocating")

        var searchString = from_input_search.text.toString()

        var geocoder = Geocoder(mContext)
        var list: List<Address> = ArrayList()
        try {
            list = geocoder.getFromLocationName(searchString, 1)
        } catch (e: IOException){
            Log.d(TAG, "geoLocate: IOException: " + e.message)
        }
        if (list.isNotEmpty()){
            var address: Address = list[0]

            Log.d(TAG, "geoLocate: found a location: $address")

            moveCamera(
                LatLng(address.latitude, address.longitude), DEFAULT_ZOOM,
            address.getAddressLine(0))
        }
    }

    private fun getDeviceLocation(){
        Log.d(TAG, "getDeviceLocation: getting the devices current location")
        mFusedLocationProviderClient = getFusedLocationProviderClient(mContext as Activity)

        try {
            if(mLocationPermissionGranted){
                var location: Task<*> = mFusedLocationProviderClient.lastLocation
                location.addOnCompleteListener(requireActivity()) { task ->
                    if (task.isSuccessful){
                        Log.d(TAG, "getDeviceLocation: found location")
                        var currentLocation: Location = (task.result as Location?)!!
                        moveCamera(LatLng(currentLocation.latitude, currentLocation.longitude),
                        DEFAULT_ZOOM, "My location")
                    } else{
                        Log.d(TAG, "getDeviceLocation: current location is null")
                        Toast.makeText(mContext, "unable to get current location",
                            Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: SecurityException){
            Log.d(TAG, "getDeviceLocation: SecurityException" + e.message)
        }

    }

    private fun moveCamera(latLng: LatLng, zoom: Float, title: String){
        Log.d(TAG, "moveCamera: moving the camera to: lat: " + latLng.latitude
        + ", long: " + latLng.longitude)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom))

        if(title != "My position"){
            var options: MarkerOptions = MarkerOptions()
                .position(latLng)
                .title(title)
            mMap.addMarker(options)
        }
        hideSoftKeyboard()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        Log.d(TAG, "onRequestPermissionsResult: called")
        mLocationPermissionGranted = false

        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty()) {
                    var i = 0
                    while (i > grantResults.size) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            mLocationPermissionGranted = false
                            Log.d(TAG, "onRequestPermissionsResult: permission failed")
                            return
                        }
                        i++
                    }
                    Log.d(TAG, "onRequestPermissionsResult: permission granted")
                    mLocationPermissionGranted = true

                    initMap()
                }
            }
        }
    }

    private fun hideSoftKeyboard(){
        requireActivity().window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
    }
}

