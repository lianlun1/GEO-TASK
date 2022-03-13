package com.lianlun.android.geotask

import com.google.android.gms.maps.model.LatLng

interface OnSendLatLngListener {

    fun onSendLatLngOrigin(latLng: LatLng){}

    fun onSendLatLngDestination(latLng: LatLng)
}