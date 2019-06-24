package com.lanyard.canvas

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.DisplayMetrics
import android.util.Size


class BitmapStream {
    companion object {
        var scale : Int
        var context : Context?
        var density : Float
        init {
            scale = 2
            context = null
            density =  DisplayMetrics.DENSITY_DEVICE_STABLE.toFloat() / DisplayMetrics.DENSITY_DEFAULT
        }
    }
    var color : Int = 0
    private var _lowBitmap : Bitmap? = null
    private var _highBitmap : Bitmap? = null
    private var _fullSize : Size
    private var _scale : Int = 1
    private var _config : Bitmap.Config = Bitmap.Config.ARGB_8888
    private var _file : String = ""

    val bitmap : Bitmap get () {
        if (_highBitmap != null) {
            return _highBitmap!!
        } else {
            return _lowBitmap!!
        }
    }

    val width : Int get () { return _fullSize.width }

    val height : Int get () { return _fullSize.height }

    constructor(file: String, options: Bitmap.Config) {
        _config = options
        _file = file
        var bitmapOptions = BitmapFactory.Options()
        bitmapOptions.inPreferredConfig = options
        bitmapOptions.inDensity = DisplayMetrics.DENSITY_DEFAULT
        bitmapOptions.inTargetDensity = DisplayMetrics.DENSITY_DEVICE_STABLE;
        bitmapOptions.inScaled = true
        bitmapOptions.inJustDecodeBounds = true

        var bimp = BitmapFactory.decodeStream ( context!!.resources.assets.open(file), null, bitmapOptions)
        _fullSize = Size((bitmapOptions.outWidth * density).toInt(),
            (bitmapOptions.outHeight * density).toInt())

        bitmapOptions.inJustDecodeBounds = false
        bitmapOptions.inSampleSize = scale
        _scale = scale
        _lowBitmap = BitmapFactory.decodeStream ( context!!.resources.assets.open(file), null, bitmapOptions)
    }

    fun inflate() {
        if (_highBitmap == null) {
            val bitmapOptions = BitmapFactory.Options()
            bitmapOptions.inPreferredConfig = _config
            bitmapOptions.inDensity = DisplayMetrics.DENSITY_DEFAULT
            bitmapOptions.inTargetDensity = DisplayMetrics.DENSITY_DEVICE_STABLE;
            bitmapOptions.inScaled = true
            _highBitmap = BitmapFactory.decodeStream ( context!!.resources.assets.open(_file), null, bitmapOptions)
        }
    }

}