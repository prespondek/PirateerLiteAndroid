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