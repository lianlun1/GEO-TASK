package com.lianlun.android.geotask

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.Task
import com.lianlun.android.geotask.R.layout.fragment_origin
import kotlinx.android.synthetic.main.fragment_origin.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import java.lang.ClassCastException

class OriginFragment : Fragment(), OnMapReadyCallback, InitializeHelperInterface {

    private lateinit var sendLatLngOriginListener: OnSendLatLngListener

    private lateinit var mMap: GoogleMap

    private lateinit var mContext: Context

    private val FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION
    private val COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION
    private val LOCATION_PERMISSION_REQUEST_CODE = 1234
    override lateinit var mFusedLocationProviderClient: FusedLocationProviderClient
    private val DEFAULT_ZOOM = 15f
    private var mLocationPermissionGranted = false
    override lateinit var placesClient: PlacesClient

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context

        try {
            sendLatLngOriginListener = context as OnSendLatLngListener
        } catch (e: ClassCastException){
            throw ClassCastException(
                "$context должен реализовывать интерфейс OnSendLatLngToListener")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(fragment_origin, container, false)

        getLocationPermission()

        val apiKey = mContext.getString(R.string.google_maps_key)

        if (!Places.isInitialized()){
            Places.initialize(mContext, apiKey)
        }
        return view
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

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

            placesClient = Places.createClient(mContext)

            init(from_input_search, mContext, ic1_gps, DEFAULT_ZOOM)
        }
    }

    private fun initMap(){
        val mapFragment = childFragmentManager
            .findFragmentById(R.id.first_map) as? SupportMapFragment

        mapFragment?.getMapAsync(this)
    }

    private fun getLocationPermission(){

        val permissions = arrayOf(FINE_LOCATION, COARSE_LOCATION)
        if (ContextCompat.checkSelfPermission(activity!!, FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(activity!!, COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
            ) {
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

    private fun getDeviceLocation(){
        mFusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(mContext as Activity)
        try {
            if(mLocationPermissionGranted){
                var location: Task<*> = mFusedLocationProviderClient.lastLocation
                location.addOnCompleteListener(requireActivity()) { task ->
                    if (task.isSuccessful){
                        var currentLocation: Location = (task.result as Location?)!!
//                        Log.d(TAG, "getDeviceLocation: task is successful")
                        moveCamera(
                            LatLng(currentLocation.latitude, currentLocation.longitude),
                            DEFAULT_ZOOM, "My location")
//                        Log.d(TAG, "getDeviceLocation: запущен moveCamera")
                    } else{
                        Toast.makeText(mContext, "unable to get current location",
                            Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: SecurityException){ }
    }

    override fun getDeviceLocationGps(){
        mFusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(mContext as Activity)
        try {
            if(mLocationPermissionGranted){
                var location: Task<*> = mFusedLocationProviderClient.lastLocation
                location.addOnCompleteListener(requireActivity()) { task ->
                    if (task.isSuccessful){
                        var currentLocation: Location = (task.result as Location?)!!

                        setLatLng(LatLng(currentLocation.latitude, currentLocation.longitude))

                        moveCamera(
                            LatLng(currentLocation.latitude, currentLocation.longitude),
                            DEFAULT_ZOOM, "My location")
                    } else{
                        Toast.makeText(mContext, "unable to get current location",
                            Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: SecurityException){ }
    }

    override fun setLatLng(latLng: LatLng) {
        sendLatLngOriginListener.onSendLatLngOrigin(latLng)
    }

    override fun moveCamera(latLng: LatLng, zoom: Float, title: String){

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
        mLocationPermissionGranted = false

        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty()) {
                    var i = 0
                    while (i > grantResults.size) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            mLocationPermissionGranted = false
                            return
                        }
                        i++
                    }
                    mLocationPermissionGranted = true

                    initMap()
                }
            }
        }
    }

    override fun hideSoftKeyboard(){
        requireActivity().window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
    }
}

