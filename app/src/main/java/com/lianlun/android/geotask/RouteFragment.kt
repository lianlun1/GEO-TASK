package com.lianlun.android.geotask

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.gms.tasks.Task
import com.lianlun.android.geotask.R.layout.fragment_route
import kotlinx.android.synthetic.main.fragment_route.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class RouteFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var origin: LatLng
    private lateinit var destination: LatLng
    private lateinit var myLocation: LatLng
    private lateinit var apiKey: String
    private lateinit var mContext: Context
    private var mLocationPermissionGranted = false
    private val FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION
    private val COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION
    private val LOCATION_PERMISSION_REQUEST_CODE = 1234
    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient


    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(fragment_route, container, false)

        val mapFragment = childFragmentManager
            .findFragmentById(R.id.route_map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        apiKey = mContext.getString(R.string.google_maps_key)

        getLocationPermission()

        return view
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        if(mLocationPermissionGranted){
            getDeviceLocation()
            if(ActivityCompat.checkSelfPermission(mContext as Activity, FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    mContext as Activity, COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                return
            }
            mMap.isMyLocationEnabled = true
            mMap.uiSettings.isMyLocationButtonEnabled = false
        }
    }

    fun setLatLng(origin: LatLng?, destination: LatLng?){
        this.origin = origin!!
        this.destination = destination!!

        val url: String = getDirectionsUrl(origin, destination)!!
        val downloadTask = DownloadTask()
        downloadTask.execute(url)
    }

    private fun getLocationPermission(){

        val permissions = arrayOf(FINE_LOCATION, COARSE_LOCATION)
        if (ContextCompat.checkSelfPermission(activity!!, FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(activity!!, COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                mLocationPermissionGranted = true
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
                        myLocation = LatLng(currentLocation.latitude, currentLocation.longitude)
                    } else{
                        Toast.makeText(mContext, "unable to get current location",
                            Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: SecurityException){ }

    }

    @Throws(IOException::class)
    private fun downloadUrl(strUrl: String): String?{
        var data = ""
        var iStream: InputStream? = null
        var urlConnection: HttpURLConnection? = null
        try {
            val url = URL(strUrl)
            urlConnection = url.openConnection() as HttpURLConnection
            urlConnection.connect()
            iStream = urlConnection!!.inputStream
            val br = BufferedReader(InputStreamReader(iStream))
            val sb = StringBuffer()
            var line: String? = ""
            while (br.readLine().also { line = it } != null){
                sb.append(line)
            }
            data = sb.toString()
            br.close()
        }catch (e: java.lang.Exception){
        }finally {
            iStream!!.close()
            urlConnection!!.disconnect()
        }
        return data
    }

    private fun getDirectionsUrl(origin: LatLng, dest: LatLng): String? {

        val str_origin =
            "origin=" + origin.latitude.toString() + "," + origin.longitude
        val str_dest =
            "destination=" + dest.latitude.toString() + "," + dest.longitude
        val key = "key=$apiKey"
        val parameters = "$str_origin&$str_dest&$key"
        val output = "json"
        val url = "https://maps.googleapis.com/maps/api/directions/$output?$parameters"
        return url
    }

    private fun builder(){
        val builder = LatLngBounds.Builder()
        builder.include(origin)
        builder.include(destination)
        builder.include(myLocation)
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100))
    }

    inner class DownloadTask: AsyncTask<String?, Void?, String?>(){

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            val parserTask = ParserTask()
            parserTask.execute(result)
        }

        override fun doInBackground(vararg url: String?): String? {
            var data = ""
            try{
                data = downloadUrl(url[0].toString()).toString()
            }catch (e: Exception){}
            return data
        }
    }

    inner class ParserTask: AsyncTask<String?, Int?, List<List<HashMap<String, String>>>?>(){
        override fun doInBackground(vararg jsonData: String?): List<List<HashMap<String, String>>>? {
            val jObject: JSONObject
            var routes: List<List<HashMap<String, String>>>? = null
            try{
                jObject = JSONObject(jsonData[0])
                val parser = DataParser()
                routes = parser.parse(jObject)
            }catch (e: java.lang.Exception){
                e.printStackTrace()
            }
            return routes
        }

        @SuppressLint("SetTextI18n")
        override fun onPostExecute(result: List<List<HashMap<String, String>>>?) {
            val points = ArrayList<LatLng?>()
            val lineOptions = PolylineOptions()
            for(i in result!!.indices){
                val path = result[i]
                for (j in path.indices){
                    val point = path[j]
                    val lat = point["lat"]!!.toDouble()
                    val lng = point["lng"]!!.toDouble()
                    val position = LatLng(lat, lng)
                    points.add(position)
                }
                lineOptions.addAll(points)
                lineOptions.width(8f)
                lineOptions.color(Color.BLACK)
                lineOptions.geodesic(true)
            }
            if (points.size != 0) {
                route_text.text = "Found"
                mMap.addPolyline(lineOptions)
                builder()
            } else{
                route_text.text = "Unknown"
            }
        }
    }
}