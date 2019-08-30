package com.lanyard.pirateerlite.fragments

import android.os.Bundle
import android.util.DisplayMetrics
import android.view.*
import android.view.View.GONE
import android.widget.*
import com.lanyard.helpers.GridLayoutManagerAutofit
import com.lanyard.pirateerlite.R
import com.lanyard.pirateerlite.controllers.BoatController
import com.lanyard.pirateerlite.models.BoatModel
import com.lanyard.pirateerlite.models.JobModel
import com.lanyard.pirateerlite.models.TownModel
import com.lanyard.pirateerlite.singletons.Audio
import com.lanyard.pirateerlite.singletons.Game
import com.lanyard.pirateerlite.singletons.User
import com.lanyard.pirateerlite.views.CargoView
import com.lanyard.pirateerlite.views.JobView
import java.util.*
import kotlin.collections.ArrayList


class JobFragment : androidx.fragment.app.Fragment(), Game.GameListener {

    private val JOBCELL_HEADER = 0
    private val JOBCELL_JOB = 1
    private val JOBCELL_STORAGE = 2

    private lateinit var _cargoView: CargoView
    private lateinit var _cargo: Array<JobView>
    private lateinit var _jobView: androidx.recyclerview.widget.RecyclerView
    private lateinit var _adapter: JobAdapter
    private var _jobTimer: Date
    private var _jobThread: Thread? = null


