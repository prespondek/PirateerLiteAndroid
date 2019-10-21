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

import android.graphics.*
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.FragmentManager
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
import com.lanyard.pirateerlite.views.BoatView
import com.lanyard.pirateerlite.views.MapScrollView
import com.lanyard.pirateerlite.views.MapView
import com.lanyard.pirateerlite.views.TownView
import kotlinx.android.synthetic.main.activity_map.*
import kotlinx.android.synthetic.main.fragment_map.*

class MapFragment : AppFragment() , Game.GameListener, User.UserListener {

    enum class Mode {
        plot, map, track, nontrack, build, buy
    }

    //private var mapModel :          MapModel?
    private var _boatControllers = ArrayList<BoatController>()
    private var _townControllers = ArrayList<TownController>()
    private var _trackBoat = false
    private var _selectedBoat: BoatController? = null
    private var _boatCourse = ArrayList<TownController>()
    private var _buildType: String? = null
    private var _buildParts = ArrayList<User.BoatPart>()
    private var _selectedTint = Color.BLACK
    private var _density : Float = 0.0f
    private lateinit var _cargoButton: ImageButton
    private lateinit var _sailButton: ImageButton
    private lateinit var _cancelButton: ImageButton
    private lateinit var _scene: MapView
    private lateinit var _scrollView: MapScrollView
    lateinit var wallet: WalletFragment
    private lateinit var _toolTip: TextView
    private val _cargoClickListener = View.OnClickListener {
        if (fragmentManager?.primaryNavigationFragment?.tag != "jobs") {
            val frag = (activity as MapActivity).swapFragment(R.id.holdButton) as JobFragment
            frag.boatController = _selectedBoat!!
        }
    }

    private var _touchListener = View.OnTouchListener { v, event ->
        stopTracking()
        false
    }

