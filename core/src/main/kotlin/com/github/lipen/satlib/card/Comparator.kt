package com.github.lipen.satlib.card

import com.github.lipen.satlib.solver.Solver

fun Solver.declareComparatorLessThan(totalizer: List<Int>, upperBound: Int, declared: Int? = null) {
    require(upperBound <= totalizer.size)

    val max = declared ?: totalizer.size
    comment("Comparator(<$upperBound up to $max)")
    for (i in max downTo upperBound) {
        // Note: `totalizer` is 0-based, but all params are naturally 1-based
        addClause(-totalizer[i - 1])
    }
}

fun Solver.declareComparatorGreaterThanOrEqual(totalizer: List<Int>, lowerBound: Int, declared: Int? = null) {
    require(lowerBound >= 1)

    val min = declared ?: 1
    comment("Comparator(>=$lowerBound from $min)")
    for (i in min..lowerBound) {
        // Note: `totalizer` is 0-based, but all params are naturally 1-based
        addClause(totalizer[i - 1])
    }
}
