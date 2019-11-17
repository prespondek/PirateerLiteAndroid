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

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.lanyard.pirateerlite.MapActivity
import com.lanyard.pirateerlite.R
import com.lanyard.pirateerlite.controllers.TownController
import com.lanyard.pirateerlite.models.TownModel
import com.lanyard.pirateerlite.singletons.User
import kotlinx.android.synthetic.main.activity_map.*

/**
 * Displays information about a town including vital statistics, cost and a description. Also contains a selectable
 * list of boats that are moored at the town.
 *
 * @author Peter Respondek
 *
 * @see R.layout.fragment_town
 */

class TownFragment : Fragment() {
    lateinit var townController: TownController

    inner class TownListAdapter :
        androidx.recyclerview.widget.RecyclerView.Adapter<TownListAdapter.TownListViewHolder>() {
        override fun onBindViewHolder(holder: TownFragment.TownListAdapter.TownListViewHolder, position: Int) {
            holder.view.findViewById<TextView>(R.id.townboatName)!!.text = townController.model.boats[position].name
            holder.view.findViewById<ImageView>(R.id.townboatImg)!!.setImageResource(
                context!!.resources.getIdentifier(
                    townController.model.boats[position].type + "_01",
                    "drawable",
                    context!!.packageName
                )
            )
        }

        inner class TownListViewHolder(val view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
            init {
                view.setOnClickListener(View.OnClickListener {
                    val map_activity = (activity as MapActivity)
                    val map = map_activity.supportFragmentManager.findFragmentByTag("map") as MapFragment
                    map.boatSelected(User.instance.boats.indexOfFirst { it === townController.model.boats[layoutPosition] })
                    map_activity.navigation.selectedItemId = R.id.navigation_map
                })
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TownListViewHolder {
            val textView = LayoutInflater.from(parent.context)
                .inflate(R.layout.cell_town, parent, false)

            return TownListViewHolder(textView)
        }

        override fun getItemCount() = townController.model.boats.size
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_town, container, false)
        val townLabel = view.findViewById<TextView>(R.id.townName)
        val description = view.findViewById<TextView>(R.id.description)
        val clazz = view.findViewById<TextView>(R.id.classLabel)
        val type = view.findViewById<TextView>(R.id.typeLabel)
        val image = view.findViewById<ImageView>(R.id.townPortrait)
        val upgradeButton = view.findViewById<Button>(R.id.upgradeButton)

        if ( savedInstanceState != null ) {
            val townId = savedInstanceState["townController"] as Long?
            if (townId != null) {
                val map = fragmentManager!!.findFragmentByTag("map") as MapFragment
                townController = map.townControllerForId(townId) ?: throw NullPointerException()
            }
        }

        upgradeButton.setOnClickListener { upgradeButtonPressed() }
        val res = context?.resources?.getIdentifier(townController.model.type.name, "drawable", context?.packageName)
        image.setImageResource(res!!)

        townLabel.text = townController.model.name
        description.text = townController.model.description
        clazz.text = townController.model.harbour.name.capitalize()
        type.text = townController.model.type.name.capitalize()
        val viewManager = androidx.recyclerview.widget.LinearLayoutManager(activity)
        val viewAdapter = TownListAdapter()

        if (townController.model.level > 0) {
            val act = activity as AppCompatActivity
            val actionBar = act.supportActionBar
            var menu = layoutInflater.inflate(R.layout.toolbar_jobs, null) as Toolbar
            act.invalidateOptionsMenu()
        }

        val table = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.boatList).apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
        }
        refresh(view)
        return view
    }

    override fun onDetach() {
        super.onDetach()
        val act = activity as AppCompatActivity
        act.invalidateOptionsMenu()
    }

    fun refresh (view : View) {
        val costLabel = view.findViewById<TextView>(R.id.costLabel)
        val sizeLabel = view.findViewById<TextView>(R.id.sizeLabel)
        val upgradeButton = view.findViewById<Button>(R.id.upgradeButton)
        sizeLabel.text = townController.model.level.toString()
        if (townController.model.level == 0) {
            upgradeButton.backgroundTintList =
                ContextCompat.getColorStateList(context!!, android.R.color.holo_red_light)
            upgradeButton.text = "Unlock"
        } else {
            upgradeButton.backgroundTintList =
                ContextCompat.getColorStateList(context!!, android.R.color.holo_blue_light)
            upgradeButton.text = "Upgrade"
        }
        val cost = getCost()
        costLabel.text = cost.toString()
        if (townController.model.level > TownModel.maxLevel) {
            upgradeButton.isEnabled = false
            costLabel.setTextColor(Color.YELLOW)
            costLabel.text = "Maximum"
        } else if (User.instance.silver < cost) {
            upgradeButton.backgroundTintList = ContextCompat.getColorStateList(context!!, android.R.color.darker_gray)
            upgradeButton.isEnabled = false
            costLabel.setTextColor(Color.RED)
        }
    }

    fun getCost( ) : Int {
        val cost : Int
        if (townController.model.level == 0) {
            cost = townController.model.purchaseCost
        } else {
            cost = townController.model.upgradeCost
        }
        return cost
    }

    fun upgradeButtonPressed() {
        User.instance.addMoney(0, -getCost())
        townController.model.level += 1
        townController.model.save()
        refresh(view!!)
        User.instance.save()
        townController.reset()
        val map = activity?.supportFragmentManager?.findFragmentByTag("map") as MapFragment
        map.townUpgraded()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong("townController", townController.model.id)
    }

}