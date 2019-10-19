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

import android.content.Context;
import android.graphics.*
import android.util.AttributeSet;
import android.view.MotionEvent
import android.util.DisplayMetrics
import android.view.WindowManager
import android.widget.ScrollView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.lanyard.canvas.*
import com.lanyard.helpers.*
import com.lanyard.library.*
import com.lanyard.pirateerlite.R
import com.lanyard.pirateerlite.controllers.BoatController
import com.lanyard.pirateerlite.controllers.TownController
import com.lanyard.pirateerlite.models.BoatModel
import com.lanyard.pirateerlite.models.TownModel
import com.lanyard.pirateerlite.models.WorldNode
import java.io.InputStreamReader
import com.lanyard.pirateerlite.singletons.Map
import java.lang.NullPointerException
import kotlin.math.max


class MapView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    CanvasView(context, attrs, defStyleAttr) {
    private var _root = CanvasNode()
    var position : Point
        get() = _root.position
    set(value) {
        println("position set:" + _root.position)
        _root.position.set(value)
    }
    var padding = Size(0, 0)
    var density = 0.0f
    private var _plotNode: CanvasNode
    private var _jobNode: CanvasNode
    private var _messageNode: CanvasNode
    private var _selectionCircle: CanvasNode?

    init {
        this.scene = CanvasScene()
        this.scene?.addChild(_root)
        this._plotNode = CanvasNode()
        this._jobNode = CanvasNode()
        this._messageNode = CanvasNode()
        this._selectionCircle = null

        var metrics = DisplayMetrics()
        var wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        wm.defaultDisplay.getMetrics(metrics)
        density = metrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT
        val gson = Gson()
        val reader = JsonReader(InputStreamReader(context.getAssets().open("map_view.json")))
        val json = gson.fromJson<HashMap<String, Any>>(reader, object : TypeToken<HashMap<String, Any>>() {}.type)
        setup(context,json)
    }

    fun clearPlot() {
        _plotNode.removeAllChildren()
    }

    fun addChild(node: CanvasNode) {
        _root.addChild(node)
    }

    fun clearJobMarkers() {
        _jobNode.removeAllChildren()
    }

    fun makeBackground(context: Context, data: ArrayList<String>, files: ArrayList<String>) {
        //var tile_index = json.getJSONArray("TileIndex")
        //var tile_file = json.getJSONArray("TileFile")
        for (y in 1..data.size) {
            val str = data[y - 1].toString()
            for (x in 1..str.length) {
                var index = str[x - 1].toInt() - 48
                var file = files[index] as String
                val tile = BitmapCache.instance.addBitmap(context, file, Bitmap.Config.RGB_565)!!
                var sprite = CanvasSprite(tile)
                sprite.anchor.set(0.0f, 1.0f)
                sprite.position.set(tile.width * (x - 1), tile.height * (y - 1))
                this._root.addChild(sprite)
            }
        }
    }

    private fun setup(context: Context, data: HashMap<String, Any>) {
        setPadding(data["Padding"] as ArrayList<Int>)
        setDimensions(data["Dimensions"] as ArrayList<Int>)
        makeBackground(context,
            data["TileIndex"] as ArrayList<String>,
            data["TileFile"] as ArrayList<String>
        )
        BitmapCache.instance.addBitmap(context,"job_marker.png", Bitmap.Config.ARGB_4444)
        makeAnimations(context, data)
        makeFlags(context, data)
        _root.addChild(_plotNode)
        _root.addChild(_jobNode)
        _jobNode.zOrder = 3
    }

    fun setPadding(data: ArrayList<Int>) {
        padding = Size(data[0], -(data[1]))
        _root.position.set(padding.width, padding.height)
    }

    fun setDimensions(data: ArrayList<Int>) {
        scene?.magnitude?.set(
            ((data[0] + padding.width * 2) * density).toInt(),
            ((data[1] + padding.height * 2) * density).toInt()
        )
    }

