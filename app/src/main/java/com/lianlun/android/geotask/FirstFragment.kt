package com.lianlun.android.geotask

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import android.view.inputmethod.EditorInfo
import android.widget.*
import android.widget.TextView.OnEditorActionListener
import androidx.annotation.NonNull
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.room.vo.Field
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationServices.getFusedLocationProviderClient
import com.google.android.gms.location.places.Place
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.lianlun.android.geotask.R.id.from_inputSearch
import com.lianlun.android.geotask.R.layout.fragment_first
import kotlinx.android.synthetic.main.fragment_first.*
import java.io.IOException
import java.util.ArrayList
import java.util.concurrent.Executor
import kotlin.math.log

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
    //autocomplete
    private val AUTOCOMPLETE_REQUEST_CODE = 1
    private val fields = listOf(com.google.android.libraries.places.api.model.Place.Field.ID,
    com.google.android.libraries.places.api.model.Place.Field.NAME)

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

        val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
            .build(mContext)
        startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE)

        from_inputSearch.setOnEditorActionListener { textView, actionId, keyEvent ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH
                || actionId == EditorInfo.IME_ACTION_DONE
                || keyEvent.action == KeyEvent.ACTION_DOWN
                || keyEvent.action == KeyEvent.KEYCODE_ENTER
            ) {
                //execute our method for searching
                geoLocate()
            }
            false
        }
        ic_gps.setOnClickListener(View.OnClickListener {
            Log.d(TAG, "onClick: clicked gps icon")
            getDeviceLocation()
        })
        hideSoftKeyboard()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode == AUTOCOMPLETE_REQUEST_CODE){
            when(resultCode){
                Activity.RESULT_OK -> {
                    data?.let {
                        val place = Autocomplete.getPlaceFromIntent(data)
                        Log.i(TAG, "onActivityResult: Place ${place.name}, ${place.id}")
                    }
                }
                AutocompleteActivity.RESULT_ERROR -> {
                    data?.let {
                        val status = Autocomplete.getStatusFromIntent(data)
                        Log.i(TAG, "onActivityResult: " + status.statusMessage)
                    }
                }
                Activity.RESULT_CANCELED -> {
                    //the user canceled the operation
                }
            }
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
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

        var searchString = from_inputSearch.text.toString()

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
                    //initialise our map
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

