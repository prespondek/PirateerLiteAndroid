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

package com.lanyard.pirateerlite.viewmodels

import android.graphics.Point
import androidx.lifecycle.ViewModel
import com.lanyard.pirateerlite.models.BoatModel
import com.lanyard.pirateerlite.models.TownModel
import com.lanyard.pirateerlite.singletons.User

/**
 * Holds transient data needed to maintain map UI consistency across its lifetime.
 * Things like the position of the view, selected boat and boats pending course.
 *
 * @author Peter Respondek
 */

class MapViewModel : ViewModel() {
    var position: Point?
    var selectedBoat: BoatModel?
    var buildType: String?
    var buildParts: ArrayList<User.BoatPart>?
    var trackBoat: Boolean
    var boatCourse: ArrayList<TownModel>


    init {
        position = null
        selectedBoat = null
        buildType = null
        buildParts = null
        trackBoat = false
        boatCourse = arrayListOf()
    }
}