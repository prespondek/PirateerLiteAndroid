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
import android.os.CountDownTimer
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.lanyard.pirateerlite.MapActivity
import com.lanyard.pirateerlite.R
import com.lanyard.pirateerlite.models.BoatModel
import com.lanyard.pirateerlite.singletons.User
import com.lanyard.pirateerlite.singletons.User.UserListener
import kotlinx.coroutines.*
import java.lang.Runnable
import java.util.*
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

class MarketFragment : AppFragment() , UserListener {
    private lateinit var _parts: ArrayList<User.BoatPart>
    private lateinit var _marketTimeStamp: Date
    private lateinit var _marketView: androidx.recyclerview.widget.RecyclerView
    private lateinit var _adapter: MarketAdapter
    //private lateinit var _marketThread :    Thread
    private lateinit var _marketTimer: CountDownTimer
    var selectedPart: User.BoatPart? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        User.instance.addListerner(this)
        _parts = User.instance.market
        _marketTimeStamp = User.instance.marketDate
        _adapter = MarketAdapter()

        val view = inflater.inflate(R.layout.fragment_market, container, false)
        _marketView = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.marketList).apply {
            setHasFixedSize(true)
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(activity)
            adapter = _adapter
        }
        var swipe = view.findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.marketRefresh);
        swipe.setOnRefreshListener {
            reloadMarket()
            swipe.isRefreshing = false
        }
        resetTimer()
        return view
    }

    fun resetTimer() {
        _marketTimer = object : CountDownTimer(getRemainingMarketTime(), 1000) {
            override fun onFinish() {
                updateMarketTimer(0)
            }

            override fun onTick(millisUntilFinished: Long) {
                updateMarketTimer(millisUntilFinished)
            }
        }
        _marketTimer.start()
    }

    fun getRemainingMarketTime(): Long {
        return User.marketInterval - (Date().time - _marketTimeStamp.time)
    }

    override fun onDetach() {
        super.onDetach()
        //_marketThread.interrupt()
    }

    override fun goldUpdated(oldValue: Int, newValue: Int) {
        moneyUpdated()
    }

    override fun silverUpdated(oldValue: Int, newValue: Int) {
        moneyUpdated()
    }

    private fun moneyUpdated() {
        for (i in 1 until _adapter.itemCount) {
            var cell = _marketView.findViewHolderForLayoutPosition(i)
            if (cell != null) {
                if (cell.itemView.isInTouchMode) {
                    continue
                }
                updateCell(cell as MarketAdapter.MarketViewHolder, i)
            }
        }
    }

    private fun reloadMarket() {
        if (getRemainingMarketTime() <= 0L) {
            _parts = User.instance.market
            _marketTimeStamp = User.instance.marketDate
            _adapter.notifyItemRangeChanged(1, _parts.size)
            resetTimer()
        }
    }

    private fun updateCell(p0: MarketAdapter.MarketViewHolder, p1: Int) {
        if (p1 == 0 || _parts.isEmpty()) return

        var moneyIcon = p0.view.findViewById<ImageView>(R.id.partMoney)
        var moneyLabel = p0.view.findViewById<TextView>(R.id.partCost)
        var boatName = p0.view.findViewById<TextView>(R.id.partName)
        var boatImage = p0.view.findViewById<ImageView>(R.id.partIcon)

        val boatPart = _parts[p1 - 1]

        val boatInfo = BoatModel.boatValues[boatPart.boat]!!
        var cost = 0
        if (boatPart.item.index < 4) {
            val partInfo = BoatModel.boatParts[boatPart.item.index]
            boatImage.setImageResource(
                context!!.resources.getIdentifier(
                    partInfo[1],
                    "drawable",
                    context!!.getPackageName()
                )
            )
            boatName.text = boatInfo[BoatModel.BoatIndex.title.index].toString() + " " + partInfo[0]
            cost = (boatInfo[BoatModel.BoatIndex.part_cost.index] as ArrayList<Double>)[boatPart.item.index].toInt()
            moneyLabel.text = cost.toString()
        } else {
            boatImage.setImageResource(
                context!!.resources.getIdentifier(
                    boatPart.boat + "_01",
                    "drawable",
                    context!!.getPackageName()
                )
            )
            boatName.text = boatInfo[BoatModel.BoatIndex.title.index].toString()
            cost = boatInfo[BoatModel.BoatIndex.boat_cost.index] as Int
            moneyLabel.text = cost.toString()
        }
        if (cost <= User.instance.gold) {
            p0.view.isClickable = true
            p0.view.background = context!!.resources.getDrawable(R.color.background_material_light, null)
            boatImage.alpha = 1.0f
            boatName.alpha = 1.0f
        } else {
            p0.view.isClickable = false
            p0.view.background = context!!.resources.getDrawable(R.color.material_grey_100, null)
            boatImage.alpha = 0.5f
            boatName.alpha = 0.5f
        }
    }

    private fun updateMarketTimer(millisUntilFinished: Long) {
        var holder = _marketView.findViewHolderForLayoutPosition(0)
        if (holder == null) {
            return
        }
        var label = holder.itemView.findViewById<TextView>(R.id.jobTimer)
        if (millisUntilFinished > 0) {
            var secs = millisUntilFinished / 1000
            val mins = secs / 60
            secs -= mins * 60
            label.text = String.format("New stock in %d.%d minutes", mins, secs)
        } else {
            label.text = "New stock available"
        }
    }

    inner class MarketAdapter() : androidx.recyclerview.widget.RecyclerView.Adapter<MarketAdapter.MarketViewHolder>() {
        inner class MarketViewHolder(val view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
            init {
                view.setOnClickListener {
                    var user = User.instance
                    selectedPart = User.instance.market[layoutPosition - 1]
                    _parts.remove(selectedPart!!)
                    User.instance.parts.add(selectedPart!!)
                    val boatInfo = BoatModel.boatValues[selectedPart!!.boat]!!
                    User.instance.addMoney(-(boatInfo[BoatModel.BoatIndex.part_cost.index] as ArrayList<Int>)[selectedPart!!.item.index], 0)
                    User.instance.save()
                    _adapter.notifyDataSetChanged()
                }
            }
        }

        override fun getItemCount(): Int {
            return max(2,_parts.size + 1)
        }

        override fun getItemViewType(position: Int): Int {
            if (position == 0) return 0
            return 1
        }

        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): MarketAdapter.MarketViewHolder {
            var view: View? = null
            if (p1 == 0) {
                view = LayoutInflater.from(context).inflate(R.layout.cell_job_header, p0, false)
            } else if (_parts.isEmpty()) {
                view = LayoutInflater.from(context).inflate(R.layout.cell_market_empty, p0, false)
            } else {
                view = LayoutInflater.from(context).inflate(R.layout.cell_market, p0, false)
            }
            return MarketViewHolder(view!!)
        }

        override fun onBindViewHolder(p0: MarketAdapter.MarketViewHolder, p1: Int) {
            if (p1 == 0) {
                var headerLabel = p0.view.findViewById<TextView>(R.id.jobLabel)
                headerLabel.setText(context!!.resources.getText(R.string.market))
                updateMarketTimer(getRemainingMarketTime())
            } else {
                updateCell(p0, p1)
            }
        }
    }

}