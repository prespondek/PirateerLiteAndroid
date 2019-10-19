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

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.lanyard.pirateerlite.R
import com.lanyard.pirateerlite.models.BoatModel
import com.lanyard.pirateerlite.models.TownModel
import com.lanyard.pirateerlite.singletons.User
import com.lanyard.pirateerlite.singletons.Map
import com.lanyard.pirateerlite.singletons.User.UserObserver
import java.util.*
import kotlin.math.max

class StatsFragment : AppFragment() , UserObserver {

    fun update(view: View?) {
        if ( view == null ) { return }
        val levelLabel =            view.findViewById<TextView>(R.id.levelLabel)
        val levelImage =            view.findViewById<ImageView>(R.id.levelImage)
        val nextXpLabel =           view.findViewById<TextView>(R.id.nextXpLabel)
        val totalXpLabel =          view.findViewById<TextView>(R.id.totalXpLabel)
        val goldLabel =             view.findViewById<TextView>(R.id.goldLabel)
        val silverLabel =           view.findViewById<TextView>(R.id.silverLabel)
        val fleetSize =             view.findViewById<TextView>(R.id.fleetSize)
        val totalSPM =              view.findViewById<TextView>(R.id.totalSPM)
        val netWorth =              view.findViewById<TextView>(R.id.netWorth)
        val totalVoyages =          view.findViewById<TextView>(R.id.totalVoyages)
        val topEarningImage =       view.findViewById<ImageView>(R.id.topEarningImage)
        val topEarningName =        view.findViewById<TextView>(R.id.topEarningName)
        val topEarningValue =       view.findViewById<TextView>(R.id.topEarningValue)
        val spmName =               view.findViewById<TextView>(R.id.spmName)
        val spmImage =              view.findViewById<ImageView>(R.id.spmImage)
        val spmValue =              view.findViewById<TextView>(R.id.spmValue)
        val mostVoyagesName =       view.findViewById<TextView>(R.id.mostVoyagesName)
        val distanceSailed =        view.findViewById<TextView>(R.id.distanceSailed)
        val mostVoyagesImage =      view.findViewById<ImageView>(R.id.mostVoyagesImage)
        val mostVoyagesValue =      view.findViewById<TextView>(R.id.mostVoyagesValue)
        val mostMileageName =       view.findViewById<TextView>(R.id.mostMileageName)
        val mostMileageValue =      view.findViewById<TextView>(R.id.mostMileageValue)
        val mostMileageImage =      view.findViewById<ImageView>(R.id.mostMileageImage)
        val mostGoodsSoldSilver =   view.findViewById<TextView>(R.id.mostGoodsSoldSilver)
        val mostGoodsSoldLabel =    view.findViewById<TextView>(R.id.mostGoodsSoldLabel)
        val mostFrequentedValue =   view.findViewById<TextView>(R.id.mostFrequentedValue)
        val mostFrequentedName =    view.findViewById<TextView>(R.id.mostFrequentedName)
        val mostGoodsBoughtLabel =  view.findViewById<TextView>(R.id.mostGoodsBoughtLabel)
        val mostGoodsBoughtSilver = view.findViewById<TextView>(R.id.mostGoodsBoughtSilver)
        val boatsSold =             view.findViewById<TextView>(R.id.boatsSold)

        val user = User.instance

        val rankInfo = User.rankValues[User.rankKeys[user.level]]!!
        levelLabel.text = rankInfo[1].toString()
        levelImage.setImageResource(
            context!!.resources.getIdentifier(
                rankInfo[0].toString(),
                "drawable",
                context!!.getPackageName()
            )
        )
        nextXpLabel.text =      (user.xpForLevel(user.level + 1) - user.xp).toString()
        totalXpLabel.text =     user.xp.toString()
        goldLabel.text =        user.gold.toString()
        silverLabel.text =      user.silver.toString()
        fleetSize.text =        user.boats.size.toString()
        boatsSold.text =        user.boatsSold.toString()
        totalSPM.text =         String.format("%.2f", max(0.0, user.silver / Date().time - user.startDate.time / 60.0 * 10))
        totalVoyages.text =     user.voyages.toString()
        distanceSailed.text =   String.format("%.0fkm", user.distance / 10)
        var worth = user.silver
        for (boat in user.boats) {
            worth += boat.value
        }
        netWorth.text = worth.toString()
        var mostBought: TownModel? = null
        var mostSold: TownModel? = null
        var mostFrequented: TownModel? = null
        for (town in Map.instance.towns) {
            if (town.level == 0) {
                continue
            }
            if (mostBought == null || town.startSilver > mostBought.startSilver) {
                mostBought = town
            }
            if (mostSold == null || town.endSilver > mostSold.endSilver) {
                mostSold = town
            }
            if (mostFrequented == null || town.totalVisits > mostFrequented.totalVisits) {
                mostFrequented = town
            }
        }
        if (mostBought != null) {
            mostGoodsBoughtLabel.text = mostBought.name
            mostGoodsBoughtSilver.text = mostBought.startSilver.toString()
        }
        if (mostSold != null) {
            mostGoodsSoldLabel.text = mostSold.name
            mostGoodsSoldSilver.text = mostSold.endSilver.toString()
        }
        if (mostFrequented != null) {
            mostFrequentedName.text = mostFrequented.name
            mostFrequentedValue.text = mostFrequented.totalVisits.toString()
        }

        data class BoatStatView (val name: TextView, val value: TextView, val image: ImageView)
        var boatViews = arrayOf<BoatStatView>(
            BoatStatView(mostMileageName, mostMileageValue, mostMileageImage),
            BoatStatView(spmName, spmValue, spmImage),
            BoatStatView(topEarningName, topEarningValue, topEarningImage) ,
            BoatStatView(mostVoyagesName, mostVoyagesValue, mostVoyagesImage)
        )
        for (idx in 0 until User.boatStatInfo.size) {
            val boatStatInfo = User.boatStatInfo[idx]
            val view = boatViews[idx]
            val stat = user.getStat(boatStatInfo.statName)
            if (stat != null) {
                view.name.text = stat.second.name
                view.value.text = boatStatInfo.statString(stat.second)
                view.image.setImageResource(
                    context!!.resources.getIdentifier(
                        stat.first.boatData.type + "_01",
                        "drawable",
                        context!!.getPackageName()
                    )
                )
                view.image.visibility = View.VISIBLE
            } else {
                view.name.text = "---"
                view.value.text = "---"
                view.image.visibility = View.INVISIBLE
            }
        }
    }

    override fun statsUpdated () {
        update(view)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_stats, container, false)
        val stats = view.findViewById<TextView>(R.id.statistics)
        stats.setText(R.string.statistics)
        User.instance.addObserver(this)
        update(view)
        return view
    }
}