    val mode: Mode
        get () {
            if (_selectedBoat != null) {
                if (_selectedBoat!!.model.town != null) {
                    return Mode.plot
                } else if (_trackBoat == false) {
                    return Mode.nontrack
                } else {
                    return Mode.track
                }
            } else if (_toolTip.visibility == VISIBLE) {
                if (_buildParts.size > 1) {
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
        if (savedInstanceState == null) {
            BitmapCache.instance.addBitmap(this.context!!, "gold_piece", R.drawable.gold_piece, Bitmap.Config.ARGB_4444)
            BitmapCache.instance.addBitmap(
                this.context!!,
                "silver_piece",
                R.drawable.silver_piece,
                Bitmap.Config.ARGB_4444
            )
        }
        // Inflate the layout for this fragment
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

        //_scrollView.scrollListener = this

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
                ConstraintSet.TOP, (vert.position.y + _scene.padding.height - button_width).toInt()
            )
            params.connect(
                button.id, ConstraintSet.LEFT, ConstraintSet.PARENT_ID,
                ConstraintSet.LEFT, (vert.position.x + _scene.padding.width - button_width).toInt()
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

        if (savedInstanceState == null) {
            _scrollView.target = _boatControllers[0].view.sprite.position
        } else {
            var screen_pos : Point
            savedInstanceState.getIntegerArrayList("scenePos").also {
                screen_pos = Point(it[0],it[1])
            }
            _scrollView.target = screen_pos
            if ( savedInstanceState.containsKey("selectedBoat") ) {
                val selectedBoat = _boatControllers.find { it.model.id == savedInstanceState.getLong("selectedBoat") }
                boatSelected(selectedBoat!!,false)
            }
            val boatCourse = savedInstanceState.getLongArray("boatCourse")
            if ( boatCourse != null && boatCourse.size > 0 ) {
                for (i in 1 until boatCourse.size) {
                    val town = _townControllers.find { it.model.id == boatCourse[i] }
                    townSelected(town!!)
                }
            }
            _buildType = savedInstanceState.getString("buildType")
            if (_buildType != null) {
                _buildParts = savedInstanceState.getParcelableArrayList<User.BoatPart>("buildParts")
                buildBoat()
            }
        }
        return view
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        var jobFrag = fragmentManager?.findFragmentByTag("job") as? JobFragment
        if (jobFrag != null) {
            jobFrag.boatController = _selectedBoat
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val scroll_pos = _scrollView.currentPosition()
        outState.putIntegerArrayList("scenePos", arrayListOf(scroll_pos.x, scroll_pos.y))
        var boat = _selectedBoat
        if (boat != null) {
            outState.putLong("selectedBoat", boat.model.id)
        }
        outState.putInt("mode",mode.ordinal)
        var path = arrayListOf<Long>()
        for (town in _boatCourse) {
            path.add(town.model.id)
        }
        outState.putLongArray("boatCourse",path.toLongArray())
        if(_buildType != null) {
            outState.putString("buildType",_buildType)
            outState.putParcelableArrayList("buildParts", _buildParts)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (this::_scene.isInitialized) {
            _scene.stopThread()
        }
    }

    override fun onResume() {
        super.onResume()
        if (this::_scene.isInitialized) {
            _scene.startThread()
        }
        if (mode == Mode.track) {
            startTracking(_selectedBoat!!)
        }
        if (mode == Mode.track || mode == Mode.nontrack) {
            _scene.clearPlot()
            plotCourseForBoat(_selectedBoat!!)
        }
    }

    override fun onPause() {
        super.onPause()
        if (this::_scene.isInitialized) {
            _scene.stopThread()
        }
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
            if (_boatCourse.contains(controller)) {
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
            if (_boatCourse.size > 0) {
                town = _boatCourse.popLast()
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
        _trackBoat = true
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
            _boatCourse.add(town)
            if (_boatCourse.size == 1) {
                townCtrl = townControllerForModel(_selectedBoat?.model?.town!!)
                if (fragmentManager?.primaryNavigationFragment?.tag != "jobs") {
                    _cargoButton.visibility = View.VISIBLE
                }
                _sailButton.visibility = View.INVISIBLE
                _cancelButton.visibility = View.VISIBLE
            } else {
                _cargoButton.visibility = View.INVISIBLE
                _sailButton.visibility = View.VISIBLE
                townCtrl = town
                for (i in 1 until _boatCourse.size) {
                    _scene.plotRouteToTown(_boatCourse[i - 1], _boatCourse[i], "nav_plotted.png")
                }
            }
            plotRoutesForTown(townCtrl, (this._selectedBoat!!.model.endurance).toFloat() * 0.5f)
            town.state = TownController.State.selected
        } else if (mode == Mode.build || mode == Mode.buy) {
            val boat = BoatModel(_buildType!!, BoatModel.makeName(), town.model)
            if (mode == Mode.build) {
                User.instance.purchaseBoatWithParts(boat, _buildParts)
            } else {
                User.instance.purchaseBoatWithMoney(boat, _buildParts)
            }
            reset()
        } else {
            reset()
            (activity as MapActivity).swapFragment(null, town)
        }
    }


    fun stopTracking() {
        if (_trackBoat == true && this._selectedBoat != null) {
            this._selectedBoat!!.view.sprite.removeAction("track")
            _cargoButton.visibility = View.INVISIBLE
        }
        _trackBoat = false
    }

    fun boatArrived(boat: BoatController) {
        val town = townControllerForModel(boat.model.town!!)
        town.updateView()
        if (_selectedBoat === boat) {
            boatSelected(boat, true)
        }
    }

    fun transferBoatBuild(fragment: androidx.fragment.app.Fragment) {
        reset()
        if (fragment is BoatInfoFragment) {
            _buildType = fragment.boatType
            _buildParts = fragment.parts
        } else if (fragment is MarketFragment) {
            _buildType = fragment.selectedPart?.boat
            _buildParts = arrayListOf(fragment.selectedPart!!)
        }
        buildBoat()
    }

    fun buildBoat() {
        _toolTip.visibility = View.VISIBLE
        //activity?.navigation?.visibility = View.GONE
        _cancelButton.visibility = View.VISIBLE
        val harborType =
            TownModel.HarbourSize.valueOf(BoatModel.boatConfig(_buildType!!, BoatModel.BoatIndex.harbourType) as String)
        for (town in _townControllers) {
            if (harborType < town.model.harbour) {
                town.state = TownController.State.blocked
            }
        }
    }


    fun sailButtonPressed() {
        _scene.clearPlot()
        var boat = _selectedBoat
        if (boat != null) {
            boat.model.plotCourse(_boatCourse.map { it.model })
            val controller = townControllerForModel(boat.model.town!!)
            boat.sail()
            controller.updateView()
            Audio.instance.queueSound(R.raw.ship_bell)
            Game.instance.boatSailed(boat.model)
            /*UNUserNotificationCenter.current().getPendingNotificationRequests { (notifications:[UNNotificationRequest]) in
                var pendingnotify = false
                let date = Date(timeIntervalSince1970:boat.model.arrivalTime)
                for notify in notifications {
                    if notify.identifier == "BoatArrival" {
                        let old_trigger = notify.trigger as! UNCalendarNotificationTrigger
                        if old_trigger.nextTriggerDate()! > date {
                        pendingnotify = true
                        break
                    }
                    }
                }
                if pendingnotify == false {
                    let content = UNMutableNotificationContent()
                    content.title = "Voyage complete"
                    content.body = String(format: "All boats are moored at their destination.")
                    content.sound = UNNotificationSound(named: UNNotificationSoundName("ship_bell"))
                    let trigger = UNCalendarNotificationTrigger(dateMatching: Calendar.current.dateComponents([.year,.month,.day,.hour,.minute,.second,], from: date ),
                    repeats: false)
                    UNUserNotificationCenter.current().add(UNNotificationRequest(identifier: "BoatArrival", content: content, trigger: trigger), withCompletionHandler: nil)
                }
            }*/
            reset()
            boatSelected(boat, false)
        }
    }


    fun cancelButtonPressed() {
        if (mode == Mode.plot) {
            if (_boatCourse.size > 1) {
                var town = _boatCourse.popLast()
                town.state = TownController.State.unselected
                town = _boatCourse.popLast()
                town.state = TownController.State.unselected
                townSelected(town)
            } else {
                val town = _boatCourse.first()
                town.state = TownController.State.unselected
                reset()
                if (resources.getBoolean(R.bool.landscape) == true && fragmentManager?.findFragmentByTag("jobs") != null) {
                    fragmentManager?.popBackStack()
                }
            }
        } else if (mode == Mode.build || mode == Mode.buy) {
            toolTip.visibility = View.INVISIBLE
            _cancelButton.visibility = View.INVISIBLE
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
        _selectedBoat = null
        _boatCourse.clear()
        _buildParts.clear()
        _scene.clearPlot()
        _scene.clearJobMarkers()
    }

}