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
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.lanyard.pirateerlite.MapActivity
import com.lanyard.pirateerlite.R
import com.lanyard.pirateerlite.models.BoatModel
import com.lanyard.pirateerlite.singletons.User
import kotlinx.android.synthetic.main.activity_map.*

class ShipyardFragment : AppFragment()  {
    inner class ShipyardAdapter() : androidx.recyclerview.widget.RecyclerView.Adapter<ShipyardFragment.ShipyardAdapter.ShipyardViewHolder>() {

        override fun onBindViewHolder(p0: ShipyardViewHolder, p1: Int) {
            var syRank = p0.view.findViewById<ImageView>(R.id.syRank)!!
            var syBoat = p0.view.findViewById<ImageView>(R.id.syBoat)!!
            var syName = p0.view.findViewById<TextView>(R.id.syName)!!
            var syTick = p0.view.findViewById<ImageView>(R.id.syTick)!!

            val name = BoatModel.boatKeys[p1]
            val data = BoatModel.boatValues[name]!!
            val rank = data[15] as String
            syRank.setImageResource(context!!.resources.getIdentifier(User.rankValues[rank]!![0] as String, "drawable", context!!.getPackageName()))
            syBoat.setImageResource(context!!.resources.getIdentifier(name + "_01","drawable", context!!.getPackageName()))
            syName.text = data[13] as? String
            if (User.instance.level < User.rankKeys.indexOfFirst { it == rank }) {
                p0.view.isClickable = false
                syName.isEnabled = false
                p0.view.background = context!!.resources.getDrawable(R.color.material_grey_100,null)
                syBoat.alpha = 0.5f
                syRank.alpha = 0.5f
                syTick.visibility = INVISIBLE
            } else {
                p0.view.isClickable = true
                syName.isEnabled = true
                p0.view.background = context!!.resources.getDrawable(R.color.background_material_light,null)
                syBoat.alpha = 1.0f
                syRank.alpha = 1.0f
                syTick.visibility = VISIBLE
            }
            if (User.instance.canBuildBoat(name)) {
                syTick.visibility = VISIBLE
            } else {
                syTick.visibility = INVISIBLE
            }
        }

        override fun getItemCount(): Int {
            return BoatModel.boatKeys.size
        }

        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): ShipyardViewHolder {
            val textView = LayoutInflater.from(context)
                .inflate(R.layout.cell_shipyard, p0, false)

            return ShipyardViewHolder(textView)
        }

        inner class ShipyardViewHolder(val view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
            init {
                view.setOnClickListener(View.OnClickListener {
                    var user = User.instance
                    var frag = (activity as MapActivity).swapFragment(R.layout.cell_shipyard) as BoatInfoFragment
                    frag.boatType = BoatModel.boatKeys[layoutPosition]
                })
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view =  inflater.inflate(R.layout.fragment_shipyard, container, false)
        view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.syTable).apply {
            setHasFixedSize(true)
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(activity)
            adapter = ShipyardAdapter()
        }
        return view
    }
}