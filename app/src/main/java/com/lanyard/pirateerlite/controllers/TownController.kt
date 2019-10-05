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

package com.lanyard.pirateerlite.controllers

import android.view.View
import android.widget.Button
import com.lanyard.pirateerlite.fragments.MapFragment
import com.lanyard.pirateerlite.models.TownModel
import com.lanyard.pirateerlite.views.TownView


class TownController (town: TownModel, button: Button, view: TownView )  {
    companion object {
        private var _controller : MapFragment? = null
        fun setController ( controller: MapFragment ) {
            TownController._controller = controller
        }
    }

    enum class State {
        selected, unselected, disabled, blocked
    }

    var model : TownModel
    var button : Button
    var view : TownView
    var state : State = State.disabled
        get() {
            return field
        }
        set (state) {
            if ((state == State.selected || state == State.unselected) && model.level == 0) {
                field = State.disabled
            } else {
                field = state
            }
            view.setState(field, model.harbour)
        }




    init{
        this.model =    town
        this.button =   button
        this.view =   view
        this.button.setOnClickListener(View.OnClickListener {
            this.buttonPressed(it)
        })
        updateView()
    }

    fun updateView () {
        view.setCounter(model.boats.size)
    }
    fun reset() {
        if (model.level > 0) {
            state = State.unselected
        }
    }

    fun buttonPressed(view: View) {

        TownController._controller?.townSelected(this)
    }
}