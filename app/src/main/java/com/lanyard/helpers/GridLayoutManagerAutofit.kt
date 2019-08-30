package com.lanyard.helpers

import android.content.Context
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import android.util.TypedValue
import androidx.recyclerview.widget.RecyclerView

class GridLayoutManagerAutofit  : androidx.recyclerview.widget.GridLayoutManager {
    private var _columnWidth: Int = 0
    private var _columnWidthChanged = true

    constructor(context: Context, columnWidth: Int) : super(context,1) {
        /* Initially set spanCount to 1, will be changed automatically later. */
        setColumnWidth(checkedColumnWidth(context, columnWidth))
    }

    constructor(context: Context, columnWidth: Int, orientation: Int, reverseLayout: Boolean) : super(context,columnWidth,orientation,reverseLayout){
        /* Initially set spanCount to 1, will be changed automatically later. */
        setColumnWidth(checkedColumnWidth(context, columnWidth))
    }

    private fun checkedColumnWidth(context: Context, columnWidth: Int): Int {
        var columnWidth = columnWidth
        if (columnWidth <= 0) {
            /* Set default columnWidth value (48dp here). It is better to move this constant
            to static constant on top, but we need context to convert it to dp, so can't really
            do so. */
            columnWidth = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 48f,
                context.getResources().getDisplayMetrics()
            ).toInt()
        }
        return columnWidth
    }

    fun setColumnWidth(newColumnWidth: Int) {
        if (newColumnWidth > 0 && newColumnWidth != _columnWidth) {
            _columnWidth = newColumnWidth
            _columnWidthChanged = true
        }
    }

    override fun onLayoutChildren(recycler: androidx.recyclerview.widget.RecyclerView.Recycler?, state: androidx.recyclerview.widget.RecyclerView.State) {
        val width = width
        val height = height
        if (_columnWidthChanged && _columnWidth > 0 && width > 0 && height > 0) {
            val totalSpace: Int
            if (orientation == androidx.recyclerview.widget.LinearLayoutManager.VERTICAL) {
                totalSpace = width - paddingRight - paddingLeft
            } else {
                totalSpace = height - paddingTop - paddingBottom
            }
            val spanCount = Math.max(1, totalSpace / _columnWidth)
            setSpanCount(spanCount)
            _columnWidthChanged = false
        }
        super.onLayoutChildren(recycler, state)
    }


}