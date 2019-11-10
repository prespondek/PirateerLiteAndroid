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

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProviders
import androidx.transition.ChangeBounds
import androidx.transition.Transition
import androidx.transition.TransitionListenerAdapter
import androidx.transition.TransitionManager
import com.lanyard.canvas.*
import com.lanyard.helpers.*
import com.lanyard.library.Edge
import com.lanyard.library.SuperScrollView
import com.lanyard.pirateerlite.MapActivity
import com.lanyard.pirateerlite.R
import com.lanyard.pirateerlite.controllers.BoatController
import com.lanyard.pirateerlite.controllers.TownController
import com.lanyard.pirateerlite.models.BoatModel
import com.lanyard.pirateerlite.models.TownModel
import com.lanyard.pirateerlite.models.WorldNode
import com.lanyard.pirateerlite.singletons.Audio
import com.lanyard.pirateerlite.singletons.Game
import com.lanyard.pirateerlite.singletons.Map
import com.lanyard.pirateerlite.singletons.User
import com.lanyard.pirateerlite.viewmodels.MapViewModel
import com.lanyard.pirateerlite.views.BoatView
import com.lanyard.pirateerlite.views.MapScrollView
import com.lanyard.pirateerlite.views.MapView
import com.lanyard.pirateerlite.views.TownView
import kotlinx.android.synthetic.main.activity_map.*
import kotlinx.android.synthetic.main.fragment_map.*

class MapFragment : Fragment(), Game.GameListener, User.UserListener {

    enum class Mode {
        plot, map, track, nontrack, build, buy
    }

    private var _boatControllers = ArrayList<BoatController>()
    private var _townControllers = ArrayList<TownController>()
    private var _density: Float = 0.0f
    private var _timer = Handler()
    private var _animating: Boolean = false
    private val _marketNotification = object : Runnable {
        override fun run() {
            val time = SystemClock.uptimeMillis() + User.instance.millisToMarketDate
            _timer.postAtTime(this, time)
            moveNotifyBox(true, R.id.market_text)
        }
    }
    private val _jobNotification = object : Runnable {
        override fun run() {
            val time = SystemClock.uptimeMillis() + User.instance.millisToJobDate
            _timer.postAtTime(this, time)
            moveNotifyBox(true, R.id.job_text)
        }
    }


    private var _selectedBoat: BoatController?
        get() {
            return _boatControllers.find { it.model == _viewModel.selectedBoat }
        }
        set(value) {
            _viewModel.selectedBoat = value?.model
        }

    private lateinit var _viewModel: MapViewModel
    private lateinit var _cargoButton: ImageButton
    private lateinit var _sailButton: ImageButton
    private lateinit var _cancelButton: ImageButton
    private lateinit var _scene: MapView
    private lateinit var _scrollView: MapScrollView
    private lateinit var _toolTip: TextView


    private val _cargoClickListener = View.OnClickListener {
        if (fragmentManager?.primaryNavigationFragment?.tag != "jobs") {
            val frag = (activity as MapActivity).swapFragment(R.id.holdButton) as JobFragment
            frag.boatController = _selectedBoat!!
        }
    }

    private var _touchListener = View.OnTouchListener { _, event ->
        stopTracking()
        false
    }

    // changing the scrollview position unless the view has done being laid out doesn't do a damn thing
    private val _viewListener = object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            // layout gets called twice on config change, the first time views are not measured so skip it.
            if (_scrollView.width == 0) return

            _scene.clearPlot()
            _scene.clearJobMarkers()
            val selectedBoat = _viewModel.selectedBoat
            var position = _viewModel.position

