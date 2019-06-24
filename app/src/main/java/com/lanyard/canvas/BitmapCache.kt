package com.lanyard.canvas

import android.graphics.Bitmap


class BitmapCache private constructor() {
    val bitmapCache: HashMap<String, BitmapStream>

    companion object {
        private var _instance : BitmapCache? = null
        val instance : BitmapCache
            get(){
            if (_instance == null) {
                _instance = BitmapCache()
            }
            return _instance!!
        }
    }

    init {
        bitmapCache = HashMap<String, BitmapStream>()
    }

    fun addBitmap(filename: String, config: Bitmap.Config) : BitmapStream? {
        var file = BitmapStream(filename, config)
        if (file != null) {
            bitmapCache.put(filename,file)
        }
        return file
    }

    fun getBitmap(filename: String)  : BitmapStream? {
        return bitmapCache.get(filename)
    }
}