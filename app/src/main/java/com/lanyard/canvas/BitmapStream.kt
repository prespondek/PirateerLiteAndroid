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
import android.util.Log
import android.util.Size
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

    inner class BitmapFetcher : Runnable {

        override fun run() {
            _highBitmap = inflate()
            _future = null
            Log.v(TAG, "adding " + _generator.name)
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
    var timestamp: Long = 0L
        private set
    private var _timer: Long
    private var _future : Future<*>? = null
    val TAG = "BitmapStream"

    fun getBitmap(timestamp: Long) : Bitmap {
        touch(timestamp)
        var bimp = _highBitmap
        if (bimp == null) {
            if (_future == null) {
                _future = BitmapCache.instance.fetchBitmap(this)
            }
            bimp = _lowBitmap
        }
        return bimp
    }

    fun touch(timestamp: Long) {
        this.timestamp = timestamp
    }

    fun exire(timestamp: Long): Boolean {
        if (this.timestamp + _timer <= timestamp) {
            if (_highBitmap != null) {
                flush()
            }
            _highBitmap = null
            if (_future != null) {
                Log.v(TAG, "cancelling " + _generator.name)
                _future?.cancel(true)
            }
            _future = null
            return true
        }
        return false
    }

    fun flush() {
        Log.v(TAG, "flushing " + _generator.name)
        _highBitmap = null
    }

    val width : Int get () { return _fullSize.width }

    val height : Int get () { return _fullSize.height }

init {
    this.timestamp = System.currentTimeMillis()
    _timer = defaultTime
}
    constructor(context: Context, res: Int, options: Bitmap.Config) : this(BitmapGenerator.make(context, res, options))
    constructor(context: Context, file: String, options: Bitmap.Config) : this(BitmapGenerator.make(context, file, options))
    constructor(generator: BitmapGenerator) {
        _generator = generator
        loadBitmap()
    }

    private fun loadBitmap ()
    {
        val bitmapOptions = BitmapFactory.Options()
        bitmapOptions.inPreferredConfig = _generator.options
        bitmapOptions.inDensity = DisplayMetrics.DENSITY_DEFAULT
        bitmapOptions.inTargetDensity = densityDpi
        bitmapOptions.inScaled = true
        bitmapOptions.inJustDecodeBounds = true

        _generator.generate(bitmapOptions)
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