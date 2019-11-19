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
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.lanyard.canvas.BitmapCache
import com.lanyard.helpers.NotificationReceiver
import com.lanyard.pirateerlite.controllers.TownController
import com.lanyard.pirateerlite.fragments.*
import com.lanyard.pirateerlite.singletons.Audio
import com.lanyard.pirateerlite.singletons.User
import com.lanyard.pirateerlite.viewmodels.MapActivityViewModel
import kotlinx.android.synthetic.main.activity_map.*
import java.util.*

/**
 * Main Activity for this app. It has two layouts. For phones portrait is the available layout.
 * For tablets portrait and a landscape layout are available. In portrait there is only one pane.
 * In landscape there are two with the map fragment always visible. A large amount of the code here
 * deals with those two layouts and the backstack. The backstack REALLY wants you to use it in
 * only one way and for a non conventional layout/flow like this app wranging that backstack was
 * a lesson in frustration. The landscape layout was straight foward until you start getting backstack
 * entries that only make sense in portrait and vice versa.
 *
 * @author Peter Respondek
 *
 * @see R.layout.activity_map
 * @see R.layout.fragment_wallet
 */

class MapActivity : AppCompatActivity(), User.UserListener {
    val TAG = "MapActivity"
    private lateinit var _viewModel: MapActivityViewModel

    private val _navigationListener = object : BottomNavigationView.OnNavigationItemSelectedListener {
        override fun onNavigationItemSelected(val1: MenuItem): Boolean {
            swapFragment(val1.itemId, null)
            return true
        }
    }

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

    // only allow checking nav menu items when on the appropriate screen
    fun filterNavigationByFragment(tag: String?) {
        if (tag == "map" || tag == "boats" || tag == "menu") {
            navigation.menu.setGroupCheckable(0, true, true)
        } else {
            navigation.menu.setGroupCheckable(0, false, true)
        }
    }

