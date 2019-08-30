package com.lanyard.pirateerlite.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.cardview.widget.CardView
import android.view.LayoutInflater
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.lanyard.pirateerlite.MapActivity
import com.lanyard.pirateerlite.R
import com.lanyard.pirateerlite.models.BoatModel
import com.lanyard.pirateerlite.singletons.User
import com.lanyard.pirateerlite.singletons.User.UserObserver
import com.lanyard.pirateerlite.views.MenuCellView

class MenuFragment : androidx.fragment.app.Fragment(), UserObserver {


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        postponeEnterTransition()

        // Inflate the layout for this fragment
        var view = inflater.inflate(R.layout.fragment_menu, container, false)

        User.instance.addObserver(this)
        var arr = arrayOf(R.id.shipyardButton,R.id.marketButton,R.id.statsButton,R.id.bankButton)
        for (idx in arr) {
            var cell = view.findViewById<MenuCellView>(idx)
            var click : View.OnClickListener?
            if (idx == R.id.bankButton) {
                click = View.OnClickListener {
                    User.instance.addMoney(-1, User.exchangeRate)
                    User.instance.save()
                }
            } else {
                click = View.OnClickListener {
                    (activity as MapActivity).swapFragment(idx)
                }
            }
            cell.findViewById<androidx.cardview.widget.CardView>(R.id.cv).setOnClickListener(click)
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        var exchangeLabel = view.findViewById<TextView>(R.id.exchangeLabel)
        goldUpdated(0, User.instance.gold)
        silverUpdated(0, User.instance.silver)
        xpUpdated(0, User.instance.xp)
        exchangeLabel.text = User.exchangeRate.toString()
        goldUpdated(0,User.instance.gold)
        startPostponedEnterTransition()
    }

    override fun goldUpdated(oldValue: Int, newValue: Int) {
        var goldLabel = view!!.findViewById<TextView>(R.id.goldLabel)
        var cell = view!!.findViewById<MenuCellView>(R.id.bankButton).findViewById<androidx.cardview.widget.CardView>(R.id.cv)
        goldLabel.text = newValue.toString()
        if (newValue > 0) {
            cell.isClickable = true
        } else {
            cell.isClickable = false
        }
    }

    override fun silverUpdated(oldValue: Int, newValue: Int) {
        var silverLabel = view!!.findViewById<TextView>(R.id.silverLabel)
        silverLabel.text = newValue.toString()
    }

    override fun xpUpdated(oldValue: Int, newValue: Int) {
        val user = User.instance
        var nextXPLabel = view!!.findViewById<TextView>(R.id.nextXPLabel)
        var totalXPLabel = view!!.findViewById<TextView>(R.id.totalXPLabel)
        var rankLabel = view!!.findViewById<TextView>(R.id.rankLabel)
        var rankImage = view!!.findViewById<ImageView>(R.id.rankImage)

        totalXPLabel.text = newValue.toString()
        nextXPLabel.text = (user.xpForLevel(user.level + 1) - user.xp ).toString()
        val level_image = User.rankValues[User.rankKeys[user.level]]!![0].toString()
        rankImage.setImageResource(context!!.resources.getIdentifier(level_image,"drawable", context!!.getPackageName()))
        rankLabel.text = User.rankValues[User.rankKeys[user.level]]!![1].toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        User.instance.removeObserver(this)
    }
}