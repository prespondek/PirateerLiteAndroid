package com.lanyard.pirateerlite.views

import android.content.Context;
import android.graphics.*
import android.util.AttributeSet;
import android.view.MotionEvent
import android.util.DisplayMetrics
import android.util.Size
import android.util.SizeF
import android.widget.ScrollView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.lanyard.canvas.*
import com.lanyard.helpers.plus
import com.lanyard.helpers.unaryMinus
import com.lanyard.library.*
import com.lanyard.pirateerlite.controllers.BoatController
import com.lanyard.pirateerlite.controllers.TownController
import com.lanyard.pirateerlite.models.BoatModel
import com.lanyard.pirateerlite.models.TownModel
import com.lanyard.pirateerlite.models.WorldNode
import java.io.InputStreamReader
import com.lanyard.pirateerlite.singletons.Map
import kotlin.math.max


class MapView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    CanvasView(context, attrs, defStyleAttr),
    MapScrollViewListener {
    var pos = Point(0, 0)
    var root = CanvasNode()
    var padding = Size(0, 0)
    var density = 0.0f
    private var _plotNode: CanvasNode
    private var _jobNode: CanvasNode
    private var _messageNode: CanvasNode
    private var _selectionCircle: CanvasNode?

    init {
        this.scene = CanvasScene()
        this.scene?.addChild(root)
        this._plotNode = CanvasNode()
        this._jobNode = CanvasNode()
        this._messageNode = CanvasNode()
        this._selectionCircle = null
        density = DisplayMetrics.DENSITY_DEVICE_STABLE.toFloat() / DisplayMetrics.DENSITY_DEFAULT
        val gson = Gson()
        val reader = JsonReader(InputStreamReader(context.getAssets().open("map_view.json")))
        val json = gson.fromJson<HashMap<String, Any>>(reader, object : TypeToken<HashMap<String, Any>>() {}.type)
        setup(json)
    }

    fun clearPlot() {
        _plotNode.removeAllChildren()
    }

    fun clearJobMarkers() {
        _jobNode.removeAllChildren()
    }

    fun makeBackground(data: ArrayList<String>, files: ArrayList<String>) {
        //var tile_index = json.getJSONArray("TileIndex")
        //var tile_file = json.getJSONArray("TileFile")
        for (y in 1..data.size) {
            val str = data[y - 1].toString()
            for (x in 1..str.length) {
                var index = str[x - 1].toInt() - 48
                var file = files[index] as String
                val tile = BitmapCache.instance.addBitmap(file, Bitmap.Config.RGB_565)!!
                var sprite = CanvasSprite(tile)
                sprite.anchor.set(0.0f, 1.0f)
                sprite.position.set(tile.width * (x - 1), tile.height * (y - 1))
                this.root.addChild(sprite)
            }
        }
    }

    private fun setup(data: HashMap<String, Any>) {
        setPadding(data["Padding"] as ArrayList<Int>)
        setDimensions(data["Dimensions"] as ArrayList<Int>)
        makeBackground(
            data["TileIndex"] as ArrayList<String>,
            data["TileFile"] as ArrayList<String>
        )
        makeAnimations(data)
        makeFlags(data)
        root.addChild(_plotNode)
        root.addChild(_jobNode)
    }

    fun setPadding(data: ArrayList<Int>) {
        padding = Size(data[0], -(data[1]))
        root.position = Point(padding.width, padding.height)
    }

    fun setDimensions(data: ArrayList<Int>) {
        scene?.size = Size(
            ((data[0] + padding.width * 2) * density).toInt(),
            ((data[1] + padding.height * 2) * density).toInt()
        )
    }


    fun makeAnimations(data: HashMap<String, Any>) {

        var anim = data["Animations"] as ArrayList<ArrayList<Any>>
        for (atom in anim) {
            val name = atom[0] as String
            val framenames = data[name] as ArrayList<String>
            var bimpl = ArrayList<BitmapStream>()
            for (frame in framenames) {
                bimpl.add(BitmapCache.instance.addBitmap(frame + ".png", Bitmap.Config.ARGB_4444)!!)
            }
            val sprite = CanvasSprite(bimpl[0])
            val action = CanvasActionAnimate(bimpl, (1000 * atom[2] as Double).toInt())

            // only negative size seems to work, negative scale arent even visible
            sprite.scale = SizeF((atom[5] as Double).toFloat(), (atom[6] as Double).toFloat())
            sprite.position = Point(
                (atom[3] as Double * density).toInt(),
                -(atom[4] as Double * density).toInt()
            )
            sprite.run(action)
            root.addChild(sprite)
        }
    }

    fun makeFlags(data: HashMap<String, Any>) {
        val flags = data["Flags"] as ArrayList<ArrayList<Double>>
        var framenames = data["flags"] as ArrayList<String>

        for (faction in TownModel.Allegiance.values) {
            if (faction == TownModel.Allegiance.none) {
                continue
            }
            for (file in framenames.map { faction.toString() + it }) {
                BitmapCache.instance.addBitmap(file + ".png", Bitmap.Config.ARGB_4444)
            }
        }
        BitmapCache.instance.addBitmap("flag_pole.png", Bitmap.Config.ARGB_4444)

        var idx = 0
        for (vert in (Map.sharedInstance.graph.vertices)) {
            val town = vert.data as? TownModel
            if (town == null || town!!.allegiance == TownModel.Allegiance.none) {
                continue
            }
            var flag = flags[idx]
            var bimpl = mutableListOf<BitmapStream>()
            for (frame in framenames) {
                bimpl.add(BitmapCache.instance.getBitmap(town!!.allegiance.toString() + frame + ".png")!!)
            }
            val sprite = CanvasSprite(bimpl[0])
            val action = CanvasActionAnimate(bimpl, 100)
            sprite.position = Point(((flag[0] + 8) * density).toInt(), ((-flag[1] - 32) * density).toInt())
            root.addChild(sprite)
            sprite.run(action)

            val flag_pole = CanvasSprite(BitmapCache.instance.getBitmap("flag_pole.png")!!)
            flag_pole.position = Point((flag[0] * density).toInt(), ((-flag[1] - 16) * density).toInt())
            root.addChild(flag_pole)
            idx += 1
        }
    }

    override fun onScrollChanged(offset: Point) {
        root.position = -offset + padding
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return super.onTouchEvent(event)
    }

    fun plotRouteToTown(start: TownController, end: TownController, image: String) {
        val path = Map.sharedInstance.getRoute(start.model, end.model)
        showCourseTrail(path, image)
    }

    fun plotJobMarkers(boat: BoatModel) {
        _jobNode.removeAllChildren()
        var towns = mutableSetOf<TownModel>()
        boat.cargo.forEach {
            if (it != null) {
                towns.add(it.destination)
            }
        }
        for (town in towns) {
            val pos = Map.sharedInstance.townPosition(town)
            val marker = CanvasSprite(BitmapCache.instance.getBitmap("job_marker.png")!!)
            marker.position.set(pos.x,pos.y)
            marker.zOrder = 2
            marker.anchor = PointF(0.5f, 0.0f)
            marker.run(
                CanvasActionRepeat(
                    CanvasActionSequence(
                        CanvasActionMoveBy(500, Point(0, 8)),
                        CanvasActionMoveBy(500, Point(0, -8))
                    )
                )
            )
            _jobNode.addChild(marker)
        }
    }

    fun plotRoutesForTown(town: TownModel, paths: MutableList<List<Edge<WorldNode>>>, distance: Float) {
        _selectionCircle?.parent = null
        val scene_pos = Map.sharedInstance.townPosition(town)
        val shape = CanvasCustom({ canvas, node, pos ->
            val paint = Paint()
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 3.0f
            paint.color = Color.parseColor("#ffffffff")
            canvas.drawCircle(pos.x.toFloat(), pos.y.toFloat(), distance * node.scale.width, paint)
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#33ffffff")
            canvas.drawCircle(pos.x.toFloat(), pos.y.toFloat(), distance * node.scale.width, paint)
        }
        , Size((distance * 2).toInt(),(distance * 2).toInt()))
        shape.position = Point(scene_pos)
        shape.scale = SizeF(0.0f,0.0f)
        shape.run(CanvasActionScaleTo(1000,1.0f))
        if (paths.size > 0) {
            val parts = Map.sharedInstance.mergeRoutes(paths[0].first().source, paths)
            for (part in parts) {
                showCourseTrail(part, "nav_plot.png")
            }
        }
        _plotNode.addChild(shape)
    }

    fun scrollViewDidScroll(scrollView: ScrollView) {
        root.position = Point(-scrollView.scrollX, scrollView.scrollY) + padding
    }

    fun showCourseTrail(path: List<Edge<WorldNode>>, image: String) {
        var spline = CardinalSpline(Graph.getRoutePositions(path))
        spline.getUniform(max(spline.length / density * 0.06, 4.0).toInt())
        spline.getUniform(max(spline.length / density * 0.06, 4.0).toInt())
        for (point in 0..spline.path.size - 1) {
            val plotSprite = CanvasSprite(BitmapCache.instance.getBitmap(image)!!)
            plotSprite.position = Point(spline.path[point])
            _plotNode.addChild(plotSprite)
            if (point == 0) { plotSprite.hidden = true }
        }
    }

    fun plotCourseForBoat(boat: BoatController) {
        val percent = boat.model.percentCourseComplete
        val time = boat.model.remainingTime
        val size = _plotNode.children.size
        for (i in 0..size - 1) {
            val rtime = size * percent
            if (i <= rtime.toInt()) {
                _plotNode.children[i].run(CanvasActionRemoveFromParent())
            } else {
                val action = CanvasActionSequence(
                    CanvasActionWait(((time / (size - rtime)) * (i - rtime)).toLong()),
                    CanvasActionScaleTo(500, 0.0f),
                    CanvasActionRemoveFromParent()
                )
                _plotNode.children[i].run(action)
            }
        }
    }

}