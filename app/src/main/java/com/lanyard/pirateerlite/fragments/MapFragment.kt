package com.lanyard.pirateerlite.fragments

import android.content.Intent
import android.graphics.*
import android.os.Bundle
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.*
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.lanyard.canvas.*
import com.lanyard.helpers.*
import com.lanyard.library.Edge
import com.lanyard.library.SuperScrollView
import com.lanyard.pirateerlite.MapActivity
import com.lanyard.pirateerlite.controllers.BoatController
import com.lanyard.pirateerlite.controllers.TownController
import com.lanyard.pirateerlite.models.BoatModel
import com.lanyard.pirateerlite.models.TownModel
import com.lanyard.pirateerlite.models.WorldNode
import com.lanyard.pirateerlite.singletons.User
import com.lanyard.pirateerlite.singletons.Map
import com.lanyard.pirateerlite.views.*
import com.lanyard.pirateerlite.R
import com.lanyard.pirateerlite.singletons.Audio
import com.lanyard.pirateerlite.singletons.Game
import kotlinx.android.synthetic.main.activity_map.*
import kotlinx.android.synthetic.main.fragment_map.*
import kotlinx.coroutines.runBlocking

class MapFragment : androidx.fragment.app.Fragment(), Game.GameListener, User.UserObserver {

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
    private lateinit var _cargoButton: ImageButton
    private lateinit var _sailButton: ImageButton
    private lateinit var _cancelButton: ImageButton
    private lateinit var _scene: MapView
    private lateinit var _scrollView: SuperScrollView
    lateinit var wallet: WalletFragment
    private lateinit var _toolTip: TextView

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
            } else if (_toolTip.isShown == true) {
                if (_buildParts.size > 1) {
                    return Mode.build
                } else {
                    return Mode.buy
                }
            } else {
                return Mode.map
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
        User.instance.addObserver(this)
        val view = inflater.inflate(R.layout.fragment_map, container, false)
        _scrollView = view.findViewById<SuperScrollView>(R.id.vscrollview)
        _scene = view.findViewById<MapView>(R.id.mapView)

        _sailButton = view.findViewById(R.id.plotButton) as ImageButton
        _sailButton.setOnClickListener { this.sailButtonPressed() }
        _cancelButton = view.findViewById(R.id.cancelButton)
        _cancelButton.setOnClickListener { this.cancelButtonPressed() }
        _cargoButton = view.findViewById(R.id.holdButton)
        _cargoButton.setOnClickListener(View.OnClickListener {
            val frag = (activity as MapActivity).swapFragment(R.id.holdButton) as JobFragment
            frag.boatController = _selectedBoat!!
        })
        _toolTip = view.findViewById(R.id.toolTip) as TextView
        val mapframe = view.findViewById(R.id.mapframe) as FrameLayout
        val buttonlayout = view.findViewById(R.id.buttonlayout) as ConstraintLayout

        val metrics = DisplayMetrics()
        activity?.windowManager?.defaultDisplay?.getMetrics(metrics)
        val logicalDensity = metrics.density

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
                _scene.root.position = -Point(scrollX, scrollY) + _scene.padding
            }
        })
        _scrollView.setOnTouchListener { v, event ->
            stopTracking()
            false
        }

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

            button.layoutParams.width = (64 * logicalDensity).toInt()
            button.layoutParams.height = (64 * logicalDensity).toInt()
            button.background = null

            val params = ConstraintSet()
            params.clone(buttonlayout)

            val button_width = (32 * logicalDensity).toInt()
            params.connect(
                button.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID,
                ConstraintSet.TOP, (vert.position.y + _scene.padding.height - button_width).toInt()
            )
            params.connect(
                button.getId(), ConstraintSet.LEFT, ConstraintSet.PARENT_ID,
                ConstraintSet.LEFT, (vert.position.x + _scene.padding.width - button_width).toInt()
            )
            params.constrainWidth(button.getId(), button_width * 2)
            params.constrainHeight(button.getId(), button_width * 2)
            params.applyTo(buttonlayout)

            val sprite = TownView()
            sprite.position = Point(vert.position.x, vert.position.y)
            sprite.zOrder = 2
            _scene.root.addChild(sprite)

            val label = CanvasLabel(town.name, null)
            label.position = Point(
                vert.position.x,
                vert.position.y - context!!.resources.getDimensionPixelSize(R.dimen.town_label_offset)
            )
            label.fontSize = context!!.resources.getDimensionPixelSize(R.dimen.town_label_size).toFloat()
            label.fontStyle = Paint.Style.FILL_AND_STROKE
            label.strokeWidth = context!!.resources.getDimensionPixelSize(R.dimen.town_label_stroke).toFloat()
            label.zOrder = 2
            label.typeface = Typeface.DEFAULT_BOLD
            _scene.root.addChild(label)

            val townCtrl = TownController(town, button, sprite)
            _townControllers.add(townCtrl)
            townCtrl.state = TownController.State.unselected
        }

        if (savedInstanceState == null) {
            _scrollView.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    var factor = context!!.getResources().displayMetrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT
                    var offset = Point((1600 * factor).toInt(), (200 * factor).toInt())
                    _scrollView.scrollTo(offset.x, offset.y)
                    _scene.root.position = -Point(offset.x, offset.y) + _scene.padding
                    _scrollView.viewTreeObserver.removeOnPreDrawListener(this)
                    return true
                }
            })
        }

        BoatController.setController(this)
        for (boatModel in User.instance.boats) {
            addBoat(boatModel)
        }
        return view
    }

    override fun onDestroy() {
        super.onDestroy()
        _scene.canvasThread?.setRunning(false)
        _scene.canvasThread?.join()
    }

    override fun onResume() {
        super.onResume()
        when (mode) {
            Mode.track -> startTracking(_selectedBoat!!)
            Mode.nontrack -> {
                startTracking(_selectedBoat!!)
                _scene.clearPlot()
                plotCourseForBoat(_selectedBoat!!)
            }
            else -> return
        }
    }

    fun addBoat(boat: BoatModel): BoatController {
        val view = BoatView(boat.type)
        val boatController = BoatController(boat, view)
        _boatControllers.add(boatController)
        view.sprite.zOrder = 4
        this._scene.root.addChild(view.sprite)
        if (boatController.model.isMoored != true) {
            boatController.sail()
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
            if (dist > distance) {
                controller.state = TownController.State.blocked
            } else if (controller.model.level > 0) {
                controller.state = TownController.State.unselected
            }
            if (controller.state != TownController.State.unselected) {
                continue
            }
            paths.add(Map.instance.getRoute(townCtrl.model, controller.model))
        }
        _scene.plotRoutesForTown(townCtrl.model, paths, distance)
    }

    /*fun townUpgraded(notification: Notification) {
        if (mode == Mode.plot) {
            var town = _boatCourse.popLast()
            if (town == null) {
                val townModel = _selectedBoat!!.model.town
                town = townControllerForModel(townModel!!)
            }
            town!!.state = TownController.State.unselected
            townSelected(town!!)
        }
    }*/

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

    fun boatSelected(boat: BoatController) {
        stopTracking()
        reset()
        _selectedBoat = boat
        val town = _selectedBoat!!.model.town
        if (town != null) {
            val scene_pos = Map.instance.townPosition(town)
            val screen_pos = screenPosition(scene_pos)
            _scrollView.scrollTo(screen_pos.x, screen_pos.y)
            _scene.root.position = -Point(screen_pos.x, screen_pos.y) + _scene.padding
            townSelected(townControllerForModel(town))
        } else {
            startTracking(this._selectedBoat!!)
            plotCourseForBoat(this._selectedBoat!!)
            _cargoButton.visibility = View.VISIBLE
        }
    }

    fun boatSelected(index: Int) {
        val boat = _boatControllers[index]
        boatSelected(boat)
    }

    fun startTracking(boat: BoatController) {
        stopTracking()
        _trackBoat = true
        val action = CanvasActionRepeat(CanvasActionCutom(1000, { node, dt ->
            val pos = node.position
            val screen_pos = this.screenPosition(pos)
            this._scrollView.scrollTo(screen_pos.x, screen_pos.y)
        }))
        _selectedBoat!!.view.sprite.run(action, "track")
    }

    fun townSelected(town: TownController) {
        if (mode == Mode.plot && town.state == TownController.State.unselected) {
            _scene.clearPlot()
            val townCtrl: TownController?
            _boatCourse.add(town)
            if (_boatCourse.size == 1) {
                townCtrl = townControllerForModel(_selectedBoat?.model?.town!!)
                _cargoButton.visibility = View.VISIBLE
                _sailButton.visibility = View.INVISIBLE
                cancelButton.visibility = View.VISIBLE
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
            //User.instance.save()
            //tabBar(enabled: true)
            reset()
        } else {
            (activity as MapActivity).swapFragment(null, town)
            //performSegue( withIdentifier: "TownSegue", sender: town )
        }
    }

    /*fun tabBar (enabled: Boolean) {
        if enabled {
            tabBarController?.tabBar.isUserInteractionEnabled = true
            tabBarController?.tabBar.tintColor = _selectedTint
            tabBarController?.tabBar.unselectedItemTintColor = UIColor.lightGray
        } else {
            tabBarController?.tabBar.isUserInteractionEnabled = false
            tabBarController?.tabBar.tintColor = UIColor(red: 0.9, green: 0.9, blue: 0.9, alpha: 1.0)
            tabBarController?.tabBar.unselectedItemTintColor = UIColor(red: 0.9, green: 0.9, blue: 0.9, alpha: 1.0)
        }
    }*/

    fun stopTracking() {
        if (_trackBoat == true && this._selectedBoat != null) {
            this._selectedBoat!!.view.sprite.removeAction("track")
            _cargoButton.visibility = View.INVISIBLE
        }
        _trackBoat = false
    }

    fun screenPosition(position: Point): Point {
        val new_position = Point(position)
        val pad = this._scene.padding
        //new_position.y = -new_position.y
        new_position.x += pad.width
        new_position.y += pad.height
        new_position.x -= _scene.width / 2
        new_position.y -= _scene.height / 2
        return new_position
    }

    /*required init?(coder aDecoder: NSCoder)
    {
        super.init(coder: aDecoder)
    }*/


    fun boatArrived(boat: BoatController) {
        val town = townControllerForModel(boat.model.town!!)
        town.updateView()
    }

    /*override func prepare(for segue: UIStoryboardSegue, sender: Any?)
    {
        if let vc = segue.destination as? TownViewController
                {
                    let town = sender as! TownController
                    vc.townController = town
                } else if let vc = segue.destination as? JobViewController {
            vc.boatController = _selectedBoat
        }
    }*/


    /*extension MapViewController : UIScrollViewDelegate
    {

        func scrollViewDidScroll(_ scrollView: UIScrollView) {
            scene.scrollViewDidScroll(scrollView)
        }

        func scrollViewWillBeginDragging(_ scrollView: UIScrollView) {
            stopTracking()
        }
    }
        fun unwindBoatSold(segue:UIStoryboardSegue) {
            reset()
        }
    */
    fun buildBoat(fragment: androidx.fragment.app.Fragment) {
        reset()
        toolTip.visibility = View.VISIBLE
        activity?.navigation?.visibility = View.GONE
        //cancelButton.visibility = View.VISIBLE
        if (fragment is BoatInfoFragment) {
            _buildType = fragment.boatType
            _buildParts = fragment.parts
        } else if (fragment is MarketFragment) {
            _buildType = fragment.selectedPart?.boat
            _buildParts = arrayListOf(fragment.selectedPart!!)
        }
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
        if (_selectedBoat != null) {
            _selectedBoat!!.model.plotCourse(_boatCourse.map { it.model })
            val controller = townControllerForModel(_selectedBoat!!.model.town!!)
            _selectedBoat!!.sail()
            controller.updateView()
            Audio.instance.queueSound(R.raw.ship_bell)

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
            }
        } else if (mode == Mode.build || mode == Mode.buy) {
            toolTip.visibility = View.INVISIBLE
            _cancelButton.visibility = View.INVISIBLE
            //tabBarController?.selectedIndex = 2
            //tabBar(enabled: true)
        }
    }

    fun applicationWillEnterBackground() {

    }

    fun applicationWillEnterForeground() {
        if (mode == Mode.track) {
            startTracking(_selectedBoat!!)
        }
        if (mode == Mode.track || mode == Mode.nontrack) {
            _scene.clearPlot()
            plotCourseForBoat(_selectedBoat!!)
        }
    }

    fun reset() {
        toolTip.visibility = View.INVISIBLE
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