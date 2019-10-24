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

import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.lanyard.canvas.BitmapCache
import com.lanyard.helpers.NotificationReceiver
import com.lanyard.pirateerlite.controllers.TownController
import com.lanyard.pirateerlite.fragments.*
import com.lanyard.pirateerlite.singletons.Audio
import com.lanyard.pirateerlite.singletons.User
import kotlinx.android.synthetic.main.activity_map.*


class MapActivity : AppCompatActivity() {

    var ignoreBackStack = false
    init {
    }

    private val _onNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener {
        navigation.menu.setGroupCheckable(0,true,true)
        val manager = supportFragmentManager
        if (manager.backStackEntryCount > 0) {
            ignoreBackStack = true
            AppFragment.blockAnimation = true
            manager.popBackStackImmediate(null, POP_BACK_STACK_INCLUSIVE)
            AppFragment.blockAnimation = false
            var fragTag = manager.primaryNavigationFragment?.tag
            if (fragTag == "map" && it.itemId == R.id.navigation_map) {
                return@OnNavigationItemSelectedListener true
            }
        }
        ignoreBackStack = true
        swapFragment(it.itemId)
        return@OnNavigationItemSelectedListener true
    }

    private val _onBackStackChangedListener = FragmentManager.OnBackStackChangedListener {
        if (ignoreBackStack == true) {
            ignoreBackStack = false
            return@OnBackStackChangedListener
        }
        val map = supportFragmentManager.findFragmentByTag("map") as MapFragment
        val frag = supportFragmentManager.primaryNavigationFragment
        val buttonId = arrayListOf(R.id.navigation_map, R.id.navigation_boats, R.id.navigation_menu)
        val fragTag = arrayListOf("map", "boats", "menu")
        if (resources.getBoolean(R.bool.landscape)) {
            buttonId.removeAt(0)
            fragTag.removeAt(0)
        }
        val item: Int = fragTag.indexOf(frag?.tag)

        filterNavigationByFragment(frag?.tag)

        if (item != -1) {
            if (navigation.selectedItemId != buttonId[item]) {
                navigation.menu.getItem(item).isChecked = true
            }
        }

    }

    fun filterNavigationByFragment(tag: String?) {
        if (tag == "map" || tag == "boats" || tag == "menu") {
            navigation.menu.setGroupCheckable(0,true,true)
        } else {
            navigation.menu.setGroupCheckable(0,false,true)
        }
    }

    override fun onBackPressed() {
        val frag = supportFragmentManager.primaryNavigationFragment
        if (frag?.tag == "town" && supportFragmentManager.backStackEntryCount == 0) {
            swapFragment(navigation.selectedItemId)
        } else {
            super.onBackPressed()
        }
    }

    fun postNotifications() {
        val user = User.instance
        for (boat in user.boats) {
            if (boat.isMoored == false) {
                val notif = NotificationReceiver.NotificationData(
                    R.drawable.ic_nav_boats,
                    boat.id.toInt(),
                    getString(R.string.channelId),
                    getString(R.string.notifBoatArrivedTitle, boat.name),
                    getString(R.string.notifBoatArrivedDescription),
                    boat.arrivalTime
                )
                NotificationReceiver().scheduleNotification(this, notif)
            }
        }
    }

    fun clearNotifications() {
        NotificationReceiver().clearNotifications(this)
    }

