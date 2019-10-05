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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import com.lanyard.canvas.CanvasLabel
import com.lanyard.canvas.CanvasSprite
import com.lanyard.canvas.BitmapCache
import com.lanyard.canvas.SizeF
import com.lanyard.pirateerlite.R
import com.lanyard.pirateerlite.controllers.TownController
import com.lanyard.pirateerlite.models.TownModel
import kotlin.math.min

class TownView : CanvasSprite( BitmapCache.instance.getBitmap("town_c1_selected.png")!! ) {
    companion object {
        var fontSize : Float = 8.0f
        fun setup(context: Context) {
            fontSize = context.resources.getDimensionPixelSize(R.dimen.boat_counter_label_size).toFloat()
            var arr = arrayOf(
                "town_c1_selected.png",     "town_c2_selected.png",     "town_c3_selected.png",
                "town_c1_unselected.png",   "town_c2_unselected.png",   "town_c3_unselected.png",
                "town_c1_disabled.png",     "town_c2_disabled.png",     "town_c3_disabled.png")
            for (n in arr) {
                BitmapCache.instance.addBitmap(context, n,Bitmap.Config.ARGB_4444)
            }
        }
    }
    private var boatCounter : CanvasLabel
    init {
        scale = SizeF(0.5f,0.5f)
        boatCounter = CanvasLabel("0", null)
        boatCounter.fontSize = fontSize
        boatCounter.fontColor = Color.BLACK
        boatCounter.typeface = Typeface.DEFAULT_BOLD
        //boatCounter.anchor = PointF(0.5f,0.5f)
        //boatCounter.position = (size * 0.5f).toPoint()
        addChild(boatCounter)
    }
    fun setCounter (num : Int) {
        if (num > 0) {
            boatCounter.text = min(9,num).toString()
        } else {
            boatCounter.text = ""
        }
    }
    fun setState (state: TownController.State, townSize: TownModel.HarbourSize) {
        when (state) {
            TownController.State.selected ->
            when (townSize) {
                TownModel.HarbourSize.pier -> texture = BitmapCache.instance.getBitmap("town_c1_selected.png")!!
                TownModel.HarbourSize.docks -> texture = BitmapCache.instance.getBitmap( "town_c2_selected.png")!!
                TownModel.HarbourSize.marina -> texture = BitmapCache.instance.getBitmap( "town_c3_selected.png" )!!
            }
            TownController.State.unselected ->
            when (townSize) {
                TownModel.HarbourSize.pier -> texture = BitmapCache.instance.getBitmap(  "town_c1_unselected.png")!!
                TownModel.HarbourSize.docks -> texture = BitmapCache.instance.getBitmap(  "town_c2_unselected.png" )!!
                TownModel.HarbourSize.marina -> texture = BitmapCache.instance.getBitmap(  "town_c3_unselected.png" )!!
            }
            else ->
            when (townSize) {
                TownModel.HarbourSize.pier -> texture = BitmapCache.instance.getBitmap(  "town_c1_disabled.png" )!!
                TownModel.HarbourSize.docks -> texture = BitmapCache.instance.getBitmap(  "town_c2_disabled.png" )!!
                TownModel.HarbourSize.marina -> texture = BitmapCache.instance.getBitmap(  "town_c3_disabled.png" )!!
            }
        }
    }
}