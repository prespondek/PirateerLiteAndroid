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
import com.lanyard.pirateerlite.models.BoatModel


class BoatListFragment() : androidx.fragment.app.Fragment() {
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
    class BoatSellFragment(val boat: BoatModel, val cell: SwipeToSellCallback, val position: Int): androidx.fragment.app.DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            return activity?.let {
                // Use the Builder class for convenient dialog construction
                var user = User.instance
                val builder = AlertDialog.Builder(it)
                    builder.setMessage("Sell boat for " + boat.value * User.exchangeRate + " silver?")
                        .setPositiveButton(R.string.yes, { dialog, id ->
                            user.addMoney(0, boat.value * User.exchangeRate)
                            user.removeBoat(boat)
                            user.save()
                            cell.commit(position)
                        })
                        .setNegativeButton(R.string.no, { dialog, id ->
                            cell.cancel(position)
                        })

                builder.create()
            } ?: throw IllegalStateException("Activity cannot be null")
        }
    }
    inner class SwipeToSellCallback : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
        private val icon : Drawable
        private val background = ColorDrawable()

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

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.adapterPosition
            var dialog = BoatSellFragment(User.instance.boatAtIndex(viewHolder.layoutPosition)!!, this, viewHolder.adapterPosition)
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
            isCurrentlyActive: Boolean
        ) {
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

    private lateinit var _table : androidx.recyclerview.widget.RecyclerView
    private lateinit var _viewAdapter: androidx.recyclerview.widget.RecyclerView.Adapter<*>
    private lateinit var _viewManager: androidx.recyclerview.widget.RecyclerView.LayoutManager


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _viewManager = androidx.recyclerview.widget.LinearLayoutManager(activity)
        _viewAdapter = BoatListAdapter()

        val view =  inflater.inflate(R.layout.fragment_boats, container, false)
        _table = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.boatTable).apply {
            setHasFixedSize(true)
            layoutManager = _viewManager
            adapter = _viewAdapter
        }
        val swipeHandler = SwipeToSellCallback()
        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(_table)
        return view
    }
}