    fun makeAnimations(context: Context, data: HashMap<String, Any>) {

        var anim = data["Animations"] as ArrayList<ArrayList<Any>>
        for (atom in anim) {
            val name = atom[0] as String
            val framenames = data[name] as ArrayList<String>
            var bimpl = ArrayList<BitmapStream>()
            val frameTime = 1000 * atom[2] as Double
            for (frame in framenames) {
                var bimp = BitmapCache.instance.addBitmap(context, frame + ".png", Bitmap.Config.ARGB_4444) ?: throw NullPointerException()
                bimp.timer = frameTime.toLong() * framenames.size + 100
                bimpl.add(bimp)
            }
            val sprite = CanvasSprite(bimpl[0])
            val action = CanvasActionAnimate(bimpl, frameTime.toInt())

            sprite.scale.set((atom[5] as Double).toFloat(), (atom[6] as Double).toFloat())
            sprite.position.set(
                (atom[3] as Double * density).toInt(),
                -(atom[4] as Double * density).toInt()
            )
            sprite.run(action)
            _root.addChild(sprite)
        }
    }

    fun makeFlags(context: Context, data: HashMap<String, Any>) {
        val flags = data["Flags"] as ArrayList<ArrayList<Double>>
        var framenames = data["flags"] as ArrayList<String>

        for (faction in TownModel.Allegiance.values) {
            if (faction == TownModel.Allegiance.none) {
                continue
            }
            for (file in framenames.map { faction.toString() + it }) {
                BitmapCache.instance.addBitmap(context,file + ".png", Bitmap.Config.ARGB_4444)
            }
        }
        BitmapCache.instance.addBitmap(context,"flag_pole.png", Bitmap.Config.ARGB_4444)

        var idx = 0
        for (vert in (Map.instance.graph.vertices)) {
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
            sprite.position.set(((flag[0] + 8) * density).toInt(), ((-flag[1] - 32) * density).toInt())
            _root.addChild(sprite)
            sprite.run(action)

            val flag_pole = CanvasSprite(BitmapCache.instance.getBitmap("flag_pole.png")!!)
            flag_pole.position.set((flag[0] * density).toInt(), ((-flag[1] - 16) * density).toInt())
            _root.addChild(flag_pole)
            idx += 1
        }
    }



    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return super.onTouchEvent(event)
    }

    fun plotRouteToTown(start: TownController, end: TownController, image: String) {
        val path = Map.instance.getRoute(start.model, end.model)
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
            val pos = Map.instance.townPosition(town)
            val marker = CanvasSprite(BitmapCache.instance.getBitmap("job_marker.png")!!)
            marker.position.set(pos.x,pos.y - 32)
            marker.zOrder = 2
            marker.anchor = PointF(0.5f, 0.0f)
            marker.scale.set(1.25f,1.25f)
            marker.run(
                CanvasActionRepeat(
                    CanvasActionSequence(
                        CanvasActionMoveBy(500, Point(0, 32)),
                        CanvasActionMoveBy(500, Point(0, -32))
                    )
                )
            )
            _jobNode.addChild(marker)
        }
    }

    fun plotRoutesForTown(town: TownModel, paths: MutableList<List<Edge<WorldNode>>>, distance: Float) {
        _selectionCircle?.parent = null
        val scene_pos = Map.instance.townPosition(town)
        var stroke = context.resources.getDimensionPixelSize(R.dimen.boat_range_stroke_size).toFloat()
        val shape = CanvasCustom({ canvas, node, transform ->
            val paint = Paint()
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = stroke
            paint.color = Color.parseColor("#ffffffff")
            canvas.drawCircle(
                transform.position.x.toFloat(),
                transform.position.y.toFloat(),
                distance * node.scale.width, paint)
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#33ffffff")
            canvas.drawCircle(
                transform.position.x.toFloat(),
                transform.position.y.toFloat(),
                distance * node.scale.width, paint)
        }
        , Size((distance * 2).toInt(),(distance * 2).toInt()))
        shape.position.set(scene_pos)
        shape.scale.set(0.0f,0.0f)
        shape.run(CanvasActionScaleTo(1000,1.0f))
        if (paths.size > 0) {
            val parts = Map.instance.mergeRoutes(paths[0].first().source, paths)
            for (part in parts) {
                showCourseTrail(part, "nav_plot.png")
            }
        }
        _plotNode.addChild(shape)
    }

    fun scrollViewDidScroll(scrollView: ScrollView) {
        _root.position.set(-scrollView.scrollX + padding.width, scrollView.scrollY + padding.height)
    }

    fun showCourseTrail(path: List<Edge<WorldNode>>, image: String) {
        var spline = CardinalSpline(Graph.getRoutePositions(path))
        spline.getUniform(max(spline.length / density * 0.06, 4.0).toInt())
        spline.getUniform(max(spline.length / density * 0.06, 4.0).toInt())
        for (point in 0 until spline.path.size) {
            val plotSprite = CanvasSprite(BitmapCache.instance.getBitmap(image)!!)
            plotSprite.position.set(spline.path[point])
            _plotNode.addChild(plotSprite)
            if (point == 0) { plotSprite.hidden = true }
        }
    }

    fun plotCourseForBoat(boat: BoatController) {
        val percent = boat.model.percentCourseComplete
        val time = boat.model.remainingTime
        val size = _plotNode.children.size
        synchronized(_plotNode.children) {
            for (i in 0 until size) {
                val rtime = size * percent
                if (i <= rtime.toInt()) {
                    _plotNode.children[i].run(CanvasActionRemoveFromParent())
                } else {
                    val action = CanvasActionSequence(
                        CanvasActionWait(((time / (size - rtime)) * (i - rtime)).toLong()),
                        CanvasActionScaleTo(1000, 0.0f),
                        CanvasActionRemoveFromParent()
                    )
                    _plotNode.children[i].run(action)
                }
            }
        }
    }

    fun showMoney(town: TownModel, gold: Int, silver: Int) {
        val pos = Map.instance.townPosition(town)
        var arr = ArrayList<Any>()

        if (gold > 0) {
            arr.addAll(arrayOf(R.drawable.gold_piece, gold.toString(), Color.parseColor("#ffffdd00"))) // 1.0f, 0.9f, 0.0f,1.0f
        }
        if (silver > 0) {
            arr.addAll(arrayOf(R.drawable.silver_piece, silver.toString(), Color.parseColor("#ffeeeeee"))) //  0.9f, 0.9f, 0.9f, 1.0f
        }
        var sarr = ArrayList<CanvasNode>()
        for (i in 0 until arr.size step 3) {
            val bmp = BitmapCache.instance.getBitmap(arr[i].toString())
            var image = CanvasSprite(bmp!!)
            val label = CanvasLabel("0",null)
            label.fontSize = context.resources.getDimensionPixelSize(R.dimen.money_label_size).toFloat()
            label.fontStyle = Paint.Style.FILL_AND_STROKE
            label.strokeWidth = context.resources.getDimensionPixelSize(R.dimen.money_label_stroke).toFloat()
            label.text = "+" + arr[i+1].toString()
            val scale = label.size.height.toFloat() / image.size.height * 1.75f
            image.scale.set(scale)
            var pad = 0.0f
            if (i > 0) {
                pad += (sarr[i-3] as CanvasSprite).size.width * 1.5f
                pad += (sarr[i-2] as CanvasLabel).size.width
            }
            label.fontColor = arr[i+2] as Int
            image.position.set(pad.toInt(), 0)
            label.position.set(image.position.x + (image.size.width.toInt() *0.5f).toInt()+ (label.size.width * 0.5f).toInt(), image.position.y)
            sarr.addAll(arrayOf(image,label))
        }
        var messageWidth = 0
        var messageHeight = 0
        sarr.forEach({
            messageWidth += it.size.width;
            messageHeight = max(messageHeight, it.size.height)
        })
        var messageSize = Size(messageWidth,messageHeight)
        val message = CanvasNode()

        sarr.forEach({
            message.addChild(it)
        })
        message.position.set(pos - messageSize * 0.5f)
        message.position.y -= 64
        message.run(CanvasActionMoveBy(1000,Point(0,-128)))
        message.run(CanvasActionSequence(
            CanvasActionWait(500),
            CanvasActionFadeTo(1000,0.0f),
            CanvasActionRemoveFromParent()))
        _root.addChild(message)
    }

}