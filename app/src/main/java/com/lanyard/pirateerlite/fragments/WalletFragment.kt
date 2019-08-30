package com.lanyard.pirateerlite.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.lanyard.pirateerlite.R
import com.lanyard.pirateerlite.singletons.User
import com.lanyard.pirateerlite.singletons.User.UserObserver

class WalletFragment : androidx.fragment.app.Fragment(), UserObserver
{
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment

        var view = inflater.inflate(R.layout.fragment_wallet, container, false)
        _goldLabel = view.findViewById<TextView>(R.id.goldText)
        _silverLabel = view.findViewById<TextView>(R.id.silverText)
        _xpLabel = view.findViewById<TextView>(R.id.xpText)
        _xpImage = view.findViewById<ImageView>(R.id.xpIcon)
        User.instance.addObserver(this)
        goldUpdated( 0,  0)
        silverUpdated( 0,  0)
        xpUpdated( 0,  0)
        return view
    }

    override fun goldUpdated(oldValue: Int, newValue: Int) {
        _goldLabel.text = User.instance.gold.toString()
    }

    override fun silverUpdated(oldValue: Int, newValue: Int) {
        _silverLabel.text = User.instance.silver.toString()
    }

    override fun xpUpdated(oldValue: Int, newValue: Int) {
        val user = User.instance
        var levelXp = user.xpForLevel(user.level)
        var xp = user.xp
        _xpLabel.text = ( levelXp - xp ).toString()
        val level_image = (User.rankValues[User.rankKeys[user.level]]!![0]).toString()
        val id = activity?.resources?.getIdentifier(level_image,"drawable",context?.getPackageName())
        _xpImage.setImageDrawable(activity!!.resources.getDrawable(id!!,null))
    }


    private lateinit var _silverLabel : TextView
    private lateinit var _goldLabel : TextView
    private lateinit var _xpLabel: TextView
    private lateinit var _xpImage: ImageView
}