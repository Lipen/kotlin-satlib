@file:Suppress("FunctionName")

package com.github.lipen.satlib.nexus.bdd

import kotlin.math.absoluteValue

data class Triplet(
    val v: Int,
    val low: Int,
    val high: Int,
)

fun BDD.getTriplet(index: Int): Triplet? {
    val i = index.absoluteValue
    if (i == 1) {
        return Triplet(0, 0, 0)
    }
    return if (storage.isOccupied(i)) {
        Triplet(v = variable(i), low = low(i).index, high = high(i).index)
    } else {
        null
    }
}

fun BDD.getTriplet(node: Ref): Triplet? = getTriplet(node.index)

fun BDD.clause(literals: Iterable<Int>): Ref {
    // TODO: check uniqueness and consistency
    var current = zero
    for (lit in literals.sortedByDescending { it.absoluteValue }) {
        // val x = mkVar(lit)
        // current = applyOr(current, x)
        current = if (lit < 0) {
            mkNode(v = -lit, low = one, high = current)
        } else {
            mkNode(v = lit, low = current, high = one)
        }
    }
    return current
}

fun BDD.clause_(literals: IntArray): Ref {
    return clause(literals.asList())
}

fun BDD.clause(vararg literals: Int): Ref {
    return clause_(literals)
}

fun BDD.cube(literals: Iterable<Int>): Ref {
    var current = one
    for (lit in literals.sortedByDescending { it.absoluteValue }) {
        current = if (lit < 0) {
            mkNode(v = -lit, low = current, high = zero)
        } else {
            mkNode(v = lit, low = zero, high = current)
        }
    }
    return current
}

fun BDD.cube_(literals: IntArray): Ref {
    return cube(literals.asList())
}

fun BDD.cube(vararg literals: Int): Ref {
    return cube_(literals)
}

fun BDD.and(xs: Iterable<Ref>): Ref {
    var f = one
    for (x in xs) {
        f = applyAnd(f, x)
    }
    return f
}

fun BDD.and_(xs: Array<out Ref>): Ref {
    return and(xs.asList())
}

fun <T: Ref> BDD.and(vararg xs: T): Ref {
    return and_(xs)
}
