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
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.lanyard.pirateerlite.R

class MenuCellView : FrameLayout
{

    constructor(context: Context, attrs: AttributeSet?) : super(context,attrs) {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.MenuCellView,
            0, 0).apply {
            try {
                _imgres = getDrawable(R.styleable.MenuCellView_image)
                _txtres = getString(R.styleable.MenuCellView_label)
            } finally {
                recycle()
            }
        }
        var rootView = inflate(context, R.layout.cell_menu, this)
        _label = rootView.findViewById<TextView>(R.id.cellMenuLabel)
        _image = rootView.findViewById<ImageView>(R.id.cellMenuImage)
        _label.text = _txtres
        _image.setImageDrawable(_imgres)
    }


    lateinit var _label : TextView
    lateinit var _image : ImageView
    var _txtres : String? = null
    var _imgres : Drawable? = null

    init {
        //var rootView = inflate(context, R.layout.cell_menu, this);
        //_label = rootView.findViewById<TextView>(R.id.cellMenuLabel);
        //_image = rootView.findViewById<ImageView>(R.id.cellMenuImage);
        //_label.text = _txtres
        //_image.setImageDrawable(_imgres)
    }

}