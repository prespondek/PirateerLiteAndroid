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
import android.graphics.*
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.ItemTouchHelper
import com.lanyard.pirateerlite.MapActivity
import com.lanyard.pirateerlite.R
import com.lanyard.pirateerlite.singletons.User
import kotlinx.android.synthetic.main.activity_map.*
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat

class BoatListFragment() : androidx.fragment.app.Fragment() {
    class BoatSellFragment(): androidx.fragment.app.DialogFragment() {

        constructor(position: Int) : this() {
            _position = position
        }
        private var _position: Int = 0

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            if ( savedInstanceState != null ) {
                _position = savedInstanceState.getInt("position") }
            return activity?.let {
                // Use the Builder class for convenient dialog construction
                var user = User.instance
                var boat = user.boats[_position]
                var cell = (fragmentManager?.findFragmentByTag("boats") as BoatListFragment).swipeCallback
                val builder = AlertDialog.Builder(it)
                builder.setMessage("Sell boat for " + boat.value * User.exchangeRate + " silver?")
                    .setPositiveButton(com.lanyard.pirateerlite.R.string.yes, { dialog, id ->
                        user.addMoney(0, boat.value * User.exchangeRate)
                        user.removeBoat(boat)
                        user.save()
                        cell.commit(_position)
                    })
                    .setNegativeButton(R.string.no, { dialog, id ->
                        cell.cancel(_position)
                    })

                builder.create()
            } ?: throw IllegalStateException("Activity cannot be null")
        }

