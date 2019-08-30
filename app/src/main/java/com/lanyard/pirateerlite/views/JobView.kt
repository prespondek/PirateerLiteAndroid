package com.lanyard.pirateerlite.views

import android.content.Context
import android.graphics.Color
import androidx.cardview.widget.CardView
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.lanyard.pirateerlite.R
import com.lanyard.pirateerlite.controllers.JobController
import com.lanyard.pirateerlite.models.JobModel

class JobView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : androidx.cardview.widget.CardView(context, attrs, defStyleAttr) {
    var job : JobModel? = null
        set(value) {
            field = value
            val destinationLabel =  findViewById<TextView>      (R.id.townName)
            val jobImage =          findViewById<ImageView>     (R.id.jobImage)
            val costLabel =         findViewById<TextView>      (R.id.moneyAmount)
            val bonusLabel =        findViewById<TextView>      (R.id.bonusLabel)
            val currencyIcon =      findViewById<ImageView>     (R.id.moneyType)
            val jobStack =          findViewById<LinearLayout>  (R.id.jobStack)
            val emptyLabel =        findViewById<TextView>      (R.id.emptyLabel)
            val bgFrame =           findViewById<FrameLayout>   (R.id.bgFrame)

            if (value != null) {
                val element = JobController.jobData.first { it[0] == field!!.type }

                var res = context.resources.getIdentifier(element!![1],"drawable", context.getPackageName())
                jobImage.setImageDrawable(context.resources.getDrawable(res,null))
                destinationLabel.text = field!!.destination.name
                costLabel.text = field!!.value.toString()
                if (field!!.isGold) {
                    var res = context.resources.getIdentifier("gold_piece","drawable", context.getPackageName())
                    currencyIcon.setImageDrawable(context.resources.getDrawable(res,null))
                } else {
                    var res = context.resources.getIdentifier("silver_piece","drawable", context.getPackageName())
                    currencyIcon.setImageDrawable(context.resources.getDrawable(res,null))
                }
                jobStack.visibility = View.VISIBLE
                emptyLabel.visibility = View.INVISIBLE
                destinationLabel.setBackgroundColor(field!!.destination.color)
            } else {
                jobStack.visibility = View.INVISIBLE
                emptyLabel.visibility = View.VISIBLE
                bonusLabel.visibility = View.INVISIBLE
                destinationLabel.setBackgroundColor(Color.parseColor("#ffeeeeee"))
            }
        }

    fun bonus(enabled: Boolean) {
        var bonusLabel =    findViewById<TextView>      (R.id.bonusLabel)
        var bgFrame =       findViewById<FrameLayout>   (R.id.bgFrame)

        if (enabled) {
            bonusLabel.visibility = View.VISIBLE
            bgFrame.setBackgroundColor(Color.parseColor("#ff00ff00"))
        } else {
            bgFrame.setBackgroundColor(Color.parseColor("#ffeeeeee"))
            bonusLabel.visibility = View.INVISIBLE
        }
    }
}