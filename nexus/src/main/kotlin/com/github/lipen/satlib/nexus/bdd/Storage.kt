package com.github.lipen.satlib.nexus.bdd

import kotlin.math.min

internal class Storage(capacity: Int) {
    private val dataOccupied = java.util.BitSet(capacity)
    private val dataVar = IntArray(capacity)
    private val dataLow = IntArray(capacity)
    private val dataHigh = IntArray(capacity)
    private val dataNext = IntArray(capacity)

    private var minFree: Int = 1

    var lastIndex: Int = 0
        private set
    var realSize: Int = 0
        private set

    fun isOccupied(index: Int): Boolean = dataOccupied[index]

    // Invariant: variable(0) = low(0) = high(0) = next(0) = 0
    fun variable(index: Int): Int = dataVar[index]
    fun low(index: Int): Int = dataLow[index]
    fun high(index: Int): Int = dataHigh[index]
    fun next(index: Int): Int = dataNext[index]

    private fun getFreeIndex(): Int {
        return (minFree..lastIndex).firstOrNull { !dataOccupied[it] } ?: ++lastIndex
    }

    internal fun alloc(): Int {
        val index = minFree
        if (index > lastIndex) {
            lastIndex = index
        }
        realSize++
        dataOccupied[index] = true
        minFree = getFreeIndex()
        return index
    }

    fun add(v: Int, low: Int, high: Int, next: Int = 0): Int {
        require(v > 0)
        require(low != 0)
        require(high != 0)
        val index = alloc()
        check(index < dataVar.size) { error("Index reached capacity") }
        dataVar[index] = v
        dataLow[index] = low
        dataHigh[index] = high
        dataNext[index] = next
        return index
    }

    fun drop(index: Int) {
        require(index > 0)
        realSize--
        dataOccupied[index] = false
        minFree = min(minFree, index)
    }

    fun setNext(index: Int, next: Int) {
        require(index > 0)
        dataNext[index] = next
    }
}
