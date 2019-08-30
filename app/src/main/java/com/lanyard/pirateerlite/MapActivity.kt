package com.lanyard.pirateerlite

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Looper
import android.os.PersistableBundle
import android.util.AttributeSet
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import android.util.DisplayMetrics
import android.view.MenuItem
import com.lanyard.canvas.BitmapCache
import com.lanyard.canvas.BitmapStream
import com.lanyard.pirateerlite.controllers.TownController
import com.lanyard.pirateerlite.fragments.*
import com.lanyard.pirateerlite.models.BoatModel
import com.lanyard.pirateerlite.singletons.User
import com.lanyard.pirateerlite.singletons.Map
import kotlinx.android.synthetic.main.activity_map.*
import android.view.Menu
import android.view.View
import android.view.View.*
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import com.lanyard.pirateerlite.singletons.Game
import java.util.concurrent.Executor


class MapActivity : AppCompatActivity() {

    private lateinit var _fragment: androidx.fragment.app.Fragment

    val fragment: androidx.fragment.app.Fragment
        get() {
            return _fragment
        }

    private var currMenuItem: Int? = null

    init {
    }

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener {
        if (currMenuItem == it.itemId) {
            return@OnNavigationItemSelectedListener false
        }
        swapFragment(it.itemId)
        return@OnNavigationItemSelectedListener true
    }

    fun swapFragment(id: Int?, tag: Any? = null): androidx.fragment.app.Fragment {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.setReorderingAllowed(true)
        transaction.setCustomAnimations(R.anim.abc_popup_enter,R.anim.abc_popup_exit)
        if (currMenuItem == R.id.navigation_map) {
            transaction.hide(_fragment)
            currMenuItem = null
        } else {
            transaction.remove(_fragment)
        }
        if (id == R.id.navigation_map) {
            navigation.menu.getItem(0).setCheckable(true)
            transaction.remove(_fragment)
            _fragment = supportFragmentManager.findFragmentByTag("map")!!
            for (i in 0 until supportFragmentManager.getBackStackEntryCount()) {
                supportFragmentManager.popBackStack()
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
            transaction.add(R.id.mapFrame, _fragment, name)
            transaction.addToBackStack(null);
        }
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
        super.onCreate(savedInstanceState)
        Looper.getMainLooper().thread.name = "PirateerMain"
        setContentView(R.layout.activity_map)
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
        if (savedInstanceState == null) {
            var arr = arrayOf("nav_plot.png", "nav_plotted.png")
            for (n in arr) {
                BitmapCache.instance.addBitmap(applicationContext, n, Bitmap.Config.ARGB_4444)
            }
            val transaction = supportFragmentManager.beginTransaction()
            var map = MapFragment()
            transaction.add(R.id.mapFrame, map, "map")
            transaction.commit()
            _fragment = map
            currMenuItem = R.id.navigation_map
            supportFragmentManager.addOnBackStackChangedListener {
                if (supportFragmentManager.backStackEntryCount == 0) {
                    navigation.selectedItemId = R.id.navigation_map
                } else {
                    _fragment = supportFragmentManager.fragments.last()
                }
            }
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        _fragment = supportFragmentManager.fragments.last()
        currMenuItem = savedInstanceState?.getInt("currMenuItem")
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
}
