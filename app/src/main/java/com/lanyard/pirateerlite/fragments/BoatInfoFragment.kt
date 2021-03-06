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

package com.lanyard.pirateerlite.fragments

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.lanyard.pirateerlite.MapActivity
import com.lanyard.pirateerlite.R
import com.lanyard.pirateerlite.models.BoatModel
import com.lanyard.pirateerlite.models.TownModel
import com.lanyard.pirateerlite.singletons.User

/**
 * Displays information about a boat class including vital statistics, cost and a description.
 *
 * @author Peter Respondek
 *
 * @see R.layout.fragment_boatinfo
 */

class BoatInfoFragment : Fragment(), User.UserListener {
    var boatType: String? = null
    var parts = ArrayList<User.BoatPart>()
    var build = false

    fun boatValue(index: BoatModel.BoatIndex): Any {
        return BoatModel.boatConfig(boatType!!, index)
    }

    class BoatInfoDialogFragment : androidx.fragment.app.DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            return activity.let {
                val builder = AlertDialog.Builder(it!!)
                builder.setMessage(R.string.noboatslots).setNegativeButton(R.string.ok, { dialog, id -> })
                builder.create()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.fragment_boatinfo, container, false)
        val boatNameLabel = view.findViewById<TextView>(R.id.boatInfoLable)
        val rangeLabel = view.findViewById<TextView>(R.id.boatInfoRange)
        val speedLabel = view.findViewById<TextView>(R.id.boatInfoSpeed)
        val boatImage = view.findViewById<ImageView>(R.id.boatInfoImage)
        val cargoSizeLabel = view.findViewById<TextView>(R.id.boatInfoCargo)
        val boatInfo = view.findViewById<TextView>(R.id.boatInfoDescription)
        val harbourSizeLabel = view.findViewById<ImageView>(R.id.boatInfoTown)
        val button = view.findViewById<TextView>(R.id.boatInfoBuildButton)

        User.instance.addListerner(this)

        val args = arguments
        if (savedInstanceState != null) {
            boatType = savedInstanceState.getString("boatType")
            build = savedInstanceState.getBoolean("build")
        } else if (args != null) {
            boatType = args.getString("boatType")
            build = args.getBoolean("build")
        }

        button.setOnClickListener {
            var manager = fragmentManager
            if (manager != null) {
                if (User.instance.numBoats < User.instance.boatSlots) {
                    var map: MapFragment? = null
                    map = manager.findFragmentByTag("map") as MapFragment
                    map.stopTracking()
                    map.reset()
                    button.isEnabled = false
                    build = true
                    if (resources.getBoolean(R.bool.landscape) != true) {
                        (activity as MapActivity).swapFragment(R.id.navigation_map, false) as MapFragment
                    }
                    map.transferBoatBuild(this)
                    map.buildBoat()
                } else {
                    val dialog = BoatInfoDialogFragment()
                    dialog.show(manager, "expand")
                }
            }
        }

        if (build == true) {
            button.isEnabled = false
        }

        rangeLabel.text = getString(R.string.rangeLabel, boatValue(BoatModel.BoatIndex.distance))
        speedLabel.text = getString(R.string.speedLabel, boatValue(BoatModel.BoatIndex.speed))
        val imgname = boatValue(BoatModel.BoatIndex.image) as String
        boatImage.setImageResource(
            context!!.resources.getIdentifier(
                imgname.removeRange(
                    imgname.length - 4,
                    imgname.length
                ), "drawable", context!!.packageName
            )
        )
        boatNameLabel.text = boatValue(BoatModel.BoatIndex.title) as String
        cargoSizeLabel.text = (boatValue(BoatModel.BoatIndex.hold_size) as Double).toInt().toString()
        boatInfo.text = boatValue(BoatModel.BoatIndex.description) as? String
        when (TownModel.HarbourSize.valueOf(boatValue(BoatModel.BoatIndex.harbourType) as String)) {
            TownModel.HarbourSize.pier ->
                harbourSizeLabel.setImageResource(
                    context!!.resources.getIdentifier("town_c1_unselected", "drawable", context!!.packageName)
                )
            TownModel.HarbourSize.docks ->
                harbourSizeLabel.setImageResource(
                    context!!.resources.getIdentifier("town_c2_unselected", "drawable", context!!.packageName)
                )
            TownModel.HarbourSize.marina ->
                harbourSizeLabel.setImageResource(
                    context!!.resources.getIdentifier("town_c3_unselected", "drawable", context!!.packageName)
                )
        }
        update(view)
        return view
    }

    fun update() {
        update(view)
    }

    private fun update(view: View?) {
        if (view == null) {
            return
        }
        val button = view.findViewById<TextView>(R.id.boatInfoBuildButton)
        val hullLabel = view.findViewById<TextView>(R.id.boatInfoHull)
        val partsLabel = view.findViewById<TextView>(R.id.boatInfoRigging)
        val sailsLabel = view.findViewById<TextView>(R.id.boatInfoSails)
        val cannonsLabel = view.findViewById<TextView>(R.id.boatInfoCannons)

        val arr = arrayOf(hullLabel, partsLabel, sailsLabel, cannonsLabel)
        val parts = boatValue(BoatModel.BoatIndex.part_amount) as List<Int>
        var canBuild = true
        for (i in 0 until parts.size) {
            val currPart = User.BoatPart(boatType!!, User.MarketItem.withIndex(i))
            val label = arr[i]
            val tparts = (User.instance.parts.filter { it == currPart })
            val numParts = tparts.size
            val targetParts = parts[i]
            label?.text = getString(R.string.partsLabel, numParts, targetParts)
            if (numParts >= targetParts) {
                for (j in 0 until targetParts) {
                    this.parts.add(tparts[j])
                }
                label?.setTextColor(Color.parseColor("#ff00ff00"))
            } else {
                label?.setTextColor(Color.parseColor("#ffff0000"))
                canBuild = false
            }
        }
        button?.isEnabled = canBuild
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("boatType", boatType)
        outState.putBoolean("build", build)
    }

    override fun boatAdded(boat: BoatModel) {
        super.boatAdded(boat)
        update(view)
    }
}