package com.lanyard.pirateerlite

import androidx.lifecycle.Observer
import android.os.Bundle
import android.util.DisplayMetrics
import com.lanyard.pirateerlite.models.BoatModel
import com.lanyard.pirateerlite.singletons.Game
import com.lanyard.pirateerlite.singletons.Map
import com.lanyard.pirateerlite.singletons.User
import android.content.Intent
import android.content.pm.ActivityInfo.*
import android.content.res.Configuration.*
import androidx.fragment.app.FragmentActivity
import com.lanyard.canvas.BitmapStream
import com.lanyard.pirateerlite.data.BoatData
import com.lanyard.pirateerlite.data.StatsData
import com.lanyard.pirateerlite.data.TownData
import com.lanyard.pirateerlite.data.UserData
import com.lanyard.pirateerlite.models.JobModel
import com.lanyard.pirateerlite.singletons.Audio

class SplashActivity : FragmentActivity(), Game.GameListener {
    var mapConfig : HashMap<String, Any>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mapConfig = Map.loadConfig(this)
        if (savedInstanceState == null) {
            var screenSize = getResources().getConfiguration().screenLayout and SCREENLAYOUT_SIZE_MASK
            if (screenSize == SCREENLAYOUT_SIZE_LARGE || screenSize == SCREENLAYOUT_SIZE_XLARGE) {
                // width > height, better to use Landscape
                setRequestedOrientation(SCREEN_ORIENTATION_UNSPECIFIED);
            } else {
                setRequestedOrientation(SCREEN_ORIENTATION_PORTRAIT);
            }
            var metrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(metrics)
            BitmapStream.density = metrics.density
            BitmapStream.densityDpi = metrics.densityDpi
            BoatModel.initialize(applicationContext)
            Game.initialize(applicationContext, mapConfig!!)
            Audio.initialize(applicationContext)

        }
        Game.instance.addGameListener(this)
    }

    override fun onDatabaseCreated () {
        var userdata = Game.instance.db.userDao().getUser()
        var statdata = Game.instance.db.statsDao().getStats()
        var boatData = Game.instance.db.boatDao().getBoats()
        var townData = Game.instance.db.townDao().getTowns()
        var count = 4
        var countDown = {
            count -= 1
            if (count == 0) {
                onPrimaryDataFetch(userdata.value!!, statdata.value!!, boatData.value!!, townData.value!!)
            }
        }
        userdata.observe(this, Observer {
            userdata.removeObservers(this)
            countDown()
        })
        statdata.observe(this, Observer {
            statdata.removeObservers(this)
            countDown()
        })
        boatData.observe(this, Observer {
            boatData.removeObservers(this)
            countDown()
        })
        townData.observe(this, Observer {
            townData.removeObservers(this)
            countDown()
        })
    }

    fun onPrimaryDataFetch(userData: Array<UserData>, statsData: Array<StatsData>, boatData: Array<BoatData>, townData: Array<TownData>) {
        var metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        Map.initialize(applicationContext,mapConfig!!, townData, metrics.density)
        User.initialize(applicationContext,userData[0], statsData, boatData)
        User.instance.addObserver(Game.instance)
        BoatModel.scale = metrics.density
        var count = User.instance.boats.size + Map.instance.towns.size * 2
        var countDown = {
            count -= 1
            if (count == 0) {
                onSecondaryDataFetch()
            }
        }
        for (boat in User.instance.boats) {
            var boatJobs = Game.instance.db.boatJobDao().getJobs(boat.id)
            boatJobs.observe(this, Observer {
                boatJobs.removeObservers(this)
                var jobs = List<JobModel>(boatJobs.value!!.size, {
                    JobModel(boatJobs.value!![it].jobData)
                })
                boat.setCargo (jobs)
                countDown()
            })
        }
        for (town in Map.instance.towns) {
            var townJobs = Game.instance.db.townJobDao().getJobs(town.id)
            townJobs.observe(this, Observer {
                townJobs.removeObservers(this)
                var jobs = townJobs.value!!.mapNotNull { if (it.dateCreated >= town.jobsTimeStamp) JobModel(it.jobData) else null }
                town.setJobs(jobs)
                countDown()
            })
        }
        for (town in Map.instance.towns) {
            var storageJobs = Game.instance.db.storageJobDao().getJobs(town.id)
            storageJobs.observe(this, Observer {
                storageJobs.removeObservers(this)
                var jobs = List<JobModel>(storageJobs.value!!.size, {
                    JobModel(storageJobs.value!![it].jobData)
                })
                town.setStorage(jobs)
                countDown()
            })
        }

    }

    fun onSecondaryDataFetch() {
        val intent = Intent(applicationContext, MapActivity::class.java)
        startActivity(intent)
        finish()
    }
}