            if (position == null) {
                position = _boatControllers[0].view.sprite.position
            }
            var reposition = true
            val curr_mode = mode
            if (curr_mode == Mode.build || curr_mode == Mode.buy) {
                buildBoat()
            } else if (curr_mode == Mode.plot || curr_mode == Mode.track || curr_mode == Mode.nontrack) {
                val boatCourse = ArrayList(_viewModel.boatCourse)
                reposition = !_viewModel.trackBoat
                boatSelected(boatControllerForModel(selectedBoat!!), false)
                if (curr_mode == Mode.plot) {
                    if (boatCourse.size > 1) {
                        for (i in 1 until boatCourse.size) {
                            townSelected(townControllerForModel(boatCourse[i]))
                        }
                    }
                } else if (curr_mode == Mode.track) {
                    reposition = false
                    startTracking(_selectedBoat!!)
                } else {
                    reposition = true
                    stopTracking()
                }
            }

            if (reposition) {
                _scrollView.focusPosition(position, false)
            }
            _scrollView.viewTreeObserver.removeOnGlobalLayoutListener(this)
        }
    }

    val mode: Mode
        get () {
            val buildType = _viewModel.buildType
            val buildParts = _viewModel.buildParts
            val selectedBoat = _selectedBoat

            if (selectedBoat != null) {
                if (selectedBoat.model.town != null) {
                    return Mode.plot
                } else if (_viewModel.trackBoat == false) {
                    return Mode.nontrack
                } else {
                    return Mode.track
                }
            } else if (buildType != null && buildParts != null) {
                if (buildParts.size > 1) {
                    return Mode.build
                } else {
                    return Mode.buy
                }
            } else {
                return Mode.map
            }
        }

    override fun onDestroyView() {
        super.onDestroyView()
        for (boat in _boatControllers) {
            boat.destroy()
        }
    }


    private val _onBackStackChangedListener = FragmentManager.OnBackStackChangedListener {
        val frag = fragmentManager?.primaryNavigationFragment
        if (frag != null && frag.tag != "map" && (mode == Mode.build || mode == Mode.buy)) {
            reset()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _viewModel = ViewModelProviders.of(this).get(MapViewModel::class.java)

        if (savedInstanceState == null) {
            BitmapCache.instance.addBitmap(this.context!!, R.drawable.gold_piece, Bitmap.Config.ARGB_4444)
            BitmapCache.instance.addBitmap(this.context!!, R.drawable.silver_piece, Bitmap.Config.ARGB_4444)
        }

        Game.instance.addGameListener(this)
        User.instance.addListerner(this)
        fragmentManager?.addOnBackStackChangedListener(_onBackStackChangedListener)

        val view = inflater.inflate(R.layout.fragment_map, container, false)
        _scrollView = view.findViewById<MapScrollView>(R.id.vscrollview)
        _scene = view.findViewById<MapView>(R.id.mapView)
        _scrollView.scene = _scene

        _sailButton = view.findViewById(R.id.plotButton) as ImageButton
        _sailButton.setOnClickListener { this.sailButtonPressed() }
        _cancelButton = view.findViewById(R.id.cancelButton)
        _cancelButton.setOnClickListener { this.cancelButtonPressed() }
        _cargoButton = view.findViewById(R.id.holdButton)
        _cargoButton.setOnClickListener(_cargoClickListener)
        _toolTip = view.findViewById(R.id.toolTip) as TextView
        val mapframe = view.findViewById(R.id.mapframe) as FrameLayout
        val buttonlayout = view.findViewById(R.id.buttonlayout) as ConstraintLayout

        val metrics = DisplayMetrics()
        activity?.windowManager?.defaultDisplay?.getMetrics(metrics)
        _density = metrics.density

        mapframe.layoutParams.width = _scene.scene!!.size.width
        mapframe.layoutParams.height = _scene.scene!!.size.height

        mapframe.minimumWidth = _scene.scene!!.size.width
        mapframe.minimumHeight = _scene.scene!!.size.height

        _scrollView.setOnScrollChangeListener(object : SuperScrollView.OnScrollChangeListener {
            override fun onScrollChange(
                v: SuperScrollView,
                scrollX: Int,
                scrollY: Int,
                oldScrollX: Int,
                oldScrollY: Int
            ) {
                _scene.position = -Point(scrollX, scrollY) + _scene.padding
            }
        })
        _scrollView.setOnTouchListener(_touchListener)

        Audio.instance.queueSound(R.raw.bg_map, true)
        TownView.setup(this.context!!)
        TownController.setController(this)
        for (vert in Map.instance.graph.vertices) {
            val town = vert.data as? TownModel
            if (town == null) {
                continue
            }

            val button = Button(activity)
            button.id = View.generateViewId()
            button.tag = town
            buttonlayout.addView(button)

            button.layoutParams.width = (64 * _density).toInt()
            button.layoutParams.height = (64 * _density).toInt()
            button.background = null

            val params = ConstraintSet()
            params.clone(buttonlayout)

            val button_width = (32 * _density).toInt()
            params.connect(
                button.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID,
                ConstraintSet.TOP, (vert.position.y + _scene.padding.height - button_width)
            )
            params.connect(
                button.id, ConstraintSet.LEFT, ConstraintSet.PARENT_ID,
                ConstraintSet.LEFT, (vert.position.x + _scene.padding.width - button_width)
            )
            params.constrainWidth(button.id, button_width * 2)
            params.constrainHeight(button.id, button_width * 2)
            params.applyTo(buttonlayout)

            val sprite = TownView()
            sprite.position.set(vert.position.x, vert.position.y)
            sprite.zOrder = 2
            _scene.addChild(sprite)

            val label = CanvasLabel(town.name, null)
            label.position.set(
                vert.position.x,
                vert.position.y - context!!.resources.getDimensionPixelSize(R.dimen.town_label_offset)
            )
            label.fontSize = context!!.resources.getDimensionPixelSize(R.dimen.town_label_size).toFloat()
            label.fontStyle = Paint.Style.FILL_AND_STROKE
            label.strokeWidth = context!!.resources.getDimensionPixelSize(R.dimen.town_label_stroke).toFloat()
            label.zOrder = 2
            label.typeface = Typeface.DEFAULT_BOLD
            _scene.addChild(label)

            val townCtrl = TownController(town, button, sprite)
            _townControllers.add(townCtrl)
            townCtrl.state = TownController.State.unselected
        }

        BoatController.setController(this)
        for (boatModel in User.instance.boats) {
            addBoat(boatModel)
        }

        _scrollView.viewTreeObserver.addOnGlobalLayoutListener(_viewListener)


        val args = arguments
        if (args != null) {
            _viewModel.selectedBoat = User.instance.boats.find { it.id == args.getLong("selectedBoat") }
            _viewModel.trackBoat = true
        }

        return view
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        val jobFrag = fragmentManager?.findFragmentByTag("job") as? JobFragment
        if (jobFrag != null) {
            jobFrag.boatController = _selectedBoat
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (this::_scene.isInitialized) {
            _scene.stopThread()
        }
    }

    override fun onPause() {
        super.onPause()
        _viewModel.position = _scrollView.currentPosition()
    }

    override fun boatJobsChanged(boat: BoatModel) {
        super.boatJobsChanged(boat)
        if (boat ===  _selectedBoat?.model) {
            _scene.clearJobMarkers()
            _scene.plotJobMarkers(boat)
        }
    }

    fun boatControllerForId(idx: Long) : BoatController? {
        return _boatControllers.find { it.model.id == idx }
    }

    fun townControllerForId(idx: Long) : TownController? {
        return _townControllers.find { it.model.id == idx }
    }

    fun addBoat(boat: BoatModel): BoatController {
        val view = BoatView(boat.type)
        val boatController = BoatController(boat, view)
        _boatControllers.add(boatController)
        view.sprite.zOrder = 4
        this._scene.addChild(view.sprite)
        if (boatController.model.isMoored != true) {
            if (!boatController.sail()) {
                boatController.view.sprite.position.set(townControllerForModel(boatController.model.town!!).view.position)
            }
        } else {
            val tc = townControllerForModel(boat.town!!)
            tc.updateView()
            view.sprite.hidden = true
        }
        return boatController
    }

    fun townControllerForModel(model: TownModel): TownController {
        val townController = _townControllers.first { it.model === model }
        return townController
    }

    fun plotCourseForBoat(boat: BoatController) {
        val course = boat.model.course
        for (i in 1 until course.size) {
            _scene.plotRouteToTown(
                townControllerForModel(course[i - 1]),
                townControllerForModel(course[i]), "nav_plot.png"
            )
        }
        _scene.plotCourseForBoat(boat)
    }

    fun plotRoutesForTown(townCtrl: TownController, distance: Float) {

        townCtrl.state = TownController.State.selected
        val startPos = Map.instance.townPosition(townCtrl.model)
        val paths = mutableListOf<List<Edge<WorldNode>>>()
        for (controller in _townControllers) {
            if (_viewModel.boatCourse.contains(controller.model)) {
                continue
            }
            val endPos = Map.instance.townPosition(controller.model)
            val dist = startPos.distance(endPos)
            if (dist > distance * _density) {
                controller.state = TownController.State.blocked
            } else if (controller.model.level > 0) {
                controller.state = TownController.State.unselected
            }
            if (controller.state != TownController.State.unselected) {
                continue
            }
            paths.add(Map.instance.getRoute(townCtrl.model, controller.model))
        }
        _scene.plotRoutesForTown(townCtrl.model, paths, distance * _density)
    }

    fun townUpgraded() {
        if (mode == Mode.plot) {
            var town: TownController? = null
            if (_viewModel.boatCourse.size > 0) {
                town = townControllerForModel(_viewModel.boatCourse.popLast())

            } else {
                val townModel = _selectedBoat!!.model.town
                town = townControllerForModel(townModel!!)
            }
            town.state = TownController.State.unselected
            townSelected(town)
        }
    }

    override fun jobDelivered(boat: BoatModel, town: TownModel, gold: Int, silver: Int, quiet: Boolean) {
        if (quiet == false) {
            _scene.showMoney(town, gold, silver)
        }
    }

    override fun boatAdded(boat: BoatModel) {
        addBoat(boat)
    }

    override fun boatRemoved(boat: BoatModel) {
        val boatController = boatControllerForModel(boat)
        _boatControllers.removeAll { it === boatController }
        boatController.view.sprite.parent = null
        if (boat.town != null) {
            val townController = townControllerForModel(boat.town!!)
            townController.updateView()
        }
        if (_selectedBoat === boatController) {
            reset()
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden) {
        } else {
            if (_selectedBoat != null) {
                _scene.plotJobMarkers(_selectedBoat!!.model)
            }
        }
    }

    fun boatControllerForModel(model: BoatModel): BoatController {
        val boatController = _boatControllers.first { it.model === model }
        return boatController
    }


    fun focusTown(town: TownModel, animate: Boolean = true) {
        val scene_pos = Map.instance.townPosition(town)
        val screen_pos = _scrollView.screenPosition(scene_pos)
        if (animate) {
            _scrollView.smoothScrollTo(screen_pos.x, screen_pos.y)
        } else {
            _scrollView.scrollTo(screen_pos.x, screen_pos.y)
        }
    }

    fun boatSelected(boat: BoatController, animate: Boolean = true) {
        stopTracking()
        reset()
        _selectedBoat = boat
        val town = _selectedBoat!!.model.town
        if (town != null) {
            focusTown(town,animate)
            townSelected(townControllerForModel(town))
        } else {
            if (animate == true) {
                startTracking(this._selectedBoat!!)
            }
            plotCourseForBoat(this._selectedBoat!!)
            if (fragmentManager?.primaryNavigationFragment?.tag != "jobs") {
                _cargoButton.visibility = View.VISIBLE
            }
        }
    }

    fun boatSelected(index: Int) {
        val boat = _boatControllers[index]
        boatSelected(boat)
    }

    fun startTracking(boat: BoatController) {
        stopTracking()
        _viewModel.trackBoat = true
        val sprite = boat.view.sprite
        _scrollView.focusNode(sprite, true)
        val action = CanvasActionSequence(
            CanvasActionWait(250),
            CanvasActionRepeat(CanvasActionCutom(1000, _scrollView::boatTracker))
        )
        _selectedBoat!!.view.sprite.run(action, "track")
    }

    fun townSelected(town: TownController) {
        if (mode == Mode.plot && town.state == TownController.State.unselected) {
            _scene.clearPlot()
            val townCtrl: TownController?
            _viewModel.boatCourse.add(town.model)
            if (_viewModel.boatCourse.size == 1) {
                townCtrl = townControllerForModel(_selectedBoat?.model?.town!!)
                _cargoButton.visibility = View.VISIBLE
                _sailButton.visibility = View.INVISIBLE
                _cancelButton.visibility = View.VISIBLE
            } else {
                _cargoButton.visibility = View.INVISIBLE
                _sailButton.visibility = View.VISIBLE
                townCtrl = town
                for (i in 1 until _viewModel.boatCourse.size) {
                    _scene.plotRouteToTown(
                        townControllerForModel(_viewModel.boatCourse[i - 1]),
                        townControllerForModel(_viewModel.boatCourse[i]),
                        "nav_plotted.png"
                    )
                }
            }
            plotRoutesForTown(townCtrl, (this._selectedBoat!!.model.endurance).toFloat() * 0.5f)
            town.state = TownController.State.selected
        } else if (mode == Mode.build || mode == Mode.buy) {
            val buildType = _viewModel.buildType
            val buildParts = _viewModel.buildParts
            if (town.state == TownController.State.unselected && buildParts != null && buildType != null) {
                val boat = BoatModel(_viewModel.buildType!!, BoatModel.makeName(), town.model)
                if (mode == Mode.build) {
                    User.instance.purchaseBoatWithParts(boat, buildParts)
                } else {
                    User.instance.purchaseBoatWithMoney(boat, buildParts)
                }
                reset()
            }
        } else {
            reset()
            (activity as MapActivity).swapFragment(null, town)
        }
    }


    fun stopTracking() {
        if (_viewModel.trackBoat == true && this._selectedBoat != null) {
            this._selectedBoat!!.view.sprite.removeAction("track")
            _cargoButton.visibility = View.INVISIBLE
        }
        _viewModel.trackBoat = false
    }

    fun boatArrived(boat: BoatController) {
        val town = townControllerForModel(boat.model.town!!)
        town.updateView()
        if (_selectedBoat === boat) {
            boatSelected(boat, true)
        }
    }

    fun transferBoatBuild(fragment: androidx.fragment.app.Fragment) {
        if (fragment is BoatInfoFragment) {
            _viewModel.buildType = fragment.boatType
            _viewModel.buildParts = fragment.parts
        } else if (fragment is MarketFragment) {
            _viewModel.buildType = fragment.selectedPart?.boat
            _viewModel.buildParts = arrayListOf(fragment.selectedPart!!)
        }
    }

    fun buildBoat() {
        _toolTip.visibility = View.VISIBLE
        _cancelButton.visibility = View.VISIBLE
        val harborType =
            TownModel.HarbourSize.valueOf(
                BoatModel.boatConfig(
                    _viewModel.buildType!!,
                    BoatModel.BoatIndex.harbourType
                ) as String
            )
        for (town in _townControllers) {
            if (harborType < town.model.harbour) {
                town.state = TownController.State.blocked
            }
        }
    }


    fun sailButtonPressed() {
        _scene.clearPlot()
        val boat = _selectedBoat
        if (boat != null) {
            boat.model.plotCourse(_viewModel.boatCourse)
            val controller = townControllerForModel(boat.model.town!!)
            boat.sail()
            controller.updateView()
            Audio.instance.queueSound(R.raw.ship_bell)
            Game.instance.boatSailed(boat.model)
            reset()
            boatSelected(boat, false)
        }
    }


    fun cancelButtonPressed() {
        if (mode == Mode.plot) {
            if (_viewModel.boatCourse.size > 1) {
                townControllerForModel(_viewModel.boatCourse.popLast())
                val town = townControllerForModel(_viewModel.boatCourse.popLast())
                town.state = TownController.State.unselected
                townSelected(town)
            } else {
                val town = townControllerForModel(_viewModel.boatCourse.first())
                town.state = TownController.State.unselected
                reset()
                if (resources.getBoolean(R.bool.landscape) == true && fragmentManager?.primaryNavigationFragment?.tag == "jobs") {
                    fragmentManager?.popBackStack()
                }
            }
        } else if (mode == Mode.build || mode == Mode.buy) {
            toolTip.visibility = View.INVISIBLE
            _cancelButton.visibility = View.INVISIBLE
            val boatinfo = fragmentManager?.findFragmentByTag("boatinfo")
            if (boatinfo is BoatInfoFragment) {
                boatinfo.update()
            }
            _viewModel.buildParts = null
            _viewModel.buildType = null
            //tabBarController?.selectedIndex = 2
            //tabBar(enabled: true)
        }
    }

    fun reset() {
        _toolTip.visibility = View.INVISIBLE
        activity?.navigation?.visibility = View.VISIBLE
        for (town in _townControllers) {
            town.reset()
        }
        _cargoButton.visibility = View.INVISIBLE
        _cancelButton.visibility = View.INVISIBLE
        _sailButton.visibility = View.INVISIBLE
        _viewModel.buildType = null
        _selectedBoat = null
        _viewModel.boatCourse.clear()
        _viewModel.buildParts = null
        _scene.clearPlot()
        _scene.clearJobMarkers()
    }

    override fun onStart() {
        super.onStart()
        postTimers()
    }

    override fun onStop() {
        super.onStop()
        _timer.removeCallbacksAndMessages(null)
    }

    fun moveNotifyBox(down: Boolean, text: Int) {
        if (_animating == true && down == true) {
            return
        }

        val main = activity
        if (main == null) return

        val mapFrame = main.findViewById<ConstraintLayout>(R.id.mapcontainer)
        val notify = main.findViewById<ConstraintLayout>(R.id.event_notify)
        val message = main.findViewById<TextView>(text)

        message.visibility = View.VISIBLE
        notify.visibility = View.VISIBLE

        val constraint = ConstraintSet()
        constraint.clone(mapFrame)

        val transition = ChangeBounds()
        transition.duration = 1000

        val apt: TransitionListenerAdapter

        if (down) {
            apt = object : TransitionListenerAdapter() {
                override fun onTransitionEnd(transition: Transition) {
                    super.onTransitionEnd(transition)
                    moveNotifyBox(false, text)
                }
            }
            _animating = true
            constraint.clear(R.id.event_notify, ConstraintSet.BOTTOM)
            constraint.connect(R.id.event_notify, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
            transition.addListener(apt)
        } else {
            apt = object : TransitionListenerAdapter() {
                override fun onTransitionEnd(transition: Transition) {
                    super.onTransitionEnd(transition)
                    notify.visibility = View.INVISIBLE
                    message.visibility = View.INVISIBLE
                    _animating = false
                }
            }
            transition.startDelay = 3000
            constraint.clear(R.id.event_notify, ConstraintSet.TOP)
            constraint.connect(R.id.event_notify, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        }

        transition.addListener(apt)
        TransitionManager.beginDelayedTransition(mapFrame, transition)
        constraint.applyTo(mapFrame)

    }

    fun postTimers() {
        val uptime = SystemClock.uptimeMillis()
        _timer.postAtTime(_marketNotification, User.instance.millisToMarketDate + uptime)
        _timer.postAtTime(_jobNotification, User.instance.millisToJobDate + uptime)
    }

}