/*
 * Copyright 2019 Peter Respondek
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lanyard.pirateerlite

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Looper
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import android.view.MenuItem
import com.lanyard.canvas.BitmapCache
import com.lanyard.pirateerlite.controllers.TownController
import com.lanyard.pirateerlite.fragments.*
import kotlinx.android.synthetic.main.activity_map.*
import android.view.Menu
import android.view.View.*
import android.widget.FrameLayout
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
import androidx.fragment.app.FragmentTransaction
import com.lanyard.pirateerlite.singletons.Audio


class MapActivity : AppCompatActivity() {

    private lateinit var _fragment: androidx.fragment.app.Fragment

    val fragment: androidx.fragment.app.Fragment
        get() {
            return _fragment
        }

    private var currMenuItem: Int? = null

    init {
    }

    private val _onNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener {
        if (currMenuItem == it.itemId) {
            return@OnNavigationItemSelectedListener true
        }
        swapFragment(it.itemId)
        return@OnNavigationItemSelectedListener true
    }

    private val _onBackStackChangedListener = FragmentManager.OnBackStackChangedListener {
        var frag = supportFragmentManager.primaryNavigationFragment
        if (frag != null) {
            _fragment = frag
        }
        var item : Int? = null
        when(frag?.tag) {
            "map" -> item = R.id.navigation_map
            "boats" -> item = R.id.navigation_boats
            "menu" -> item = R.id.navigation_menu
        }
        if (item != null) {
            currMenuItem = item
            navigation.selectedItemId = item
        }
    }

    override fun onBackPressed() {
        val map = supportFragmentManager.findFragmentByTag("map")
        if (getResources().getBoolean(R.bool.landscape) != true &&
            supportFragmentManager.backStackEntryCount == 0 &&
            map != null && map.isHidden ) {
            navigation.selectedItemId = R.id.navigation_map
        } else {
            super.onBackPressed()
        }
    }

    fun swapFragment(id: Int?, tag: Any? = null): androidx.fragment.app.Fragment {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.setReorderingAllowed(true)
        transaction.setCustomAnimations(R.anim.abc_popup_enter, R.anim.abc_popup_exit)
        var prevFrag = _fragment
            if (currMenuItem == R.id.navigation_map) {
                transaction.hide(prevFrag)
                currMenuItem = null
            } else {
                transaction.remove(prevFrag)
            }
        if (id == R.id.navigation_map) {
            navigation.menu.getItem(0).setCheckable(true)
            _fragment = supportFragmentManager.findFragmentByTag("map")!!
            transaction.runOnCommit {
                for (i in 0 until supportFragmentManager.getBackStackEntryCount()) {
                    supportFragmentManager.popBackStack()
                }
            }
            transaction.show(_fragment)
            navigation.visibility = VISIBLE
        } else {
            var name = ""
            if (tag is TownController) {
                var frag = TownFragment()
                frag.townController = tag
                navigation.menu.getItem(0).setCheckable(false)
                _fragment = frag
                name = "town"
            } else {
                when (id) {
                    R.layout.cell_shipyard -> {
                        _fragment = BoatInfoFragment()
                        name = "boatinfo"
                    }
                    R.id.shipyardButton -> {
                        _fragment = ShipyardFragment()
                        name = "shipyard"
                    }
                    R.id.marketButton -> {
                        _fragment = MarketFragment()
                        name = "market"
                    }
                    R.id.statsButton -> {
                        _fragment = StatsFragment()
                        name = "stats"
                    }
                    R.id.navigation_menu -> {
                        _fragment = MenuFragment()
                        name = "menu"
                    }
                    R.id.navigation_boats -> {
                        _fragment = BoatListFragment()
                        name = "boats"
                    }
                    R.id.holdButton -> {
                        _fragment = JobFragment()
                        navigation.menu.getItem(0).setCheckable(false)
                        name = "jobs"
                    }
                }
            }
            transaction.add(R.id.menuFrame, _fragment, name)
            transaction.addToBackStack(name)
        }
        transaction.setPrimaryNavigationFragment(_fragment)
        transaction.commit()
        currMenuItem = id
        return _fragment
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (_fragment.tag == "town") {
            menuInflater.inflate(R.menu.jobs_menu, menu)
        }
        return true
    }


    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (_fragment.tag == "town") {
            var town = _fragment as TownFragment
            when (item?.title) {
                "Jobs" -> {
                    var frag = swapFragment(R.id.holdButton, null) as JobFragment
                    frag.townModel = town.townController.model
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        if(getResources().getBoolean(R.bool.portrait_only)){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        super.onCreate(savedInstanceState)
        Looper.getMainLooper().thread.name = "PirateerMain"
        setContentView(R.layout.activity_map)
        navigation.setOnNavigationItemSelectedListener(_onNavigationItemSelectedListener)
        supportFragmentManager.addOnBackStackChangedListener (_onBackStackChangedListener)

        var arr = arrayOf("nav_plot.png", "nav_plotted.png")
        for (n in arr) {
            BitmapCache.instance.addBitmap(applicationContext, n, Bitmap.Config.ARGB_4444)
        }
        if (getResources().getBoolean(R.bool.landscape) == true) {
            navigation.menu.removeItem(R.id.navigation_map)
        }

        val transaction = supportFragmentManager.beginTransaction()
        if (savedInstanceState == null) {
            val map = MapFragment()
            transaction.add(R.id.mapFrame, map, "map")
            if (getResources().getBoolean(R.bool.landscape) == true) {
                _fragment = BoatListFragment()
                transaction.add(R.id.menuFrame, _fragment, "boats")
                currMenuItem = R.id.navigation_boats
            } else {
                currMenuItem = R.id.navigation_map
                _fragment = map
            }
        } else {
            val map = supportFragmentManager.findFragmentByTag("map")!!
            val frag = supportFragmentManager.fragments.findLast { it.tag != null && it.tag != "wallet" && it.tag != "map" && it !is DialogFragment }
            if (getResources().getBoolean(R.bool.landscape) == true) {
                transaction.show(map)
                if (frag == null) {
                    _fragment = BoatListFragment()
                    transaction.add(R.id.menuFrame, _fragment, "boats")
                    currMenuItem = R.id.navigation_boats
                }
            } else {
                transaction.hide(map)
            }
            if (frag != null) {
                _fragment = frag
                currMenuItem = savedInstanceState.getInt("currMenuItem")
            }
        }
        transaction.setPrimaryNavigationFragment(_fragment)
        transaction.commit()
    }


    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
    }


    override fun onSaveInstanceState(outState: Bundle?) {
        if (currMenuItem != null) {
            outState?.putInt("currMenuItem", currMenuItem!!)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onPause() {
        super.onPause()
        Audio.instance.pause()
    }

    override fun onResume() {
        super.onResume()
        Audio.instance.resume()
    }
}
