package com.lanyard.pirateerlite.viewmodels

import android.graphics.Point
import androidx.lifecycle.ViewModel
import com.lanyard.pirateerlite.models.BoatModel
import com.lanyard.pirateerlite.models.TownModel
import com.lanyard.pirateerlite.singletons.User

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