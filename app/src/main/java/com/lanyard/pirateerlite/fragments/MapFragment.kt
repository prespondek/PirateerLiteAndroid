package com.lanyard.pirateerlite.fragments

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Typeface
import android.media.MediaPlayer
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.constraint.ConstraintSet
import android.support.v4.app.Fragment
import android.util.DisplayMetrics
import android.util.SizeF
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.lanyard.canvas.*
import com.lanyard.helpers.*
import com.lanyard.library.Edge
import com.lanyard.pirateerlite.MapActivity
import com.lanyard.pirateerlite.controllers.BoatController
import com.lanyard.pirateerlite.controllers.TownController
import com.lanyard.pirateerlite.models.BoatModel
import com.lanyard.pirateerlite.models.TownModel
import com.lanyard.pirateerlite.models.WorldNode
import com.lanyard.pirateerlite.singletons.User
import com.lanyard.pirateerlite.singletons.Map
import com.lanyard.pirateerlite.views.*
import com.lanyard.pirateeronline.R
import kotlinx.android.synthetic.main.fragment_map.*

class MapFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        var view = inflater.inflate(R.layout.fragment_map, container, false)
        _scrollView = view.findViewById<MapScrollView>(R.id.vscrollview)
        _scene = view.findViewById<MapView>(R.id.mapView)

        _sailButton = view.findViewById(R.id.plotButton) as ImageButton
        _sailButton?.setOnClickListener { this.sailButtonPressed() }
        _cancelButton = view.findViewById(R.id.cancelButton)
        _cancelButton?.setOnClickListener{ this.cancelButtonPressed() }
        _cargoButton = view.findViewById(R.id.holdButton)
        _cargoButton?.setOnClickListener(View.OnClickListener { (activity as MapActivity).swapFragment(R.id.holdButton) })
        _toolTip = view.findViewById(R.id.toolTip) as TextView
        var mapframe = view.findViewById(R.id.mapframe) as FrameLayout
        var buttonlayout = view.findViewById(R.id.buttonlayout) as ConstraintLayout

        var metrics = DisplayMetrics()
        activity?.windowManager?.defaultDisplay?.getMetrics(metrics)
        val logicalDensity = metrics.density

        mapframe.layoutParams.width = _scene.scene!!.size.width
        mapframe.layoutParams.height = _scene.scene!!.size.height

        mapframe.minimumWidth = _scene.scene!!.size.width
        mapframe.minimumHeight = _scene.scene!!.size.height

        _scrollView.getViewTreeObserver().addOnScrollChangedListener(_scrollView)
        _scrollView.scrollListener = _scene

        var mp = MediaPlayer.create(activity,R.raw.bg_map)
        mp.isLooping = true
        mp.start()

        TownView.setup()
        TownController.setController(this)
        for (vert in Map.sharedInstance.graph.vertices) {
            val town = vert.data as? TownModel
            if (town == null) { continue }

            val button = Button(activity)
            button.id = View.generateViewId()
            buttonlayout.addView(button)

            button.layoutParams.width = (64 * logicalDensity).toInt()
            button.layoutParams.height = (64 * logicalDensity).toInt()
            button.background = null

            var params = ConstraintSet()
            params.clone(buttonlayout)
            val button_width = (32 * logicalDensity).toInt()
            params.connect(button.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID,
                ConstraintSet.TOP, (vert.position.y + _scene.padding.height - button_width).toInt());
            params.connect(button.getId(), ConstraintSet.LEFT, ConstraintSet.PARENT_ID,
                ConstraintSet.LEFT, (vert.position.x + _scene.padding.width - button_width).toInt());
            params.constrainWidth(button.getId(), button_width * 2)
            params.constrainHeight(button.getId(), button_width * 2)
            params.applyTo(buttonlayout)

            val sprite = TownView()
            sprite.scale = SizeF(0.5f,0.5f)
            sprite.position = Point(vert.position.x , vert.position.y)
            sprite.zOrder = 2
            _scene.root.addChild(sprite)

            val label = CanvasLabel(town.name, null)
            label.position = Point(vert.position.x, vert.position.y - 48)
            label.fontSize = 48.0f
            label.fontStyle = Paint.Style.FILL_AND_STROKE
            label.strokeWidth = 6.0f
            label.zOrder = 2
            label.typeface = Typeface.DEFAULT_BOLD
            _scene.root.addChild(label)

            val townCtrl = TownController(town!!, button, sprite)
            _townControllers.add(townCtrl)
            townCtrl.state = TownController.State.unselected
        }

        /*this._selectedTint = tabBarController!.tabBar.tintColor
        tabBarController!.tabBar.unselectedItemTintColor = UIColor.lightGray*/

        BoatController.setController(this)
        for (boatModel in User.sharedInstance.boats) {
            addBoat( boatModel )
        }

        return view
    }


    enum class Mode {
        plot, map, track, nontrack, build, buy
    }

