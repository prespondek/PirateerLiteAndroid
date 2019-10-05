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

package com.lanyard.pirateerlite.singletons

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.SparseArray
import kotlinx.coroutines.*
import java.lang.NullPointerException
import java.lang.ref.WeakReference
import java.util.Collections.synchronizedMap

open class Audio private constructor(context: Context) : SoundPool.OnLoadCompleteListener {

    override fun onLoadComplete(soundPool: SoundPool?, sampleId: Int, status: Int) {
        synchronized(mediaState) {
            for (i in 0 until mediaState.size()) {
                val key = mediaState.keyAt(i)
                val obj = mediaState.get(key)
                if (obj.res == sampleId) {
                    obj.loaded = true
                }
            }
        }
    }

    companion object {
        val instance: Audio get() = _instance ?: throw NullPointerException()
        internal var _instance: Audio? = null
        fun initialize(context: Context) {
            if (_instance == null) {
                _instance = Audio(context)
            }
        }
    }

    class MediaTicket(var state: MediaInfo, var looping: Boolean)
    class MediaInfo(var res: Int, var loaded: Boolean)

    var context: WeakReference<Context>
    val audioTickets = synchronizedMap<Int,MediaTicket>(mutableMapOf())
    private val soundPool : SoundPool
    private val audioLoop: Job
    private val mediaState = SparseArray<MediaInfo>()

    init {
        soundPool = SoundPool.Builder().apply {
            setMaxStreams(8)
            setAudioAttributes(AudioAttributes.Builder().apply {
                setUsage(AudioAttributes.USAGE_GAME)
                setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            }.build())
        }.build()
        _instance = this
        soundPool.setOnLoadCompleteListener(this)
        this.context = WeakReference(context)
        audioLoop = GlobalScope.launch {
            while (this.isActive) {
                playSounds()
                delay(500)
            }
        }
    }

    private fun playSounds() {
        if (context.get() != null && !audioTickets.isEmpty()) {
            synchronized(audioTickets) {
                val iter = audioTickets.iterator()
                while (iter.hasNext()) {
                    val ticket = iter.next().value
                    if (ticket.state.loaded == true) {
                        var loop = 0
                        if (ticket.looping == true) {
                            loop = -1
                        }
                        soundPool.play(ticket.state.res,1.0f,1.0f,1, loop,1.0f)
                        iter.remove()
                    }
                }
            }
        }
    }

    fun pause() {
        soundPool.autoPause()
    }

    fun resume() {
        soundPool.autoResume()
    }

    fun loadSound(res: Int) : MediaInfo {
        var idx = mediaState[res]
        if (idx == null) {
            idx = MediaInfo(soundPool.load(context.get(),res,1), false)
            mediaState.put(res,idx)
        }
        return idx
    }

    fun queueSound(file: String, looping: Boolean = false) {
        val idx = context.get()?.resources?.getIdentifier(file, "raw", context.get()?.packageName)
        if (idx != null) {
            queueSound(idx, looping)
        }
    }

    fun queueSound(res: Int, looping: Boolean = false) {
        var idx = loadSound(res)
        val ticket = audioTickets[res]
        if (ticket == null) {
            audioTickets[res] = MediaTicket(idx, looping)
        }
    }
}