        override fun onSaveInstanceState(outState: Bundle) {
            super.onSaveInstanceState(outState)
            outState.putInt("position",_position)
        }
    }

    class FleetDialogFragment : androidx.fragment.app.DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            return activity?.let {
                // Use the Builder class for convenient dialog construction
                var user = User.instance
                val builder = AlertDialog.Builder(it)
                if (user.silver >= user.boatSlotCost) {
                    builder.setMessage("Buy extra boat slot for " + user.boatSlotCost.toString() + " silver")
                        .setPositiveButton(R.string.yes, { dialog, id ->
                            user.addMoney(0, -user.boatSlotCost)
                            user.boatSlots += 1
                            user.save()
                        })
                        .setNegativeButton(R.string.no, { dialog, id -> })
                } else {
                    builder.setMessage(R.string.expandNo)
                        .setNegativeButton(R.string.ok, { dialog, id -> })
                }
                builder.create()
            } ?: throw IllegalStateException("Activity cannot be null")
        }
    }

    inner class SwipeToSellCallback : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
        val icon : Drawable
        val background = ColorDrawable()

        init {
            icon = ContextCompat.getDrawable(context!!, R.drawable.ic_local_atm_black_24dp)!!
            icon.setTint(Color.parseColor("#ffffffff"))
            icon.setTintMode(PorterDuff.Mode.SRC_ATOP)
            background.color = Color.parseColor("#ff009900")
        }


        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            return false
        }

        override fun getSwipeDirs(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
            val boat = User.instance.boats[viewHolder.layoutPosition]
            if ( !boat.isMoored ) {
                return 0
            } else {
                return super.getSwipeDirs(recyclerView, viewHolder)
            }
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.adapterPosition
            var dialog = BoatSellFragment(viewHolder.adapterPosition)
            dialog.show(fragmentManager,"sell")
        }

        override fun isItemViewSwipeEnabled(): Boolean {
            return User.instance.boats.size > 1
        }

        fun commit (position : Int) {
            _viewAdapter.notifyItemRemoved(position)
            _viewAdapter.notifyItemChanged(_viewAdapter.itemCount - 2)
        }

        fun cancel (position : Int) {
            _viewAdapter.notifyItemChanged(position)
        }

        override fun onChildDraw(
            canvas: Canvas,
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            dX: Float,
            dY: Float,
            actionState: Int,
            isCurrentlyActive: Boolean)
        {
            super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            var itemView = viewHolder.itemView
            val backgroundCornerOffset = 20
            val iconMargin = (itemView.getHeight() - icon.getIntrinsicHeight()) / 2
            val iconTop = itemView.getTop() + (itemView.getHeight() - icon.getIntrinsicHeight()) / 2
            val iconBottom = iconTop + icon.getIntrinsicHeight()
            if (dX > 0) {
                val iconLeft = itemView.getLeft() + iconMargin + icon.getIntrinsicWidth()
                val iconRight = itemView.getLeft() + iconMargin
                icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                background.setBounds(itemView.getLeft(), itemView.getTop(),
                    itemView.getLeft() + dX.toInt() + backgroundCornerOffset, itemView.getBottom())
            } else if (dX < 0) {
                val iconLeft = itemView.getRight() - iconMargin - icon.getIntrinsicWidth()
                val iconRight = itemView.getRight() - iconMargin
                icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                background.setBounds(itemView.getRight() + dX.toInt() - backgroundCornerOffset,
                    itemView.getTop(), itemView.getRight(), itemView.getBottom())
            } else {
                background.setBounds(0, 0, 0, 0)
                icon.setBounds(0,0,0,0)
            }
            background.draw(canvas)
            icon.draw(canvas)
        }
    }

    inner class BoatListAdapter() : androidx.recyclerview.widget.RecyclerView.Adapter<BoatListAdapter.BoatListViewHolder>() {

        inner class BoatListViewHolder(val view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
            init {
                view.setOnClickListener(View.OnClickListener {
                    var user = User.instance

                    if (this.layoutPosition < user.numBoats) {
                        val map_activity = (activity as MapActivity)
                        val map = map_activity.supportFragmentManager.findFragmentByTag("map") as MapFragment
                        map.boatSelected(this.layoutPosition)
                        map_activity.navigation.selectedItemId = R.id.navigation_map
                    } else if (this.layoutPosition < user.boatSlots) {
                    } else {
                        var dialog = FleetDialogFragment()
                        dialog.show(fragmentManager,"expand")
                    }
                })
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup,
                                        viewType: Int): BoatListViewHolder {
            val textView = LayoutInflater.from(parent.context)
                .inflate(R.layout.cell_boat, parent, false)


            return BoatListViewHolder(textView)
        }

        // Replace the contents of a view (invoked by the layout manager)
        override fun onBindViewHolder(holder: BoatListViewHolder, position: Int) {
            var user = User.instance
            var moorframe = holder.view.findViewById<LinearLayout>(R.id.moorFrame)!!
            var boatframe = holder.view.findViewById<LinearLayout>(R.id.boatFrame)!!
            var emptyLabel = holder.view.findViewById<TextView>(R.id.emptyLabel)!!

            if (position < user.boats.size) {
                boatframe.visibility = VISIBLE
                moorframe.visibility = INVISIBLE
                emptyLabel.visibility = INVISIBLE
                holder.view.findViewById<TextView>(R.id.boatName)!!.text = user.boats[position].name
                if (user.boats[position].isMoored) {
                    holder.view.findViewById<TextView>(R.id.boatStatus)!!.text =
                        "Moored at " + user.boats[position].town!!.name
                } else {
                    holder.view.findViewById<TextView>(R.id.boatStatus)!!.text =
                        "Sailing to " + user.boats[position].destination!!.name
                }
                holder.view.findViewById<ImageView>(R.id.boatImg)!!.setImageResource(
                    context!!.resources.getIdentifier(user.boats[position].type + "_01", "drawable", context!!.getPackageName())
                )
            } else if (position < user.boatSlots) {
                boatframe.visibility = INVISIBLE
                moorframe.visibility = INVISIBLE
                emptyLabel.visibility = VISIBLE
            } else {
                boatframe.visibility = INVISIBLE
                moorframe.visibility = VISIBLE
                emptyLabel.visibility = INVISIBLE
                var cost = holder.view.findViewById<TextView>(R.id.mooringCost)
                cost.text = user.boatSlotCost.toString()
            }

        }

        override fun getItemCount() = User.instance.boatSlots + 1
    }

    private lateinit var _table : RecyclerView
    private lateinit var _viewAdapter : RecyclerView.Adapter<*>
    private lateinit var _viewManager : RecyclerView.LayoutManager
    lateinit var swipeCallback : SwipeToSellCallback

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_boats, container, false)
        _viewManager = androidx.recyclerview.widget.LinearLayoutManager(activity)
        _viewAdapter = BoatListAdapter()
        _table = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.boatTable).apply {
            setHasFixedSize(true)
            layoutManager = _viewManager
            adapter = _viewAdapter
        }
        swipeCallback = SwipeToSellCallback()
        ItemTouchHelper(swipeCallback).attachToRecyclerView(_table)
        return view
    }

}