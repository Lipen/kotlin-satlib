@file:Suppress("FunctionName")

package com.github.lipen.satlib.nexus.bdd

import java.util.PriorityQueue
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
    // TODO: check uniqueness and consistency
    var current = one
    for (lit in literals.sortedByDescending { it.absoluteValue }) {
        // val x = mkVar(lit)
        // current = applyAnd(current, x)
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

fun BDD.axioms(clauses: Iterable<List<Int>>): List<Ref> {
    return clauses.map { clause(it) }
}

fun BDD.cojoinLinear(clauses: Iterable<Ref>): Ref {
    var f = one
    for (clause in clauses) {
        f = applyAnd(f, clause)
    }
    return f
}

fun BDD.cojoinTree(clauses: Iterable<Ref>): Ref {
    data class Node(val node: Ref) : Comparable<Node> {
        val size: Int = descendants(node).size

        override fun compareTo(other: Node): Int {
            return size compareTo other.size
        }
    }

    val queue = PriorityQueue(clauses.map { Node(it) })
    while (queue.size > 1) {
        val a = queue.remove()
        val b = queue.remove()
        val r = Node(applyAnd(a.node, b.node))
        queue.add(r)
    }
    return queue.remove().node
}