companion object {
    private var _map : MapFragment? = null

    val instance : MapFragment
        get() {
            return _map!!
        }

}
    //private var mapModel :          MapModel?
    private var _boatControllers =      ArrayList<BoatController>()
    private var _townControllers =      ArrayList<TownController>()
    private var _trackBoat =            false
    private var _selectedBoat :         BoatController? = null
    private var _boatCourse =           ArrayList<TownController>()
    private var _buildType :            String? = null
    private var _buildParts =           ArrayList<User.BoatPart>()
    private var _selectedTint =         Color.BLACK
    private lateinit var _cargoButton:  ImageButton
    private lateinit var _sailButton:   ImageButton
    private lateinit var _cancelButton: ImageButton
    private lateinit var _scene:        MapView
    private lateinit var _scrollView:   MapScrollView
    lateinit var wallet:                WalletFragment
    private lateinit var _toolTip:      TextView

    val mode : Mode
        get (){
            if (_selectedBoat != null) {
                if (_selectedBoat!!.model.town != null) {
                return Mode.plot
            } else if (_trackBoat == false) {
                return Mode.nontrack
            } else {
                return Mode.track
            }
            } else if (_toolTip?.isShown == true) {
                if (_buildParts.size > 1) {
                    return Mode.build
                } else {
                    return Mode.buy
                }
            } else {
                return Mode.map
            }
        }

    fun addBoat ( boat: BoatModel ) : BoatController
    {
        val view = BoatView( boat.type )
        val boatController = BoatController( boat, view )
        _boatControllers.add( boatController )
        view.sprite.zOrder = 4
        this._scene.root.addChild( view.sprite )
        if (boatController.model.isMoored != true) {
            boatController.sail()
        } else {
            val tc = townControllerForModel(boat.town!!)
            tc.updateView()
            view.sprite.hidden = true
        }
        return boatController
    }

    fun townControllerForModel (model: TownModel) : TownController
    {
        val townController = _townControllers.first { it.model === model }
        return townController!!
    }

    fun plotCourseForBoat ( boat: BoatController ) {
        val course = boat.model.course
                for (i in 1..course.size-1) {
                    _scene.plotRouteToTown( townControllerForModel(course[i-1]),
                    townControllerForModel(course[i]),"nav_plot")
                }
        _scene.plotCourseForBoat(boat)
    }

    fun plotRoutesForTown ( townCtrl: TownController, distance: Float )
    {
        townCtrl.state = TownController.State.selected
        val startPos = Map.sharedInstance.townPosition( townCtrl.model )
        var paths = mutableListOf<List<Edge<WorldNode>>>()
        for (controller in _townControllers) {
            if (_boatCourse.contains(controller)) { continue }
            val endPos = Map.sharedInstance.townPosition( controller.model )
            val dist = startPos.distance( endPos )
            if (dist > distance) {
                controller.state = TownController.State.blocked
            }
            else if (controller.model.level > 0) {
                controller.state = TownController.State.unselected
            }
            if (controller.state != TownController.State.unselected) { continue }
            paths.add(Map.sharedInstance.getRoute(townCtrl.model, controller.model))
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

    /*fun jobDelivered(notification: Notification) {
        val town = notification.userInfo!["Town"] as! TownModel
        val gold = notification.userInfo!["Gold"] as! Int
        val silver = notification.userInfo!["Silver"] as! Int
        val quiet = notification.userInfo!["Quiet"] as! Bool
        if (quiet == false) {
            _scene.showMoney(town, gold, silver)
        }
    }*/

    fun boatAdded      (boat: BoatModel) {
        addBoat(boat)
    }

    fun boatRemoved    (boat: BoatModel) {
        val boatController = boatControllerForModel(boat)
        _boatControllers.removeAll {it === boatController}
        boatController.view.sprite.parent = null
        if (boat.town != null) {
            val townController = townControllerForModel(boat.town!!)
            townController.updateView()
        }
        if (_selectedBoat!! === boatController) {
            reset()
        }
    }

    internal fun goldUpdated(oldValue: Int, newValue: Int) {
        wallet.goldUpdated(oldValue,newValue)
    }

    internal fun silverUpdated(oldValue: Int, newValue: Int) {
        wallet.silverUpdated(oldValue,newValue)
    }

    /*override fun viewWillAppear(animated: Boolean)
    {
        if let boat = _selectedBoat {
            scene.plotJobMarkers(boat.model)
        }
        super.viewWillAppear(animated)
        navigationController?.setNavigationBarHidden(true, animated: animated)
        if mode == .build {
            tabBar(enabled: false)
        }

    }

    override fun viewWillDisappear(animated: Boolean)
    {
        super.viewWillDisappear(animated)
        navigationController?.setNavigationBarHidden(false, animated: animated)
    }*/

    fun boatControllerForModel (model: BoatModel) : BoatController
    {
        val boatController = _boatControllers.first {it.model === model}
        return boatController!!
    }

    fun boatSelected ( boat: BoatController)
    {
        stopTracking()
        reset()
        _selectedBoat = boat
        val town = _selectedBoat!!.model.town
                if (town != null) {
                    val scene_pos = Map.sharedInstance.townPosition( town!! )
                    val screen_pos = screenPosition( scene_pos )
                    //_scrollView.scrollY = screen_pos.y
                    //_scrollView.scrollX = screen_pos.x
                    _scrollView.scrollTo(screen_pos.x, screen_pos.y)
                    townSelected( townControllerForModel( town!! ) )
                } else {
                    startTracking( this._selectedBoat!! )
                    plotCourseForBoat ( this._selectedBoat!! )
                    _cargoButton.visibility = View.VISIBLE
                }
    }

    fun boatSelected ( index: Int )
    {
        val boat = _boatControllers[index]
        boatSelected(boat)
    }

    fun startTracking ( boat: BoatController )
    {
        stopTracking()
        _trackBoat = true
        val action = CanvasActionRepeat(CanvasActionCutom(1000, { node,dt->
            val pos = node.position
            val screen_pos = this.screenPosition(pos)
            this._scrollView.scrollTo(screen_pos.x,screen_pos.y)
        }))
        _selectedBoat!!.view.sprite.run(action, "track")
    }

    fun townSelected ( town: TownController ) {
        if ( mode == Mode.plot && town.state == TownController.State.unselected ) {
            _scene.clearPlot()
            var townCtrl : TownController?
            _boatCourse.add(town)
            if (_boatCourse.size == 1) {
                townCtrl = townControllerForModel( _selectedBoat?.model?.town!! )
                _cargoButton?.visibility = View.VISIBLE
                _sailButton?.visibility = View.INVISIBLE
                cancelButton.visibility = View.VISIBLE
            } else {
                _cargoButton?.visibility = View.INVISIBLE
                _sailButton?.visibility = View.VISIBLE
                townCtrl = town
                for (i in 1.._boatCourse.size - 1) {
                    _scene.plotRouteToTown( _boatCourse[i-1], _boatCourse[i], "nav_plotted.png" )
                }
            }
            plotRoutesForTown( townCtrl!! ,(this._selectedBoat!!.model.endurance).toFloat())
            town.state = TownController.State.selected
        } else if (mode == Mode.build || mode == Mode.buy) {
            val boat = BoatModel(_buildType!!, BoatModel.makeName(), town.model)
            if (mode == Mode.build) {
                User.sharedInstance.purchaseBoatWithParts(boat, _buildParts)
            } else {
                User.sharedInstance.purchaseBoatWithMoney(boat, _buildParts)
            }
            User.sharedInstance.save()
            //tabBar(enabled: true)
            reset()
        } else {
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

    fun stopTracking ( )
    {
        if (_trackBoat == true && this._selectedBoat != null) {
            this._selectedBoat!!.view.sprite.removeAction("track")
            _cargoButton?.visibility = View.INVISIBLE
        }
        _trackBoat = false
    }

    fun screenPosition ( position: Point ) : Point
    {
        var new_position = Point(position)
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


    fun boatArrived( boat: BoatController )
    {
        boat.view.sprite.hidden = true
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

    fun unwindToMap(fragment: Fragment) {
        reset()
        toolTip.visibility = View.VISIBLE
        cancelButton.visibility = View.VISIBLE
        if (fragment is BoatListFragment) {
            _buildType = source.boatType
            _buildParts = source.parts
        } else if let source = segue.source as? MarketViewController {
            _buildType = source.selectedPart?.boat
            _buildParts = [source.selectedPart!]
        }
        val harborType = TownModel.HarbourSize(BoatModel.boatData(_buildType!, BoatModel.BoatIndex.harbourType) as! String)
        for (town in _townControllers) {
            if (harborType! > town.model.harbour) {
            town.state = .blocked
        }
        }
    }*/


    fun sailButtonPressed() {
        _scene.clearPlot()
        if (_selectedBoat != null) {
            _selectedBoat!!.model.plotCourse(_boatCourse.map { it.model })
            val controller = townControllerForModel(_selectedBoat!!.model.town!!)
            _selectedBoat!!.sail()
            controller.updateView()
            var mp = MediaPlayer.create(activity,R.raw.ship_bell)
            mp.start()

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
            User.sharedInstance.save()
            reset()
        }
    }


    fun cancelButtonPressed() {
        if (mode == Mode.plot) {
            if (_boatCourse.size > 1) {
                var town = _boatCourse.popLast()
                town?.state = TownController.State.unselected
                        town = _boatCourse.popLast()
                town?.state = TownController.State.unselected
                        townSelected( town!! )
            }  else {
                val town = _boatCourse.first()
                town?.state = TownController.State.unselected
                reset()
            }
        } else if (mode == Mode.build ||  mode == Mode.buy) {
            toolTip.visibility = View.INVISIBLE
            _cancelButton.visibility = View.INVISIBLE
            //tabBarController?.selectedIndex = 2
            //tabBar(enabled: true)
        }
    }
    fun applicationWillEnterBackground () {

    }

    fun applicationWillEnterForeground () {
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
        for (town in _townControllers) {
            town.reset()
        }
        _cargoButton?.visibility = View.INVISIBLE
        _cancelButton?.visibility = View.INVISIBLE
        _sailButton?.visibility = View.INVISIBLE
        _selectedBoat = null
        _boatCourse.clear()
        _buildParts.clear()
        _scene.clearPlot()
        _scene.clearJobMarkers()
    }
}