package com.lanyard.pirateerlite.views

import android.graphics.Bitmap
import android.graphics.Color
import com.lanyard.canvas.CanvasLabel
import com.lanyard.canvas.CanvasSprite
import com.lanyard.canvas.BitmapCache
import com.lanyard.pirateerlite.controllers.TownController
import com.lanyard.pirateerlite.models.TownModel
import kotlin.math.min

class TownView : CanvasSprite( BitmapCache.instance.getBitmap("town_c1_selected.png")!! ) {
    companion object {
        fun setup() {
            var arr = arrayOf(
                "town_c1_selected.png",     "town_c2_selected.png",     "town_c3_selected.png",
                "town_c1_unselected.png",   "town_c2_unselected.png",   "town_c3_unselected.png",
                "town_c1_disabled.png",     "town_c2_disabled.png",     "town_c3_disabled.png")
            for (n in arr) {
                BitmapCache.instance.addBitmap(n,Bitmap.Config.ARGB_4444)
            }
        }
    }
    private var boatCounter : CanvasLabel
    init {
        boatCounter = CanvasLabel("0", null)
        boatCounter.fontSize = 12.0f
        boatCounter.fontColor = Color.BLACK
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