    override fun onBackPressed() {
        var prev = _viewModel.popBackStack()
        var tag = supportFragmentManager.primaryNavigationFragment?.tag
        // skip over map calls in portrait
        while (prev != null && ((resources.getBoolean(R.bool.landscape) == true && prev.name == "map") || (tag != null && tag == prev.name))) {
            prev = _viewModel.popBackStack()
        }
        if (prev == null) {
            super.onBackPressed()
        } else {
            filterNavigationByFragment(prev.name)
            when (prev.name) {
                "map" -> navigation.menu.findItem(R.id.navigation_map).isChecked = true
                "menu" -> navigation.menu.findItem(R.id.navigation_menu).isChecked = true
                "boats" -> navigation.menu.findItem(R.id.navigation_boats).isChecked = true
            }
            swapFragment(prev, null, true)
        }
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

    private fun createFragment(name: String): androidx.fragment.app.Fragment {
        val map = supportFragmentManager.findFragmentByTag("map")!! as MapFragment
        var frag: androidx.fragment.app.Fragment? = null

        when (name) {
            "town" -> {
                frag = TownFragment()
            }
            "map" -> {
                frag = map
            }
            "boatinfo" -> {
                frag = BoatInfoFragment()
            }
            "shipyard" -> {
                frag = ShipyardFragment()
            }
            "market" -> {
                frag = MarketFragment()
            }
            "stats" -> {
                frag = StatsFragment()
            }
            "menu" -> {
                frag = MenuFragment()
            }
            "boats" -> {
                frag = BoatListFragment()
            }
            "jobs" -> {
                frag = JobFragment()
            }
            else -> {
                frag = map
            }
        }

        return frag
    }

    private fun swapFragment(
        entry: MapActivityViewModel.BackStackEntry,
        tag: Any? = null,
        addToBackStack: Boolean = true
    ): androidx.fragment.app.Fragment {
        val transaction = supportFragmentManager.beginTransaction()
        val map = supportFragmentManager.findFragmentByTag("map")!! as MapFragment
        val prevFrag = supportFragmentManager.primaryNavigationFragment ?: throw NullPointerException()
        val fragTag = prevFrag.tag
        if (map.mode == MapFragment.Mode.build || map.mode == MapFragment.Mode.buy) {
            map.reset()
        }
        filterNavigationByFragment(entry.name)

        // resave the save instance state bundle once the fragment is on the way out
        val prevbs = _viewModel.popBackStack()
        if (prevbs != null) {
            var bundle: Bundle? = prevbs.bundle
            if (prevbs.name == fragTag) {
                bundle = Bundle()
                prevFrag.onSaveInstanceState(bundle)
            }
            _viewModel.addToBackStack(prevbs.name, bundle)
        }

        if (fragTag != null) {
            if (fragTag == "map") {
                transaction.hide(prevFrag)
            } else {
                transaction.remove(prevFrag)
            }
        }
        val frag: androidx.fragment.app.Fragment = createFragment(entry.name)

        if (frag is TownFragment) {
            frag.townController = tag as TownController
        }

        frag.arguments = entry.bundle
        if (entry.name == "map") {
            transaction.show(frag)
        } else {
            transaction.add(R.id.menuFrame, frag, entry.name)
        }
        if (addToBackStack) {
            _viewModel.addToBackStack(entry.name, entry.bundle)
        }
        transaction.setPrimaryNavigationFragment(frag)
        transaction.commit()
        return frag
    }

    fun swapFragment(id: Int?, tag: Any? = null, addToBackStack: Boolean = true): androidx.fragment.app.Fragment {
        var name = ""
        if (tag is TownController) {
            name = "town"
        } else {
            when (id) {
                R.id.navigation_map -> {
                    name = "map"
                }
                R.layout.cell_shipyard -> {
                    name = "boatinfo"
                }
                R.id.shipyardButton -> {
                    name = "shipyard"
                }
                R.id.marketButton -> {
                    name = "market"
                }
                R.id.statsButton -> {
                    name = "stats"
                }
                R.id.navigation_menu -> {
                    name = "menu"
                }
                R.id.navigation_boats -> {
                    name = "boats"
                }
                R.id.holdButton -> {
                    name = "jobs"
                }
            }
        }
        return swapFragment(MapActivityViewModel.BackStackEntry(name, null), tag, addToBackStack)
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

        _viewModel = ViewModelProviders.of(this).get(MapActivityViewModel::class.java)

        User.instance.addListerner(this)
        Looper.getMainLooper().thread.name = "PirateerMain"
        setContentView(R.layout.activity_map)
        navigation.menu.setGroupCheckable(0, true, true)
        val arr = arrayOf("nav_plot.png", "nav_plotted.png")

        navigation.setOnNavigationItemSelectedListener(_navigationListener)

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
            map = MapFragment()
            transaction.add(R.id.mapFrame, map, "map")
            _viewModel.addToBackStack("map")
            if (resources.getBoolean(R.bool.landscape) == true) {
                _viewModel.addToBackStack("boats")
                frag = BoatListFragment()
                transaction.add(R.id.menuFrame, frag, "boats")
                navigation.menu.getItem(0).isChecked = true
            } else {
                frag = map
            }
            currLevel = User.instance.level
        } else {
            currLevel = savedInstanceState.getInt("currLevel")
            val wasLandscape = savedInstanceState.getBoolean("wasLandscape")

            map = supportFragmentManager.findFragmentByTag("map") as MapFragment
            frag = supportFragmentManager.primaryNavigationFragment
            filterNavigationByFragment(frag?.tag)

            if (resources.getBoolean(R.bool.landscape) == true) {
                transaction.show(map)

                // if the current fragment is map it follows that you were is portrait before switching to landscape
                // so we can grab your previous backstack fragment and pop it into the menuframe
                if (frag?.tag == "map") {
                    val prev = _viewModel.popBackStack()
                    if (prev != null) {
                        if (prev.name != "map") {
                            frag = createFragment(prev.name)
                            transaction.add(R.id.menuFrame, frag, prev.name)
                            frag.arguments = prev.bundle
                        }
                        _viewModel.addToBackStack(prev.name, prev.bundle)
                    }
                }

                // otherwise just use the boatlist fragment as default
                if (frag == null || frag.tag == "map") {
                    frag = BoatListFragment()
                    _viewModel.addToBackStack("boats")
                    transaction.add(R.id.menuFrame, frag, "boats")
                }
            } else {
                if (frag != null && savedInstanceState.getBoolean("mapVisible") == true) {
                    transaction.hide(frag)
                    frag = map
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
        val prevFrag = supportFragmentManager.primaryNavigationFragment
        if (prevFrag != null) {
            _viewModel.updateLast(prevFrag)
        }
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
