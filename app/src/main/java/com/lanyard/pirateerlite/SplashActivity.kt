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

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
import android.content.res.Configuration.*
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.lanyard.canvas.BitmapStream
import com.lanyard.pirateerlite.models.BoatModel
import com.lanyard.pirateerlite.models.JobModel
import com.lanyard.pirateerlite.singletons.Audio
import com.lanyard.pirateerlite.singletons.Game
import com.lanyard.pirateerlite.singletons.Map
import com.lanyard.pirateerlite.singletons.User
import com.lanyard.pirateerlite.viewmodels.SplashActivityViewModel

/**
 * Throws up the splash screen when the app is launched and fetches all the
 * necessary data to construct the UI including database queries etc.
 *
 * @author Peter Respondek
 */

class SplashActivity : FragmentActivity() {
    var mapConfig : HashMap<String, Any>? = null
    private lateinit var _viewModel: SplashActivityViewModel

    /**
     * Make initial setup and call to our Room database. Naturally the viewmodel holds persistent Livedata
     * which this activity works with. If the activity is trashed duplicate calls wont be made.
     */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // lock our view to portrait it the screen does not support it.
        if (resources.getBoolean(R.bool.portrait_only)) {
            requestedOrientation = SCREEN_ORIENTATION_PORTRAIT
        }

        // If the activity is started via a notification the app may already be started.
        if (Map.isInitialized && User.isInitialized) {
            startMapActivity()
            return
        }

        createNotificationChannel()
        mapConfig = Map.loadConfig(this)

        if (savedInstanceState == null) {
            val screenSize = resources.configuration.screenLayout and SCREENLAYOUT_SIZE_MASK
            if (screenSize == SCREENLAYOUT_SIZE_LARGE || screenSize == SCREENLAYOUT_SIZE_XLARGE) {
                // width > height, better to use Landscape
                requestedOrientation = SCREEN_ORIENTATION_UNSPECIFIED
            } else {
                requestedOrientation = SCREEN_ORIENTATION_PORTRAIT
            }
            val metrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(metrics)
            BitmapStream.density = metrics.density
            BitmapStream.densityDpi = metrics.densityDpi
            BoatModel.initialize(applicationContext)
            Game.initialize(applicationContext, mapConfig!!)
            Audio.initialize(applicationContext)
        }

        _viewModel = ViewModelProviders.of(this).get(SplashActivityViewModel::class.java)

        _viewModel.dbReady.observe(this, Observer {
            var count = 4
            val countDown = {
                count -= 1
                if (count == 0) {
                    onPrimaryDataFetch()
                }
            }
            val observer = Observer<Any> { countDown() }
            _viewModel.userdata?.observe(this, observer)
            _viewModel.statdata?.observe(this, observer)
            _viewModel.boatData?.observe(this, observer)
            _viewModel.townData?.observe(this, observer)
        })
    }

    /**
     * Create the NotificationChannel, but only on API 26+ because
     * the NotificationChannel class is new and not in the support library.
     * This allows the user to turn off specific notifications.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.boatArriveChannelName)
            val descriptionText = getString(R.string.boatArriveChannelDesc)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(getString(R.string.channelId), name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Once we have our initial data and prepopulated the database it if needed, we can make queries
     * about town and boat jobs/cargo/storage and start user and game singletons.
     */

    private fun onPrimaryDataFetch() {
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        Map.initialize(mapConfig!!,_viewModel.townData?.value!!, metrics.density)
        User.initialize(applicationContext,_viewModel.userdata?.value!![0], _viewModel.statdata?.value!!, _viewModel.boatData?.value!!)
        User.instance.addListerner(Game.instance)

        var count = 3
        val countDown = {
            count -= 1
            if (count == 0) {
                startMapActivity()
            }
        }

        val boatIds = mutableListOf<Long>()
        User.instance.boats.mapTo(boatIds,{ it.id })
        val townIds = mutableListOf<Long>()
        Map.instance.towns.mapTo(townIds,{ it.id })

        _viewModel.fetchBoatJobs(boatIds).observe(this, Observer {
            if (count > 0) {
                for (boat in User.instance.boats) {
                    val boatJobs = it.filter { boat.id == it.boatid }.map { JobModel(it.jobData) }
                    boat.setCargo(boatJobs)
                }
                countDown()
            }
        })
        _viewModel.fetchTownJobs(townIds).observe(this, Observer {
            if (count > 0) {
                for (town in Map.instance.towns) {
                    val townJobs = it.filter { town.id == it.townid }.map { JobModel(it.jobData) }
                    town.setJobs(townJobs)
                }
                countDown()
            }
        })

        _viewModel.fetchStorageJobs(townIds).observe(this, Observer {
            if (count > 0) {
                for (town in Map.instance.towns) {
                    val townJobs = it.filter { town.id == it.townid }.map { JobModel(it.jobData) }
                    town.setStorage(townJobs)
                }
                countDown()
            }
        })

    }

    /**
     * Start the map activity. If we started the activity via a local notification the intent
     * contains data we need to focus the map on the selected boat. We pass that data through to the map intent.
     */

    private fun startMapActivity() {
        val boatid = intent.getLongExtra("boatid", 0)
        val intent = Intent(applicationContext, MapActivity::class.java)
        intent.putExtra("boatid", boatid)
        startActivity(intent)
        finish()
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    }

}