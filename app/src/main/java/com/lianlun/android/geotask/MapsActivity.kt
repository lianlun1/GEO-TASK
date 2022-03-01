package com.lianlun.android.geotask

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log

import kotlinx.android.synthetic.main.activity_maps.*

class MapsActivity : AppCompatActivity() {

    private val TAG = "MapsActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        val fragmentAdapter = TabHandler(supportFragmentManager)
        viewpager.adapter = fragmentAdapter

        tabs.setupWithViewPager(viewpager)
    }

//    override fun onSendData(data: String) {
//        (supportFragmentManager.
//        findFragmentById(R.id.from_listFragment) as? FromListFragment)?.setEnteredText(data)
//        Log.d(TAG, "onSendData: передача из MapsActivity: $data")
//        autocompleteHelper()
//    }

//    private fun autocompleteHelper(){
//        supportFragmentManager.beginTransaction()
//            .add(R.id.from_listFragment, FromListFragment::class.java, null)
//            .commit()
//    }
}