    inner class JobAdapter(val jobs: ArrayList<JobModel?>) :
        androidx.recyclerview.widget.RecyclerView.Adapter<JobFragment.JobAdapter.JobViewHolder>() {

        fun setJobs(jobs: Iterable<JobModel?>) {
            this.jobs.clear()
            for (job in jobs) {
                this.jobs.add(job)
            }
        }

        inner class JobViewHolder(val view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view),
            View.OnClickListener {
            init {
                view.setOnClickListener(this)
            }

            override fun onClick(view: View) {
                val jobview = view.findViewById<JobView>(R.id.jobView)
                if (jobview != null) {
                    jobTouch(jobview as JobView)
                }
            }
        }


        override fun onBindViewHolder(p0: JobViewHolder, p1: Int) {
            when (getItemViewType(p1)) {
                JOBCELL_HEADER -> {
                    val jobLabel = p0.view.findViewById<TextView>(R.id.jobLabel)
                    if (p1 == 0) {
                        jobLabel.setText(context!!.resources.getText(R.string.jobs_jobs))
                    } else {
                        jobLabel.setText(context!!.resources.getText(R.string.jobs_storage))
                    }
                }
                JOBCELL_JOB -> {
                    val idx = p1 - 1
                    val jobview = p0.view.findViewById<JobView>(R.id.jobView)
                    var job: JobModel? = null
                    if (idx < jobs.size) {
                        job = jobs[idx]
                    }
                    jobview.job = job
                }
                JOBCELL_STORAGE -> {
                    val jobview = p0.view.findViewById<JobView>(R.id.jobView)
                    val idx = p1 - 2 - jobs.size
                    if (idx >= _storage.size) {
                        jobview.job = null
                    } else {
                        val job = _storage[idx]
                        jobview.job = job
                    }
                }
            }
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

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JobFragment.JobAdapter.JobViewHolder {
            val metrics = DisplayMetrics()
            activity!!.windowManager.defaultDisplay.getMetrics(metrics)
            if (viewType == JOBCELL_HEADER) {
                val cell = LayoutInflater.from(parent.context).inflate(R.layout.cell_job_header, parent, false)
                return JobViewHolder(cell)
            }

            val frame = FrameLayout(parent.context)
            val cell = LayoutInflater.from(parent.context).inflate(R.layout.cell_job_cell, frame, false)

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
            if (position == 0 || position == _jobs!!.size + 1) {
                return JOBCELL_HEADER
            } else if (position <= _jobs!!.size) {
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
        _jobTimer = Date()
        townModel = null
        boatController = null
    }

    override fun onDestroy() {
        super.onDestroy()
        _jobThread?.interrupt()
    }


    fun updateCargoValue() {
        val value = _cargoView.cargoValue
        _goldLabel.text = value[0].toString()
        _silverLabel.text = value[1].toString()
    }

    private fun updateJobTimer() {
        val holder = _jobView.findViewHolderForLayoutPosition(0)
        if (holder == null) {
            return
        }
        val label = holder.itemView.findViewById<TextView>(R.id.jobTimer)
        if (townModel!!.jobsDirty == false) {
            val diff = (User.jobInterval - (Date().time - User.instance.jobDate.time)) / 60
            var secs = diff / 10
            val mins = secs / 60
            secs = secs - mins * 60
            label.text = "New stock in " + mins + "." + secs + " minutes"
        } else {
            label.text = "New stock available"
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
            //NotificationCenter.default.removeObserver(self)
            //viewDidLoad()
            _adapter.notifyDataSetChanged()
        }
    }

    fun reloadJobs() {
        if (townModel!!.jobsDirty == true) {
            _jobs = townModel!!.jobs
            _adapter.setJobs(_jobs!!)
        }
        _adapter.notifyItemRangeChanged(1, _jobs!!.size)
        updateCargoValue()
    }

    fun updateJobs() {
        if (boatController == null) {
            _cargoPanel.visibility = GONE
            _jobs = townModel!!.jobs
            _storage = townModel!!.storage
            //_refreshControl.addTarget(self, action: #selector(reloadJobs(_:)), for: UIControl.Event.valueChanged)
            //jobView.addSubview(_refreshControl)
        } else if (boatController!!.isSailing != true) {
            townModel = boatController!!.model.town!!
            //townModel.delegate = self
            _jobs = townModel!!.jobs
            _storage = townModel!!.storage
            _cargo = _cargoView.setup(this.activity!!, _size, boatController!!)
            _cargoView.cargoListener = object : CargoView.CargoListener {
                override fun jobPressed(view: JobModel) : Boolean {
                    return cargoTouch(view)
                }
            }
        } else {
            _cargoPanel.visibility = GONE
            _jobs = boatController!!.model.cargo.toList()
        }
    }

    fun cargoTouch(job: JobModel) : Boolean {
        var idx = _jobs!!.indexOfFirst { it === job }
        var clear = false
        if (idx != -1) {
            _adapter.addJob(job, idx)
            val holder = _jobView.findViewHolderForLayoutPosition(idx + 1)
            if (holder != null) {
                val cell = holder.itemView.findViewById<JobView>(R.id.jobView)
                cell.job = job
            }
            clear = true
        } else {
            idx = _storage.indexOfFirst { it == null }
            if (idx != -1) {
                _storage[idx] = job
                val holder = _jobView.findViewHolderForLayoutPosition(idx + 2 + _jobs!!.size)
                if (holder != null) {
                    val cell = holder.itemView.findViewById<JobView>(R.id.jobView)
                    cell.job = job
                }
                clear = true
            }
        }
        Audio.instance.queueSound(R.raw.button_select)
        updateCargoValue()
        return clear
    }


    fun jobTouch(view: JobView) {
        if (townModel == null || boatController == null || view.job == null) {
            return
        }

        if (boatController!!.model.cargo.filter { it != null }.size < boatController!!.model.cargoSize) {
            _cargoView.addJob(view.job!!)
            _adapter.clearJob(view.job!!)
            Audio.instance.queueSound(R.raw.button_select)
            val idx = _storage.indexOfFirst { view.job === it }
            view.job = null
            if (idx != -1) {
                _storage[idx] = null
            }
        } else {
        }
        updateCargoValue()
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Game.instance.addGameListener(this)
        val metrics = DisplayMetrics()
        activity!!.windowManager.defaultDisplay.getMetrics(metrics)
        val viewManager = GridLayoutManagerAutofit(container!!.context, (120 * metrics.density).toInt())

        val view = inflater.inflate(R.layout.fragment_jobs, container, false)
        _jobView = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.jobTable)
        _cargoView = view.findViewById<CargoView>(R.id.cargoView)
        _goldLabel = view.findViewById<TextView>(R.id.goldLabel)
        _silverLabel = view.findViewById<TextView>(R.id.silverLabel)
        _cargoPanel = view.findViewById<FrameLayout>(R.id.cargoPanel)
        val swipe = view.findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.jobRefresh);
        swipe.setOnRefreshListener {
            reloadJobs()
            swipe.isRefreshing = false
        }

        updateJobs()
        val table_jobs = ArrayList(jobs)
        if (boatController != null && townModel != null) {
            for (boat in townModel!!.boats) {
                for (job in boat.cargo) {
                    if (job?.source === townModel) {
                        val idx = table_jobs.indexOfFirst { it === job }
                        if (idx >= 0) {
                            table_jobs[idx] = null
                        }
                    }
                }
            }
        }
        _adapter = JobAdapter(table_jobs)

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
            adapter = _adapter
        }

        if (townModel != null) {

            _jobThread = object : Thread() {

                override fun run() {
                    try {
                        while (!this.isInterrupted) {
                            Thread.sleep(1000)
                            activity?.runOnUiThread(Runnable {
                                updateJobTimer()
                            })
                        }
                    } catch (e: InterruptedException) {
                    }

                }
            }
            _jobThread?.start();
        }
        return view
    }
}