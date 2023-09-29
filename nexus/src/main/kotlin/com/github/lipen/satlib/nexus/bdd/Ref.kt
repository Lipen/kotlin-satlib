package com.github.lipen.satlib.nexus.bdd


import kotlin.math.absoluteValue

@JvmInline
value class Ref(val index: Int) : Comparable<Ref> {
    val negated: Boolean
        get() = index < 0

    init {
        require(index != 0)
    }

    operator fun unaryMinus(): Ref {
        return Ref(-index)
    }

    override fun compareTo(other: Ref): Int {
        return index.compareTo(other.index)
    }

    override fun toString(): String {
        return "${if (negated) "~" else ""}@${index.absoluteValue}"
    }
}
