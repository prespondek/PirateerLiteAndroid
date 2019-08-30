package com.lanyard.canvas

import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache
import java.sql.Timestamp
import java.util.concurrent.*
import javax.xml.datatype.DatatypeConstants.SECONDS




class BitmapCache private constructor() {
    val streamCache: LinkedHashMap<String, BitmapStream>
    val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    val cacheSize = maxMemory / 2
    private var _threadPool = Executors.newFixedThreadPool(4)
    var enabled = true

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
        streamCache = LinkedHashMap<String, BitmapStream>()
    }


    fun addBitmap(context: Context, filename: String, config: Bitmap.Config) : BitmapStream? {
        var file = getBitmap(filename)
        if (file == null) {
            file = addBitmap(BitmapStream.BitmapGenerator.make(context, filename,config))
        }
        return file
    }

    fun addBitmap(context: Context, filename: String, resource: Int, config: Bitmap.Config) : BitmapStream? {
        var file = getBitmap(resource.toString())
        if (file == null) {
            file = addBitmap(BitmapStream.BitmapGenerator.make(context, resource,config))
        }
        return file
    }

    private fun addBitmap(generator: BitmapStream.BitmapGenerator) : BitmapStream? {
        var file = BitmapStream(generator)
        streamCache.put(generator.name, file)
        return file
    }

    fun getBitmap(res: Int)  : BitmapStream? {
        return getBitmap(res.toString())
    }

    fun getBitmap(filename: String)  : BitmapStream? {
        return streamCache.get(filename)
        /*var bimp = streamCache.remove(filename)
        if (bimp != null) {
            streamCache.put(filename,bimp)
        }
        return bimp*/
    }

    fun fetchBitmap(stream: BitmapStream) : Future<*>
    {
        return _threadPool.submit(stream.BitmapFetcher())
    }

    fun flushExpired(timestamp: Long) {
        if (enabled == false) { return }
        synchronized(streamCache) {
            var itr = streamCache.iterator()
            while (itr.hasNext()) {
                var stream = itr.next().value
                stream.flush(timestamp)
                /*if (!stream.flush(timestamp)) {
                    break
                }*/
            }
        }
    }

}