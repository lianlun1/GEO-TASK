package com.lianlun.android.geotask

import com.google.android.gms.maps.model.LatLng

interface OnSendLatLngListener {

    fun onSendLatLngFrom(latLng: LatLng){}

    fun onSendLatLngTo(latLng: LatLng)
}