    fun swapFragment(id: Int?, tag: Any? = null): androidx.fragment.app.Fragment {
        val transaction = supportFragmentManager.beginTransaction()
        val map = supportFragmentManager.findFragmentByTag("map")!! as MapFragment
        val prevFrag = supportFragmentManager.primaryNavigationFragment ?: throw NullPointerException()
        if (prevFrag.tag == "map") {
            transaction.hide(prevFrag)
        } else {
            transaction.remove(prevFrag)
        }
        var frag: androidx.fragment.app.Fragment? = null
        var name = ""
        if (id == R.id.navigation_map) {
            frag = map
            name = "map"
            transaction.show(frag)
        } else {
            if (tag is TownController) {
                frag = TownFragment()
                frag.townController = tag
                name = "town"
            } else {
                require(id != R.id.navigation_map) { "Bad State" }
                when (id) {
                    R.layout.cell_shipyard -> {
                        frag = BoatInfoFragment()
                        name = "boatinfo"
                    }
                    R.id.shipyardButton -> {
                        frag = ShipyardFragment()
                        name = "shipyard"
                    }
                    R.id.marketButton -> {
                        frag = MarketFragment()
                        name = "market"
                    }
                    R.id.statsButton -> {
                        frag = StatsFragment()
                        name = "stats"
                    }
                    R.id.navigation_menu -> {
                        frag = MenuFragment()
                        name = "menu"
                    }
                    R.id.navigation_boats -> {
                        frag = BoatListFragment()
                        name = "boats"
                    }
                    R.id.holdButton -> {
                        frag = JobFragment()
                        name = "jobs"
                    }
                }
            }
            transaction.add(R.id.menuFrame, frag!!, name)
        }
        if ((name != "map" && name != "boats" && name != "menu" && name != "town") ||
            (name == "town" && supportFragmentManager.backStackEntryCount > 0) ||
            (name == "map" && prevFrag.tag == "boatinfo" && (map.mode == MapFragment.Mode.build || map.mode == MapFragment.Mode.buy))
        ) {
            transaction.addToBackStack(name)
        } else {
            filterNavigationByFragment(name)
        }
        transaction.setPrimaryNavigationFragment(frag)
        transaction.commit()
        return frag
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val frag = supportFragmentManager.primaryNavigationFragment ?: throw NullPointerException()
        if (frag.tag == "town") {
            menuInflater.inflate(R.menu.jobs_menu, menu)
        }
        return super.onPrepareOptionsMenu(menu)
    }


    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        val frag = supportFragmentManager.primaryNavigationFragment ?: throw NullPointerException()
        if (frag.tag == "town") {
            val town = frag as TownFragment
            when (item?.title) {
                "Jobs" -> {
                    val jobFrag = swapFragment(R.id.holdButton, null) as JobFragment
                    jobFrag.townModel = town.townController.model
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (resources.getBoolean(R.bool.portrait_only)) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        Looper.getMainLooper().thread.name = "PirateerMain"
        setContentView(R.layout.activity_map)
        supportFragmentManager.addOnBackStackChangedListener(_onBackStackChangedListener)
        navigation.menu.setGroupCheckable(0, true, true)
        val arr = arrayOf("nav_plot.png", "nav_plotted.png")
        for (n in arr) {
            BitmapCache.instance.addBitmap(applicationContext, n, Bitmap.Config.ARGB_4444)
        }
        if (resources.getBoolean(R.bool.landscape) == true) {
            navigation.menu.removeItem(R.id.navigation_map)
        }

        val transaction = supportFragmentManager.beginTransaction()
        transaction.setReorderingAllowed(true)
        var frag: androidx.fragment.app.Fragment? = null
        if (savedInstanceState == null) {
            val map = MapFragment()
            transaction.add(R.id.mapFrame, map, "map")
            if (resources.getBoolean(R.bool.landscape) == true) {
                frag = BoatListFragment()
                transaction.add(R.id.menuFrame, frag, "boats")
                navigation.menu.getItem(0).isChecked = true
            } else {
                frag = map
            }
        } else {
            val map = supportFragmentManager.findFragmentByTag("map") as MapFragment
            frag = supportFragmentManager.primaryNavigationFragment
            filterNavigationByFragment(frag?.tag)

            if (resources.getBoolean(R.bool.landscape) == true) {
                transaction.show(map)
                if (frag == null || frag.tag == "map") {
                    frag = BoatListFragment()
                    transaction.add(R.id.menuFrame, frag, "boats")
                    navigation.menu.getItem(0).isChecked = true
                }
            } else {
                if (frag != null && savedInstanceState.getBoolean("mapVisible") == true) {
                    transaction.hide(frag)
                } else {
                    transaction.hide(map)
                }
            }
        }
        transaction.setPrimaryNavigationFragment(frag)
        transaction.commit()
        transaction.runOnCommit {
            LocalBroadcastManager.getInstance(this).sendBroadcast(Intent("surfaceCreated"))

        }
        navigation.setOnNavigationItemSelectedListener(_onNavigationItemSelectedListener)

    }


    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        val map = supportFragmentManager.findFragmentByTag("map") as MapFragment
        if (map.mode == MapFragment.Mode.build || map.mode == MapFragment.Mode.buy) {
            outState?.putBoolean("mapVisible",true)
        } else {
            outState?.putBoolean("mapVisible",false)
        }
    }

    override fun onPause() {
        Audio.instance.pause()
        postNotifications()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        clearNotifications()
        Audio.instance.resume()
    }


}
