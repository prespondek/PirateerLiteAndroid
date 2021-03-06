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

package com.lanyard.pirateerlite.fragments

import android.os.Bundle
import android.os.CountDownTimer
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.lanyard.helpers.GridLayoutManagerAutofit
import com.lanyard.pirateerlite.R
import com.lanyard.pirateerlite.controllers.BoatController
import com.lanyard.pirateerlite.models.BoatModel
import com.lanyard.pirateerlite.models.JobModel
import com.lanyard.pirateerlite.models.TownModel
import com.lanyard.pirateerlite.singletons.Audio
import com.lanyard.pirateerlite.singletons.Game
import com.lanyard.pirateerlite.singletons.Map
import com.lanyard.pirateerlite.singletons.User
import com.lanyard.pirateerlite.views.CargoView
import com.lanyard.pirateerlite.views.JobView
import java.util.*
import kotlin.collections.ArrayList

/**
 * If a boat is selected and moored at a town this fragment handles loading and unloading cargo off a boat.
 * If the boat is sailing it allows the user to see what is in the boats hold.
 * If opened from the TownInfoFragment it show all jobs the town has.
 * The top panel is the jobView which is a recyclerview that utilises a GridLayoutManager
 * and has CardViews for the job cells. The bottom panel is a Custom view with a
 * GestureRecogniser so the user can select a cargo slot by swiping the CardViews
 *
 * @author Peter Respondek
 *
 * @see R.layout.fragment_jobs
 * @see R.layout.cell_job_cell
 * @see JobView
 * @see GridLayoutManagerAutofit
 * @see CargoView
 */

class JobFragment : Fragment(), Game.GameListener {

    private val JOBCELL_HEADER = 0
    private val JOBCELL_JOB = 1
    private val JOBCELL_STORAGE = 2

    private lateinit var _cargoView: CargoView
    private lateinit var _cargo: Array<JobView>
    private lateinit var _jobView: RecyclerView
    private lateinit var _adapter: JobAdapter
    private lateinit var _jobTimeStamp: Date
    private lateinit var _jobTimer: CountDownTimer


    fun getRemainingJobTime(): Long {
        return User.jobInterval - (Date().time - _jobTimeStamp.time)
    }

    fun resetTimer() {
        if (this::_jobTimer.isInitialized == true) {
            _jobTimer.cancel()
        }
        _jobTimer = object : CountDownTimer(getRemainingJobTime(), 1000) {
            override fun onFinish() {
                updateJobTimer(0)
            }

            override fun onTick(millisUntilFinished: Long) {
                updateJobTimer(millisUntilFinished)
            }
        }
        _jobTimer.start()
    }


    inner class JobAdapter(val jobs: ArrayList<JobModel?>) :
        androidx.recyclerview.widget.RecyclerView.Adapter<JobFragment.JobAdapter.JobViewHolder>() {

        fun setJobs(jobs: Iterable<JobModel?>) {
            this.jobs.clear()
            for (job in jobs) {
                this.jobs.add(job)
            }
        }

        inner class JobViewHolder( val view: View ) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view),
            View.OnClickListener {
            init {
                view.setOnClickListener(this)
            }

            override fun onClick(view: View) {
                val jobview = view.findViewById<JobView>(R.id.jobView)
                if (jobview != null) {
                    jobTouch(jobview)
                }
            }
        }


        override fun onBindViewHolder(p0: JobViewHolder, p1: Int) {
            var job: JobModel? = null
            var jobview: JobView? = null
            when (getItemViewType(p1)) {
                JOBCELL_HEADER -> {
                    val jobLabel = p0.view.findViewById<TextView>(R.id.jobLabel)
                    if (p1 == 0) {
                        jobLabel.text = resources.getText(R.string.jobs_jobs)
                    } else {
                        jobLabel.text = resources.getText(R.string.jobs_storage)
                    }
                }
                JOBCELL_JOB -> {
                    val idx = p1 - 1
                    jobview = p0.view.findViewById<JobView>(R.id.jobView)
                    if (idx < jobs.size) {
                        job = jobs[idx]
                    }
                    jobview.job = job
                }
                JOBCELL_STORAGE -> {
                    jobview = p0.view.findViewById<JobView>(R.id.jobView)
                    val idx = p1 - 2 - jobs.size
                    if (idx >= _storage.size) {
                        jobview.job = null
                    } else {
                        job = _storage[idx]
                        jobview.job = job
                    }
                }
            }
            loadJobBimp(job, jobview)
        }

        fun clearJob(job: JobModel) {
            val idx = jobs.indexOfFirst { it === job }
            if (idx != -1) {
                jobs[idx] = null
            }
            notifyItemChanged(idx + 1)
        }

        fun addJob(job: JobModel, index: Int) {
            if (index >= 0 && index < jobs.size) {
                jobs[index] = job
            }
            notifyItemChanged(index + 1)
        }

