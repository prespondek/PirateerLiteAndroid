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

import androidx.lifecycle.Observer
import android.os.Bundle
import android.util.DisplayMetrics
import com.lanyard.pirateerlite.models.BoatModel
import com.lanyard.pirateerlite.singletons.Game
import com.lanyard.pirateerlite.singletons.Map
import com.lanyard.pirateerlite.singletons.User
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ActivityInfo.*
import android.content.res.Configuration.*
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import com.lanyard.canvas.BitmapStream
import com.lanyard.pirateerlite.data.BoatData
import com.lanyard.pirateerlite.data.StatsData
import com.lanyard.pirateerlite.data.TownData
import com.lanyard.pirateerlite.data.UserData
import com.lanyard.pirateerlite.models.JobModel
import com.lanyard.pirateerlite.singletons.Audio
import androidx.lifecycle.ViewModelProviders
import com.lanyard.library.SplinePath
import com.lanyard.pirateerlite.models.TownModel
import com.lanyard.pirateerlite.viewmodels.SplashViewModel


class SplashActivity : FragmentActivity() {
    var mapConfig : HashMap<String, Any>? = null
    lateinit private var _viewModel : SplashViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        if(getResources().getBoolean(R.bool.portrait_only)){
            setRequestedOrientation(SCREEN_ORIENTATION_PORTRAIT);
        }
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



    fun onPrimaryDataFetch() {
        var metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        Map.initialize(applicationContext,mapConfig!!,_viewModel.townData?.value!!, metrics.density)
        User.initialize(applicationContext,_viewModel.userdata?.value!![0], _viewModel.statdata?.value!!, _viewModel.boatData?.value!!)
        User.instance.addObserver(Game.instance)

        var count = 3
        var countDown = {
            count -= 1
            if (count == 0) {
                onSecondaryDataFetch()
            }
        }

        var boatIds = mutableListOf<Long>()
        User.instance.boats.mapTo(boatIds,{ it.id })
        var townIds = mutableListOf<Long>()
        Map.instance.towns.mapTo(townIds,{ it.id })

        _viewModel.fetchBoatJobs(boatIds).observe(this, Observer {

            var boat : BoatModel? = null
            var jobs = mutableListOf<JobModel>()
            for (data in it) {
                if (boat == null || data.boatid != boat.id) {
                    if (!jobs.isEmpty()) {
                        boat?.setCargo(jobs)
                    }
                    boat = User.instance.boats.find { it.id == data.boatid }
                }
                if (boat != null) {
                    jobs.add(JobModel(data.jobData))
                }
            }
            countDown()
        })
        _viewModel.fetchTownJobs(townIds).observe(this, Observer {
            var town : TownModel? = null
            var jobs = mutableListOf<JobModel>()
            for (data in it) {
                if (town == null || data.townid != town.id) {
                    if (!jobs.isEmpty()) {
                        town?.setJobs(jobs)
                    }
                    town = Map.instance.towns.find { it.id == data.townid }
                }
                if (town != null) {
                    jobs.add(JobModel(data.jobData))
                }
            }
            countDown()
        })

        _viewModel.fetchStorageJobs(townIds).observe(this, Observer {
            var town : TownModel? = null
            var jobs = mutableListOf<JobModel>()
            for (data in it) {
                if (town == null || data.townid != town!!.id) {
                    if (!jobs.isEmpty()) {
                        town?.setStorage(jobs)
                    }
                    town = Map.instance.towns.find { it.id == data.townid }
                }
                if (town != null) {
                    jobs.add(JobModel(data.jobData))
                }
            }
            countDown()
        })

    }

    fun onSecondaryDataFetch() {

        val intent = Intent(applicationContext, MapActivity::class.java)
        startActivity(intent)
        finish()
    }
}