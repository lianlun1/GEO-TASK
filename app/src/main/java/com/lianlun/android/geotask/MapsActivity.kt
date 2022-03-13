package com.lianlun.android.geotask

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.tabs.TabLayoutMediator

import kotlinx.android.synthetic.main.activity_maps.*
import kotlinx.android.synthetic.main.activity_maps.viewpager

class MapsActivity : AppCompatActivity(), OnSendLatLngListener{

    private var origin: LatLng? = null
    private var destination: LatLng? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        val pageAdapter: FragmentStateAdapter = TabHandler(this)
        viewpager?.adapter = pageAdapter

        val apiKey = getString(R.string.google_maps_key)

        val tabLayoutMediator = TabLayoutMediator(
            tabs, viewpager
        ) { tab, position ->
            when(position){
                0 -> tab.text = "Откуда"
                else -> tab.text = "Куда"
            }
        }
        tabLayoutMediator.attach()

        button()
    }


    override fun onSendLatLngOrigin(latLng: LatLng) {
        this.origin = latLng
    }

    override fun onSendLatLngDestination(latLng: LatLng) {
        this.destination = latLng
    }

    private fun button() {
        route_button.setOnClickListener(View.OnClickListener {
            when {
                origin == null -> {
                    Toast.makeText(this, "Выберите оба адреса", Toast.LENGTH_SHORT).show()
                }
                destination == null -> {
                    Toast.makeText(this, "Выберите оба адреса", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    setContentView(R.layout.activity_maps_route)
                    val fragment = supportFragmentManager.findFragmentById(R.id.routeFragment) as RouteFragment
                    fragment.setLatLng(origin, destination)
                }
            }
        })
    }
}