        override fun onCreateViewHolder(vgroup: ViewGroup, viewType: Int): JobFragment.JobAdapter.JobViewHolder {
            val metrics = DisplayMetrics()

            activity?.windowManager?.defaultDisplay?.getMetrics(metrics)
            if (viewType == JOBCELL_HEADER) {
                val cell = LayoutInflater.from(vgroup.context).inflate(R.layout.cell_job_header, vgroup, false)
                return JobViewHolder(cell)
            }

            val frame = FrameLayout(vgroup.context)
            val cell = LayoutInflater.from(vgroup.context).inflate(R.layout.cell_job_cell, frame, false)

            frame.layoutParams =
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, cell.layoutParams.height)
            val params =
                FrameLayout.LayoutParams(cell.layoutParams.width, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER)
            val card = cell.findViewById<JobView>(R.id.jobView)
            card.layoutParams = params
            frame.addView(card)

            return JobViewHolder(frame)
        }

        override fun getItemViewType(position: Int): Int {
            if (position == 0 || position == jobs.size + 1) {
                return JOBCELL_HEADER
            } else if (position <= jobs.size) {
                return JOBCELL_JOB
            }
            return JOBCELL_STORAGE
        }

        override fun getItemCount(): Int {
            if (townModel != null) {
                return jobs.size + 2 + townModel!!.storageSize
            } else if (boatController == null) {
                return jobs.size + 1
            } else {
                return jobs.size + 1
            }
        }
    }

    fun loadJobBimp(job: JobModel?, jobview: JobView?) {
        val resources = context?.resources
        if (resources != null && job != null && jobview != null) {
            val element = JobModel.jobData.first { it[0] == job.type }[1]
            val res = resources.getIdentifier(element, "drawable", context?.packageName)
            jobview.setJobDrawable(resources.getDrawable(res, null), false)
        }
    }


    private lateinit var _cargoPanel: FrameLayout
    private lateinit var _goldLabel: TextView
    private lateinit var _silverLabel: TextView
    private var _jobs: List<JobModel?>?
    private lateinit var _storage: ArrayList<JobModel?>
    private var _size: Float

    val jobs: List<JobModel?>
        get() {
            return _jobs!!
        }
    var townModel: TownModel?
    var boatController: BoatController?

    init {
        _size = 0.0f
        _jobs = null
        townModel = null
        boatController = null
    }


    fun updateCargoValue() {
        if (boatController == null) { return }
        Game.instance.boatJobsChanged(boatController!!.model)
        if (_cargoPanel.visibility == VISIBLE) {
            val value = _cargoView.cargoValue
            _goldLabel.text = value[0].toString()
            _silverLabel.text = value[1].toString()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _jobTimer.cancel()
    }

    private fun updateJobTimer(millisUntilFinished: Long) {
        val holder = _jobView.findViewHolderForLayoutPosition(0)
        if (holder == null) {
            return
        }
        val label = holder.itemView.findViewById<TextView>(R.id.jobTimer)
        if (townModel == null) {
            label.text = ""
        } else if (millisUntilFinished > 0) {
            var secs = millisUntilFinished / 1000
            val mins = secs / 60
            secs -= mins * 60
            label.text = resources.getString(R.string.newStockTimer, mins, secs.toString().padStart(2, '0'))
        } else {
            label.text = resources.getString(R.string.newStock)
        }
    }

    override fun jobDelivered(boat: BoatModel, town: TownModel, gold: Int, silver: Int, quiet: Boolean) {
        if (boat === boatController?.model) {
            _jobs = boat.cargo.toList()
            _adapter.setJobs(_jobs!!)
        }
        _adapter.notifyDataSetChanged()
    }

    override fun boatArrived(boat: BoatModel, town: TownModel) {
        if (boat === boatController?.model) {
            updateJobs()
        }
    }

    override fun boatSailed(boat: BoatModel) {
        if (boat === boatController?.model) {
            updateJobs()
        }
    }

    fun reloadJobs() {
        val town = townModel
        if (town == null) {
            return
        }

        if (town.jobsDirty == true) {
            _jobTimeStamp = User.instance.jobDate
            _jobs = town.jobs
            _adapter.setJobs(_jobs!!)
        }
        _adapter.notifyItemRangeChanged(1, _jobs!!.size)
        updateCargoValue()
        resetTimer()
    }

    fun updateJobs() {
        val boat = boatController
        val town = townModel
        if (boat == null && town != null) {
            _cargoPanel.visibility = GONE
            _jobs = town.jobs
            _storage = town.storage
            _adapter = JobAdapter(ArrayList(_jobs))
        } else if (boat != null && boat.isSailing != true) {
            townModel = boat.model.town
            _jobs = townModel!!.jobs
            _storage = townModel!!.storage
            val table_jobs = ArrayList(jobs)
            for (townBoat in townModel!!.boats) {
                for (job in townBoat.cargo) {
                    if (job?.source === townModel) {
                        val idx = table_jobs.indexOfFirst { it === job }
                        if (idx >= 0) {
                            table_jobs[idx] = null
                        }
                    }
                }
            }
            _adapter = JobAdapter(table_jobs)
            _cargo = _cargoView.setup(this.activity!!, _size, boatController!!)
            for (cargo in boatController!!.model.cargo) {
                if (cargo != null) {
                    val jobview = _cargoView.setJob(cargo)
                    loadJobBimp(cargo, jobview)
                }
            }
            _cargoView.cargoListener = object : CargoView.CargoListener {
                override fun jobPressed(view: JobModel) : Boolean {
                    return cargoTouch(view)
                }
            }
            _cargoPanel.visibility = VISIBLE
            updateCargoValue()
        } else if (boat != null && boat.isSailing == true) {
            townModel = null
            updateJobTimer(0)
            _cargoPanel.visibility = GONE
            _storage = ArrayList()
            _jobs = boatController!!.model.cargo.toList()
            _adapter = JobAdapter(ArrayList(_jobs))
        } else {
            _adapter = JobAdapter(ArrayList())
        }
        _jobView.adapter = _adapter
        resetTimer()
    }


    fun cargoTouch(job: JobModel) : Boolean {
        var idx = _jobs!!.indexOfFirst { it === job }
        var clear = false
        var holder: RecyclerView.ViewHolder? = null
        if (idx != -1) {
            _adapter.addJob(job, idx)
            holder = _jobView.findViewHolderForLayoutPosition(idx + 1)
            clear = true
        } else {
            idx = _storage.indexOfFirst { it == null }
            if (idx != -1) {
                _storage[idx] = job
                townModel?.storage!![idx] = job
                holder = _jobView.findViewHolderForLayoutPosition(idx + 2 + _jobs!!.size)
                clear = true
            }
        }
        if (holder != null) {
            val jobview = holder.itemView.findViewById<JobView>(R.id.jobView)
            jobview.job = job
            loadJobBimp(job, jobview)
        }
        Audio.instance.queueSound(R.raw.button_select)
        if (clear == true) {
            val idn = boatController?.model?.cargo?.indexOfFirst { it === job }
            if (idn != null && idn > -1) {
                boatController?.model?.cargo?.set(idn, null)
            }
        }
        updateCargoValue()
        return clear
    }


    fun jobTouch(view: JobView) {
        if (townModel == null || boatController == null || view.job == null) {
            return
        }

        println("Job Touch")
        for (cargo in boatController!!.model.cargo) {
            println(cargo.toString())
        }
        if (boatController!!.model.cargo.filter { it != null }.size < boatController!!.model.cargoSize) {
            val jobview = _cargoView.addJob(view.job!!)
            loadJobBimp(view.job, jobview)
            _adapter.clearJob(view.job!!)
            Audio.instance.queueSound(R.raw.button_select)
            val idx = _storage.indexOfFirst { view.job === it }
            view.job = null
            if (idx != -1) {
                townModel?.storage!![idx] = null
                _storage[idx] = null
            }
        } else {
        }
        updateCargoValue()
    }

    override fun onDetach() {
        super.onDetach()
        Game.instance.removeGameListener(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val metrics = DisplayMetrics()
        activity!!.windowManager.defaultDisplay.getMetrics(metrics)
        val viewManager = GridLayoutManagerAutofit(container!!.context, (120 * metrics.density).toInt())
        //_viewModel = ViewModelProviders.of(this).get(JobViewModel::class.java)
        _jobTimeStamp = User.instance.jobDate
        val view = inflater.inflate(R.layout.fragment_jobs, container, false)
        _jobView = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.jobTable)
        _cargoView = view.findViewById<CargoView>(R.id.cargoView)
        _goldLabel = view.findViewById<TextView>(R.id.goldLabel)
        _silverLabel = view.findViewById<TextView>(R.id.silverLabel)
        _cargoPanel = view.findViewById<FrameLayout>(R.id.cargoPanel)
        val swipe = view.findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.jobRefresh)
        swipe.setOnRefreshListener {
            reloadJobs()
            swipe.isRefreshing = false
        }

        viewManager.spanSizeLookup = object : androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                val span = viewManager.spanCount
                if (_adapter.getItemViewType(position) == JOBCELL_HEADER) {
                    return span
                }
                return 1
            }
        }

        _jobView.apply {
            setHasFixedSize(true)
            layoutManager = viewManager
        }

        if ( savedInstanceState != null ) {
            val boatId = savedInstanceState["boatController"] as Long?
            if ( boatId != null ) {
                val map = fragmentManager!!.findFragmentByTag("map") as MapFragment
                boatController = map.boatControllerForId(boatId)
            }
            val townId = savedInstanceState["townModel"] as Long?
            if ( townId != null ) {
                townModel = Map.instance.towns.find { it.id == townId }
            }
        }

        updateJobs()
        updateCargoValue()

        Game.instance.addGameListener(this)

        return view
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val boat = boatController
        if (boat != null) {
            outState.putLong("boatController", boat.model.id)
        }
        val town = townModel
        if (town != null) {
            outState.putLong("townModel", town.id)
        }
    }
}