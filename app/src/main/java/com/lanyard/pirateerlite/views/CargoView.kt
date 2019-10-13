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

package com.lanyard.pirateerlite.views

import android.app.Activity
import android.content.Context
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import android.util.AttributeSet
import android.util.DisplayMetrics
import com.lanyard.pirateerlite.R
import com.lanyard.pirateerlite.controllers.BoatController
import com.lanyard.pirateerlite.models.JobModel
import android.content.Context.LAYOUT_INFLATER_SERVICE
import androidx.core.view.GestureDetectorCompat
import android.transition.*
import com.lanyard.helpers.popLast
import android.view.*
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import com.lanyard.pirateerlite.models.BoatModel
import com.lanyard.pirateerlite.models.TownModel
import com.lanyard.pirateerlite.singletons.Audio


class CargoView : ConstraintLayout, Transition.TransitionListener {
    constructor(context: Context) : super(context) {}
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {}

    interface CargoListener {
        fun jobPressed(job: JobModel) : Boolean { return false }
    }
    var cargoListener : CargoListener? = null

    private inner class CargoGestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onFling(event1: MotionEvent, event2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            if (_stage == 0) {
                swipeStarted()
            }
            return true
        }

        override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
            if (cargoListener != null) {
                val view = _views.last()
                if (view.job != null && cargoListener!!.jobPressed(view.job!!)) {
                    view.job = null
                }
                updateCells()
            }
            return super.onSingleTapConfirmed(e)
        }

    }

    override fun onTransitionStart(transition: Transition) {}

    override fun onTransitionEnd(transition: Transition) {
        if (_stage == 2) {
            _stage = 0
            updateCells()
            return
        }
        val transition = ChangeBounds()
        transition.interpolator = DecelerateInterpolator(1.0f)
        transition.duration = 300
        transition.addListener(this)
        _stage += 1
        var params = ConstraintSet()
        params.clone(this)
        params.clear(_views.last().id, ConstraintSet.START)
        params.connect(_views.last().id, ConstraintSet.END, id, ConstraintSet.END, _views.last().width / 6 * _views.size)
        _views.add(0, _views.popLast())
        _index.add(0, _index.popLast())
        if (_views.size > 1) {
            for (i in 0 until _views.size) {
                params.setElevation(_views[i].id, 4.0f * (i + 1))
            }
        }
        params.applyTo(this)
        TransitionManager.beginDelayedTransition(this, transition)
    }

    override fun onTransitionCancel(transition: Transition) {}

    override fun onTransitionPause(transition: Transition) {}

    override fun onTransitionResume(transition: Transition) {}

    fun addJob(job: JobModel) {
        if (_views.indexOfLast { it.job === job } == -1) {
            val idx = _views.indexOfLast { it.job === null }
            _views[idx].job = job
            _boat.cargo[_index[idx]] = job
            if (updateCells()) {
                Audio.instance.queueSound(R.raw.cargo_bonus)
            }
        }
    }

    fun updateCells(): Boolean {
        var bonus = false
        for (view in _views) {
            var button = view.findViewById<FrameLayout>(R.id.bgFrame)
            button.setOnTouchListener(null)
            val views = _views.filter { it.job != null && it.job?.destination === view.job?.destination }
            if (views.size == _views.size) {
                for (cell in views) {
                    cell.bonus(true)
                    bonus = true
                }
            } else {
                view.bonus(false)
            }
        }
        var button = _views.last().findViewById<FrameLayout>(R.id.bgFrame)
        button.setOnTouchListener { v, event -> !_detector.onTouchEvent(event) }
        return bonus
    }


    fun swipeStarted() {
        var button = _views.last().findViewById<FrameLayout>(R.id.bgFrame)
        button.setOnTouchListener (null)

        val transition = ChangeBounds()
        transition.interpolator = DecelerateInterpolator(1.0f)
        transition.duration = 500
        transition.addListener(this)

        var params = ConstraintSet()
        params.clone(this)
        params.connect(_views.last().id, ConstraintSet.START, id, ConstraintSet.START, 0)
        params.clear(_views.last().id, ConstraintSet.END)
        _stage += 1
        if (_views.size > 1) {
            for (i in 0 until _views.size - 1) {
                params.clear(_views[i].id, ConstraintSet.END)
                params.connect(
                    _views[i].id,
                    ConstraintSet.END,
                    id,
                    ConstraintSet.END,
                    _views[i].width / 6 * (_views.size - (i + 1))
                )
            }
        }

        params.applyTo(this)
        TransitionManager.beginDelayedTransition(this, transition)
    }

    private lateinit var _detector: GestureDetectorCompat
    private var _orderDistance: Float = 0.0f
    private var _views = ArrayList<JobView>()
    private var _stage = 0
    private var _index = ArrayList<Int>()
    private lateinit var _boat : BoatModel

    val views: ArrayList<JobView>
        get() {
            return _views
        }
    //var delegate : CargoViewDelegate?
    val cargoValue: Array<Int>
        get() {
            var gold = 0
            var silver = 0
            var destinations = mutableSetOf<TownModel?>()
            for (job in _boat.cargo) {
                destinations.add(job?.destination)
                if (job != null) {
                    if (job.isGold) {
                        gold += job.value
                    } else {
                        silver += job.value
                    }
                }
            }
            if (destinations.size == 1 && destinations.first() != null) {
                gold = (gold * 1.25f).toInt()
                silver = (silver * 1.25f).toInt()
            }
            return arrayOf(gold, silver)
        }

    fun setup(activity: Activity, height: Float, boat: BoatController): Array<JobView> {
        _detector = GestureDetectorCompat(context, CargoGestureListener())
        _boat = boat.model
        var metrics = DisplayMetrics()
        activity.windowManager.defaultDisplay.getMetrics(metrics)

        _views = ArrayList<JobView>()


        var width = 0
        for (i in 0 until _boat.cargoSize) {
            val inflater = context.getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val cell = inflater.inflate(R.layout.cell_job_cell, this, false) as JobView
            width = cell.layoutParams.width
            cell.id = View.generateViewId()
            cell.job = _boat.cargo[i]
            addView(cell)
            var params = ConstraintSet()
            params.clone(this)
            params.connect(cell.id, ConstraintSet.BOTTOM, id, ConstraintSet.BOTTOM, 0)
            params.connect(cell.id, ConstraintSet.TOP, id, ConstraintSet.TOP, 0)
            params.connect(cell.id, ConstraintSet.END, id, ConstraintSet.END, width / 6 * (boat.model.cargoSize - i))
            cell.elevation = i + 1.toFloat() * 4
            params.applyTo(this)
            _views.add(cell)
            _index.add(i)
        }
        layoutParams.width = width * 2 + (boat.model.cargoSize * width / 6)
        _orderDistance = boat.model.cargoSize.toFloat()
        updateCells()
        return _views.toTypedArray()
    }
}