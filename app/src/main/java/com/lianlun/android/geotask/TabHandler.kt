package com.lianlun.android.geotask

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.annotation.NonNull




class TabHandler(fragment: FragmentActivity): FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int {
        return 2
    }

    @NonNull
    override fun createFragment(position: Int): Fragment {
        return when(position){
            0 -> OriginFragment()
            else -> DestinationFragment()
        }
    }

}