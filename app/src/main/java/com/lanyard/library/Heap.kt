package com.lanyard.library

import java.util.*

class PriorityQueue<T> (sort: (T, T) -> Boolean) {
    private var heap: Heap<T>

    init {
        heap = Heap(sort)
    }

    fun isEmpty(): Boolean {
        return heap.isEmpty()
    }

    fun count(): Int {
        return heap.count()
    }

    fun peek() : T? {
        return heap.peek()
    }

    fun enqueue(element: T) {
        heap.insert(element)
    }

    fun dequeue() : T? {
        return heap.remove()
    }

    fun changePriority(index: Int, value: T) {
        return heap.replace(index, value)
    }
}

class Heap<T>(sort: (T, T) -> Boolean) {

    var nodes: ArrayList<T>

    private var orderCriteria: (T, T) -> Boolean

    init {
        this.orderCriteria = sort
        this.nodes = ArrayList<T>()
    }

    constructor (array: ArrayList<T>, sort: (T, T) -> Boolean) : this(sort) {
        configureHeap(array)
    }

    private fun configureHeap(array: ArrayList<T>) {
        this.nodes = array
        for (i in nodes.size / 2 - 1 downTo 0 step 1) {
            shiftDown(i)
        }
    }

    fun isEmpty(): Boolean {
        return nodes.isEmpty()
    }

    fun count(): Int {
        return nodes.size
    }

    internal fun parentIndex(idx: Int): Int {
        return (idx - 1) / 2
    }

    internal fun leftChildIndex(idx: Int): Int {
        return 2 * idx + 1
    }

    internal fun rightChildIndex(idx: Int): Int {
        return 2 * idx + 2
    }

    public fun peek(): T? {
        return nodes.first()
    }

    fun insert(value: T) {
        nodes.add(value)
        shiftUp(nodes.size - 1)
    }

    fun <S : Sequence<T>> insert(sequence: S) {
        for (value in sequence) {
            insert(value)
        }
    }

    /**
     * Allows you to change an element. This reorders the heap so that
     * the max-heap or min-heap property still holds.
     */
    fun replace(index: Int, value: T) {
        if (index >= nodes.size) {
            return
        }

        remove(index)
        insert(value)
    }

    /**
     * Removes the root node from the heap. For a max-heap, this is the maximum
     * value; for a min-heap it is the minimum value. Performance: O(log n).
     */
    fun remove(): T? {
        if (nodes.isEmpty()) {
            return null
        }

        if (nodes.size == 1) {
            return nodes.removeAt(nodes.size - 1)
        } else {
            // Use the last node to replace the first one, then fix the heap by
            // shifting this new first node into its proper position.
            val value = nodes[0]
            nodes[0] = nodes.removeAt(nodes.size - 1)
            shiftDown(0)
            return value
        }
    }

    fun remove(index: Int): T? {
        if (index >= nodes.size) {
            return null
        }

        val size = nodes.size - 1
        if (index != size) {
            Collections.swap(nodes, index, size)
            shiftDown(index, size)
            shiftUp(index)
        }
        return nodes.removeAt(nodes.size - 1)
    }


    internal fun shiftUp(index: Int) {
        var childIndex = index
        val child = nodes[childIndex]
        var parentIndex = this.parentIndex(childIndex)

        while (childIndex > 0 && orderCriteria(child, nodes[parentIndex])) {
            nodes[childIndex] = nodes[parentIndex]
            childIndex = parentIndex
            parentIndex = this.parentIndex(childIndex)
        }

        nodes[childIndex] = child
    }

    internal fun shiftDown(startIndex: Int, endIndex: Int) {
        val leftChildIndex = this.leftChildIndex(startIndex)
        val rightChildIndex = leftChildIndex + 1

        var first = startIndex
        if (leftChildIndex < endIndex && orderCriteria(nodes[leftChildIndex], nodes[first])) {
            first = leftChildIndex
        }
        if (rightChildIndex < endIndex && orderCriteria(nodes[rightChildIndex], nodes[first])) {
            first = rightChildIndex
        }
        if (first == startIndex) {
            return
        }
        Collections.swap(nodes, startIndex, first)
        shiftDown(first, endIndex)
    }

    internal fun shiftDown(index: Int) {
        shiftDown(index, nodes.size)
    }

}