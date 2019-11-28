package com.lanyard.pirateerlite.viewmodels

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import com.lanyard.helpers.popLast

/**
 * ViewModel for MapActivity. Contains bare bones replacement for default fragment backstack
 */

class MapActivityViewModel : ViewModel() {
    data class BackStackEntry(var name: String, var bundle: Bundle?)

    private val backstack: ArrayList<BackStackEntry>

    init {
        backstack = arrayListOf()
    }

    fun addToBackStack(fragment: Fragment) {
        val frag = fragment.tag
        if (frag != null) {
            val bundle = Bundle()
            fragment.onSaveInstanceState(bundle)
            addToBackStack(frag, bundle)
        }
    }

    fun addToBackStack(name: String, bundle: Bundle? = null) {
        if (backstack.size == 0 || backstack.last().name != name) {
            val idx = backstack.indexOfFirst { it.name == name }
            if (idx > -1) {
                backstack.subList(idx, backstack.size).clear()
            }
            backstack.add(BackStackEntry(name, bundle))
            //printBackStack()
        }
    }

    fun updateLast(fragment: Fragment) {
        if (fragment.tag == backstack.last().name) {
            val bundle = Bundle()
            fragment.onSaveInstanceState(bundle)
            backstack.last().bundle = bundle
        }
    }

    fun popBackStack(): BackStackEntry? {
        if (backstack.size > 0) {
            //printBackStack()
            return backstack.popLast()
        }
        return null
    }

    private fun printBackStack() {
        println("BackStack")
        println("---------")
        for (entry in backstack) {
            println(entry.name)
        }
    }
}