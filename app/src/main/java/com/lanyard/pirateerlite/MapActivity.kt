package com.lanyard.pirateerlite

import android.graphics.Bitmap
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.util.DisplayMetrics
import com.lanyard.canvas.BitmapCache
import com.lanyard.canvas.BitmapStream
import com.lanyard.pirateerlite.fragments.BoatListFragment
import com.lanyard.pirateerlite.fragments.MapFragment
import com.lanyard.pirateerlite.fragments.MenuFragment
import com.lanyard.pirateerlite.fragments.WalletFragment
import com.lanyard.pirateerlite.models.BoatModel
import com.lanyard.pirateerlite.singletons.User
import com.lanyard.pirateerlite.singletons.Map
import com.lanyard.pirateeronline.R
import kotlinx.android.synthetic.main.activity_map.*


class MapActivity : AppCompatActivity() {

    companion object {
        lateinit var _instance : MapActivity
        val instance : MapActivity
        get() {
            return _instance
        }
    }

    var currFragment : Fragment? = null
    var currMenuItem : Int? = null

    init {
        _instance =  this
        BitmapStream.context = this
    }

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener {
        if ( currMenuItem == it.itemId ) {
            return@OnNavigationItemSelectedListener false
        }
        swapFragment(it.itemId)
        return@OnNavigationItemSelectedListener true
    }

    fun swapFragment( id: Int ) {
        val transaction = supportFragmentManager.beginTransaction()
        if ( currMenuItem == R.id.navigation_map ) {
            transaction.hide(currFragment!!)
            currMenuItem = null
        }
        if ( id == R.id.navigation_map ) {
            transaction.remove(currFragment!!)
            currFragment = supportFragmentManager.findFragmentByTag("map")!!
            transaction.show(currFragment!!)
        } else {
            var tag = ""
            if (id == R.id.navigation_menu) {
                currFragment = MenuFragment()
                tag = "menu"
            } else if (id == R.id.navigation_boats) {
                currFragment = BoatListFragment()
                tag = "boats"
            }
            if (currMenuItem == null) {
                transaction.add(R.id.mapFrame, currFragment!!,tag)
            } else {
                transaction.replace(R.id.mapFrame, currFragment!!,tag)
            }
        }
        transaction.commit()
        currMenuItem = id
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        Map.init(metrics.density)
        BoatModel.scale = metrics.density
        User.sharedInstance
        var arr = arrayOf("nav_plot.png","nav_plotted.png")
        for (n in arr) {
            BitmapCache.instance.addBitmap(n,Bitmap.Config.ARGB_4444)
        }

        setContentView(R.layout.activity_map)
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
        val fragmentManager = supportFragmentManager
        //val fragmentTransaction = fragmentManager.beginTransaction()
        //currFragment = MapFragment()
        currMenuItem =  R.id.navigation_map
        currFragment = fragmentManager.findFragmentByTag("map")
        (currFragment as MapFragment).wallet = fragmentManager.findFragmentById(R.id.wallet) as WalletFragment
        //fragmentTransaction.add(R.id.frag_container, currFragment!!,"map")
        //fragmentTransaction.commit()
    }

}
