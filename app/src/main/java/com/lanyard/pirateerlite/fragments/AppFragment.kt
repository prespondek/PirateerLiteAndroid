package com.lanyard.pirateerlite.fragments

import android.view.View.GONE
import android.view.animation.Animation
import android.view.animation.AnimationUtils.loadAnimation
import com.lanyard.pirateerlite.R


open class AppFragment : androidx.fragment.app.Fragment() {
    companion object {
        var blockAnimation : Boolean = false
    }

    override fun onCreateAnimation(transit: Int, enter: Boolean, nextAnim: Int): Animation? {
        var anim = super.onCreateAnimation(transit, enter, nextAnim)
        if (blockAnimation) {
            anim = null
            view?.visibility = GONE
        } else if (enter) {
            anim = loadAnimation(context, R.anim.abc_popup_enter)
        } else {
            anim = loadAnimation(context, R.anim.abc_popup_exit)
        }
        return anim
    }
}