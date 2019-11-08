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
import com.lanyard.pirateerlite.viewmodels.SplashViewModel


class SplashActivity : FragmentActivity() {
    var mapConfig : HashMap<String, Any>? = null
    private lateinit var _viewModel: SplashViewModel

    init {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (resources.getBoolean(R.bool.portrait_only)) {
            requestedOrientation = SCREEN_ORIENTATION_PORTRAIT
        }

        if (Map.isInitialized && User.isInitialized) {
            startMapActivity()
        }

        createNotificationChannel()
        mapConfig = Map.loadConfig(this)

        if (savedInstanceState == null) {
            var screenSize = resources.configuration.screenLayout and SCREENLAYOUT_SIZE_MASK
            if (screenSize == SCREENLAYOUT_SIZE_LARGE || screenSize == SCREENLAYOUT_SIZE_XLARGE) {
                // width > height, better to use Landscape
                requestedOrientation = SCREEN_ORIENTATION_UNSPECIFIED
            } else {
                requestedOrientation = SCREEN_ORIENTATION_PORTRAIT
            }
            var metrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(metrics)
            BitmapStream.density = metrics.density
            BitmapStream.densityDpi = metrics.densityDpi
            BoatModel.initialize(applicationContext)
            Game.initialize(applicationContext, mapConfig!!)
            Audio.initialize(applicationContext)
        }

        _viewModel = ViewModelProviders.of(this).get(SplashViewModel::class.java)

        _viewModel.dbReady.observe(this, Observer {
            var count = 4
            var countDown = {
                count -= 1
                if (count == 0) {
                    onPrimaryDataFetch()
                }
            }
            var observer = Observer<Any> { countDown() }
            _viewModel.userdata?.observe(this, observer)
            _viewModel.statdata?.observe(this, observer)
            _viewModel.boatData?.observe(this, observer)
            _viewModel.townData?.observe(this, observer)
        })
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
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

    fun onPrimaryDataFetch() {
        var metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        Map.initialize(applicationContext,mapConfig!!,_viewModel.townData?.value!!, metrics.density)
        User.initialize(applicationContext,_viewModel.userdata?.value!![0], _viewModel.statdata?.value!!, _viewModel.boatData?.value!!)
        User.instance.addListerner(Game.instance)

        var count = 3
        var countDown = {
            count -= 1
            if (count == 0) {
                startMapActivity()
            }
        }

        var boatIds = mutableListOf<Long>()
        User.instance.boats.mapTo(boatIds,{ it.id })
        var townIds = mutableListOf<Long>()
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

    fun startMapActivity() {
        var boatid = intent.getLongExtra("boatid", 0)
        val intent = Intent(applicationContext, MapActivity::class.java)
        intent.putExtra("boatid", boatid)
        startActivity(intent)
        finish()
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    }

}