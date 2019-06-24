package com.lanyard.pirateerlite.fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.lanyard.pirateeronline.R
import com.lanyard.pirateerlite.singletons.User

class WalletFragment : Fragment()
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
        return view
    }

    fun goldUpdated(oldValue: Int, newValue: Int) {
        _goldLabel.text = User.sharedInstance.gold.toString()
    }

    fun silverUpdated(oldValue: Int, newValue: Int) {
        _silverLabel.text = User.sharedInstance.silver.toString()
    }

    fun xpUpdated(oldValue: Int, newValue: Int) {
        val user = User.sharedInstance
        _xpLabel.text = (user.xpForLevel(user.level) - user.xp).toString()
        val level_image = (User.rankValues[User.rankKeys[user.level]]!![0]).toString()
        val id = activity!!.resources.getIdentifier(level_image,"drawable","R" )
        _xpImage.setImageDrawable(activity!!.resources.getDrawable(id,null))
    }


    private lateinit var _silverLabel : TextView
    private lateinit var _goldLabel : TextView
    private lateinit var _xpLabel: TextView
    private lateinit var _xpImage: ImageView
}