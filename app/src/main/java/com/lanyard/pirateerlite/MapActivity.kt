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

import android.app.Dialog
import android.app.PendingIntent
import android.content.ComponentCallbacks2
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.lanyard.canvas.BitmapCache
import com.lanyard.helpers.NotificationReceiver
import com.lanyard.pirateerlite.controllers.TownController
import com.lanyard.pirateerlite.fragments.*
import com.lanyard.pirateerlite.singletons.Audio
import com.lanyard.pirateerlite.singletons.User
import kotlinx.android.synthetic.main.activity_map.*
import java.util.*

/**
 * @author Peter Respondek
 */

class MapActivity : AppCompatActivity(), User.UserListener {
    val TAG = "MapActivity"
    var ignoreBackStack = false
    var startLandscape = true

    init {
    }

    class LevelUpDialogFragment : androidx.fragment.app.DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            return activity.let {
                val builder = AlertDialog.Builder(it!!)
                builder.setTitle(R.string.levelUpTitle)
                builder.setMessage(R.string.levelUpMessage).setNegativeButton(R.string.ok, { dialog, id -> })
                builder.create()
            }
        }
    }

    override fun levelUpdated(oldValue: Int, newValue: Int) {
        if (supportFragmentManager.findFragmentByTag("levelUp") == null) {
            try {
                val leveldialog = LevelUpDialogFragment()
                leveldialog.show(supportFragmentManager, "levelup")
            } catch (e: IllegalStateException) {
                Log.e("MapActivity", e.toString())
            }
        }
    }

    fun buttonTag(id: Int): String {
        var tag = ""
        when (id) {
            R.id.navigation_map -> {
                tag = "map"
            }
            R.id.navigation_boats -> {
                tag = "boats"
            }
            R.id.navigation_menu -> {
                tag = "menu"
            }
        }
        return tag
    }

    private val _onNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener {
        navigation.menu.setGroupCheckable(0,true,true)
        val manager = supportFragmentManager
        var fragTag = manager.primaryNavigationFragment?.tag
        var popped = false
        var tag = buttonTag(it.itemId)
        if (manager.backStackEntryCount > 0) {
            ignoreBackStack = true
            if (tag == "map") {
                popped = manager.popBackStackImmediate(null, POP_BACK_STACK_INCLUSIVE)
            } else {
                popped = manager.popBackStackImmediate(tag, 0)
            }
        }
        if (popped == false || manager.primaryNavigationFragment?.tag != tag) {
            ignoreBackStack = true
            swapFragment(it.itemId)
        }
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

    // only allow checking nav menu items when on the appropriate screen
    fun filterNavigationByFragment(tag: String?) {
        if (tag == "map" || tag == "boats" || tag == "menu") {
            navigation.menu.setGroupCheckable(0,true,true)
        } else {
            navigation.menu.setGroupCheckable(0,false,true)
        }
    }

    override fun onBackPressed() {
        val manager = supportFragmentManager

        // skip over job fragment when the back button is pressed. This fragment can have bad data when you sell the boat.
        if (manager.backStackEntryCount > 1) {
            val frag = manager.getBackStackEntryAt(manager.backStackEntryCount - 2).name
            if (frag == "jobs") {
                manager.popBackStackImmediate()
            }
        }

        // backstack wrangling. If you start portrait, then change to landscape the initial portrait transaction plays
        // if back button is pressed on the last backstack element.
        if (manager.backStackEntryCount == 1 &&
            startLandscape == false &&
            resources.getBoolean(R.bool.landscape) == true
        ) {
            finish()
            return
        }
        super.onBackPressed()
    }

    fun postNotifications() {
        val user = User.instance
        for (boat in user.boats) {
            if (boat.isMoored == false) {

                val notifyIntent = Intent(this, SplashActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }

                notifyIntent.putExtra("boatid", boat.id)
                val notifyPendingIntent = PendingIntent.getActivity(
                    this, boat.id.toInt(), notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT
                )

                val builder = NotificationCompat.Builder(this, getString(R.string.channelId))
                    .setSmallIcon(R.drawable.ic_nav_boats)
                    .setContentTitle(getString(R.string.notifBoatArrivedTitle, boat.name))
                    .setContentText(getString(R.string.notifBoatArrivedDescription))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(notifyPendingIntent)
                    .setSound(Uri.parse("android.resource://com.lanyard.pirateerlite/" + R.raw.ship_bell))
                    .setAutoCancel(true)

                NotificationReceiver().scheduleNotification(
                    this,
                    boat.id.toInt(),
                    builder.build(),
                    Date(boat.arrivalTime)
                )
            }
        }
    }

    fun clearNotifications() {
        NotificationReceiver().clearNotifications(this)
    }

    fun swapFragment(id: Int?, tag: Any? = null): androidx.fragment.app.Fragment {
        var startTime = System.nanoTime()
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
        if ((name != "map") ||
            ((name == "boats" && resources.getBoolean(R.bool.landscape) == false) ||
                    (name == "map" && prevFrag.tag == "boatinfo" && (map.mode == MapFragment.Mode.build || map.mode == MapFragment.Mode.buy)))
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

        User.instance.addListerner(this)
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

        val boatid = intent.getLongExtra("boatid", 0)

        val transaction = supportFragmentManager.beginTransaction()
        transaction.setReorderingAllowed(true)
        var frag: androidx.fragment.app.Fragment? = null
        var currLevel = 0
        val map: MapFragment
        if (savedInstanceState == null) {
            startLandscape = resources.getBoolean(R.bool.landscape)
            map = MapFragment()
            transaction.add(R.id.mapFrame, map, "map")
            if (resources.getBoolean(R.bool.landscape) == true) {
                frag = BoatListFragment()
                transaction.add(R.id.menuFrame, frag, "boats")
                navigation.menu.getItem(0).isChecked = true
            } else {
                frag = map
            }
            currLevel = User.instance.level
        } else {
            startLandscape = savedInstanceState.getBoolean("startLandscape")

            currLevel = savedInstanceState.getInt("currLevel")

            map = supportFragmentManager.findFragmentByTag("map") as MapFragment
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

        if (boatid != 0L) {
            var args = map.arguments
            if (args == null) {
                args = Bundle()
                map.arguments = args
            }
            args.putLong("selectedBoat", boatid)
        }

        transaction.setPrimaryNavigationFragment(frag)
        transaction.commit()
        if (currLevel != User.instance.level) {
            levelUpdated(currLevel, User.instance.level)
        }
        navigation.setOnNavigationItemSelectedListener(_onNavigationItemSelectedListener)
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("currLevel", User.instance.level)
        val map = supportFragmentManager.findFragmentByTag("map") as MapFragment
        if (map.mode == MapFragment.Mode.build || map.mode == MapFragment.Mode.buy) {
            outState.putBoolean("mapVisible", true)
        } else {
            outState.putBoolean("mapVisible", false)
        }
        outState.putBoolean("startLandscape", startLandscape)
    }

    override fun onTrimMemory(level: Int) {

        when (level) {

            ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> {
            }

            ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE,
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW,
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> {
            }

            ComponentCallbacks2.TRIM_MEMORY_BACKGROUND,
            ComponentCallbacks2.TRIM_MEMORY_MODERATE,
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> {
                BitmapCache.instance.trim()
            }
            else -> {
            }
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
