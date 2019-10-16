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

package com.lanyard.canvas

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.DisplayMetrics
import android.util.Size
import androidx.lifecycle.LiveData
import java.util.concurrent.Future


class BitmapStream {
    companion object {
        var scale : Int
        var density : Float
        var densityDpi : Int
        var defaultTime : Long
        init {
            scale = 4
            density = 1.0f
            densityDpi = 1
            defaultTime = 500
        }
    }

    inner class BitmapFetcher() : Runnable {

        override fun run() {
            _highBitmap = inflate()
            _future = null
            //println("adding " + _generator.name)
        }
    }

    abstract class BitmapGenerator (val name: String, val options: Bitmap.Config) {
        companion object {
            fun make(context: Context, file: String, options: Bitmap.Config) : BitmapGenerator
            {
                return object : BitmapGenerator(file, options) {
                    override fun generate(options: BitmapFactory.Options): Bitmap? {
                        return BitmapFactory.decodeStream ( context.resources.assets.open(file), null, options)
                    }
                }
            }

            fun make(context: Context, res: Int, options: Bitmap.Config) : BitmapGenerator
            {
                return object : BitmapGenerator(res.toString(), options) {
                    override fun generate(options: BitmapFactory.Options): Bitmap? {
                        return BitmapFactory.decodeResource(context.resources, res, options)
                    }
                }
            }
        }

        abstract fun generate(options: BitmapFactory.Options) : Bitmap?
    }

    var color : Int = 0
    lateinit var _lowBitmap : Bitmap
    private var _highBitmap : Bitmap? = null
    private var _fullSize : Size = Size(0,0)
    private var _scale : Int = 1
    private var _generator : BitmapGenerator
    private var _timestamp : Long
    var timer : Long
    private var _future : Future<*>? = null

    fun getBitmap(timestamp: Long) : Bitmap {
        _timestamp = timestamp
        var bimp = _highBitmap
        if (bimp == null) {
            if (_future == null) {
                _future = BitmapCache.instance.fetchBitmap(this)
            }
            bimp = _lowBitmap
        }
        return bimp
    }

    fun flush (timestamp: Long) : Boolean {
        if (_timestamp + timer <= timestamp) {
            if (_highBitmap != null) {
                println("flushing " + _generator.name)
            }
            _highBitmap = null
            if (_future != null) {
                println("cancelling " + _generator.name)
                _future?.cancel(true)
            }
            _future = null
            return true
        }
        return false
    }

    val width : Int get () { return _fullSize.width }

    val height : Int get () { return _fullSize.height }

init {
    _timestamp = System.currentTimeMillis()
    timer = defaultTime
}
    constructor(context: Context, res: Int, options: Bitmap.Config) : this(BitmapGenerator.make(context, res, options))
    constructor(context: Context, file: String, options: Bitmap.Config) : this(BitmapGenerator.make(context, file, options))
    constructor(generator: BitmapGenerator) {
        _generator = generator
        loadBitmap()
    }

    private fun loadBitmap ()
    {
        var bitmapOptions = BitmapFactory.Options()
        bitmapOptions.inPreferredConfig = _generator.options
        bitmapOptions.inDensity = DisplayMetrics.DENSITY_DEFAULT
        bitmapOptions.inTargetDensity = densityDpi
        bitmapOptions.inScaled = true
        bitmapOptions.inJustDecodeBounds = true

        var bimp = _generator.generate(bitmapOptions)
        _fullSize = Size((bitmapOptions.outWidth * density).toInt(),
            (bitmapOptions.outHeight * density).toInt())

        bitmapOptions.inJustDecodeBounds = false
        bitmapOptions.inSampleSize = scale
        _scale = scale
        _lowBitmap = _generator.generate(bitmapOptions)!!
    }

    fun inflate() : Bitmap? {
        if (_highBitmap == null) {
            val bitmapOptions = BitmapFactory.Options()
            bitmapOptions.inPreferredConfig = _generator.options
            bitmapOptions.inDensity = DisplayMetrics.DENSITY_DEFAULT
            bitmapOptions.inTargetDensity = densityDpi
            bitmapOptions.inScaled = true
            return _generator.generate(bitmapOptions)
        } else {
            return _highBitmap
        }
    }
}