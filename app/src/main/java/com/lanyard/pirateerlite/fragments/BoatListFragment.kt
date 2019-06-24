package com.lanyard.pirateerlite.fragments

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.lanyard.canvas.BitmapCache
import com.lanyard.pirateerlite.MapActivity
import com.lanyard.pirateerlite.models.BoatModel
import com.lanyard.pirateerlite.singletons.User
import com.lanyard.pirateeronline.R
import kotlinx.android.synthetic.main.activity_map.*


class BoatListFragment : Fragment() {

    inner class BoatListAdapter(private val boats: List<BoatModel>) : RecyclerView.Adapter<BoatListAdapter.BoatListViewHolder>() {

        inner class BoatListViewHolder(val view: View ) : RecyclerView.ViewHolder(view), View.OnClickListener {
            init {
                view.setOnClickListener(this)
            }

            override fun onClick(view: View) {
                onBoatSelected(layoutPosition)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup,
                                        viewType: Int): BoatListAdapter.BoatListViewHolder {
            val textView = LayoutInflater.from(parent.context)
                .inflate(R.layout.cell_boat, parent, false)

            return BoatListViewHolder(textView)
        }

        // Replace the contents of a view (invoked by the layout manager)
        override fun onBindViewHolder(holder: BoatListViewHolder, position: Int) {

            holder.view.findViewById<TextView>(R.id.boatName)!!.text = boats[position].name
            if (boats[position].isMoored) {
                holder.view.findViewById<TextView>(R.id.boatStatus)!!.text = "Moored at " + boats[position].town!!.name
            } else {
                holder.view.findViewById<TextView>(R.id.boatStatus)!!.text = "Sailing to " + boats[position].destination!!.name
            }
            var boatImg = BitmapCache.instance.getBitmap(boats[position].type + "_01.png")!!
            boatImg.inflate()
            holder.view.findViewById<ImageView>(R.id.boatImg)!!.setImageBitmap(boatImg.bitmap)
        }

        override fun getItemCount() = boats.size
    }

    private lateinit var _table : RecyclerView
    private lateinit var _viewAdapter: RecyclerView.Adapter<*>
    private lateinit var _viewManager: RecyclerView.LayoutManager

    fun onBoatSelected( index: Int ) {
        val map_activity = (activity as MapActivity)
        val map = map_activity.supportFragmentManager.findFragmentByTag("map") as MapFragment
        map.boatSelected(index)
        map_activity.navigation.selectedItemId = R.id.navigation_map
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _viewManager = LinearLayoutManager(activity)
        _viewAdapter = BoatListAdapter(User.sharedInstance.boats)

        val view =  inflater.inflate(R.layout.fragment_boats, container, false)
        _table = view.findViewById<RecyclerView>(R.id.boatTable).apply {
            setHasFixedSize(true)
            layoutManager = _viewManager
            adapter = _viewAdapter

        }
        return view
    }
}