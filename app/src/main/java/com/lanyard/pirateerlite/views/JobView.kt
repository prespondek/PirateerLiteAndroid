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

package com.lanyard.pirateerlite.views

import android.content.Context
import android.graphics.Color
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

                val res = context.resources.getIdentifier(element[1], "drawable", context.packageName)
                jobImage.setImageDrawable(context.resources.getDrawable(res,null))
                destinationLabel.text = field!!.destination.name
                costLabel.text = field!!.value.toString()
                if (field!!.isGold) {
                    val res = context.resources.getIdentifier("gold_piece", "drawable", context.packageName)
                    currencyIcon.setImageDrawable(context.resources.getDrawable(res,null))
                } else {
                    val res = context.resources.getIdentifier("silver_piece", "drawable", context.packageName)
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