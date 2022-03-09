package com.lianlun.android.geotask

import android.content.Context
import android.graphics.Color
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.PolylineOptions
import com.lianlun.android.geotask.R.layout.fragment_route
import kotlinx.coroutines.joinAll
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.ln

class RouteFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var TAG: String = "RouteFragment"
    private lateinit var origin: LatLng
    private lateinit var destination: LatLng
    private lateinit var apiKey: String
    private lateinit var mContext: Context
    private lateinit var polylineList: List<LatLng>
    private lateinit var polylineOptions: PolylineOptions

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

        return view
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
    }

    fun setLatLng(origin: LatLng, destination: LatLng){
        Log.d(TAG, "setLatLng: получение From: $origin, To: $destination")
        this.origin = origin
        this.destination = destination

        val url: String = getDirectionsUrl(origin, destination)!!
        val downloadTask = DownloadTask()
        downloadTask.execute(url)
//        builder()
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
            Log.d(TAG, "downloadUrl: ${e.toString()}")
        }finally {
            iStream!!.close()
            urlConnection!!.disconnect()
        }
        Log.d(TAG, "downloadUrl: data: $data")
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
        Log.d(TAG, "getDirectionsUrl: url: $url")
        return url
    }

    private fun builder(){
        val builder = LatLngBounds.Builder()
        builder.include(origin)
        builder.include(destination)
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100))
    }

//    private fun decodePoly(encoded: String): List<LatLng>? {
//        val poly: MutableList<LatLng> = ArrayList()
//        var index = 0
//        val len = encoded.length
//        var lat = 0
//        var lng = 0
//        while (index < len) {
//            var b = 0
//            var shift = 0
//            var result = 0
//            do {
//                if (index < len) {
//                    b = encoded[index++].code - 63
//                }
//                result = result or (b and 0x1f shl shift)
//                shift += 5
//            } while (b >= 0x20)
//            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
//            lat += dlat
//            shift = 0
//            result = 0
//            do {
//                if (index < len) {
//                    b = encoded[index++].code - 63
//                }
//                result = result or (b and 0x1f shl shift)
//                shift += 5
//            } while (b >= 0x20)
//            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
//            lng += dlng
//            val p = LatLng(
//                lat.toDouble() / 1E5,
//                lng.toDouble() / 1E5
//            )
//            poly.add(p)
//        }
//        return poly
//    }

    inner class DownloadTask: AsyncTask<String?, Void?, String?>(){

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            val parserTask = ParserTask()
            parserTask.execute(result)
            Log.d(TAG, "onPostExecute: result: $result")
        }

        override fun doInBackground(vararg url: String?): String? {
            var data = ""
            try{
                data = downloadUrl(url[0].toString()).toString()
                Log.d(TAG, "doInBackground: url: ${url[0]}")
            }catch (e: Exception){}
            Log.d(TAG, "doInBackground: data: $data")
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
            Log.d(TAG, "doInBackground: routes: $routes")
            return routes
        }

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
                    Log.d(TAG, "onPostExecute: positions: $position")
                    points.add(position)
                }
                lineOptions.addAll(points)
                lineOptions.width(8f)
                lineOptions.color(Color.BLACK)
                lineOptions.geodesic(true)
            }
            if (points.size != 0) mMap.addPolyline(lineOptions)
            builder()
        }
    }
}