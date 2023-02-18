package com.github.lipen.satlib.utils

interface Tuple {
    val values: List<*>

    companion object {
        // constructor
        operator fun <T> invoke(t: T): Tuple1<T> = Tuple1(t)

        // constructor
        operator fun <T1, T2> invoke(t1: T1, t2: T2): Tuple2<T1, T2> = Tuple2(t1, t2)

        // constructor
        operator fun <T1, T2, T3> invoke(t1: T1, t2: T2, t3: T3): Tuple3<T1, T2, T3> = Tuple3(t1, t2, t3)
    }
}

val Tuple.size: Int get() = values.size

// ==================================================

data class Tuple1<out T>(
    val t: T,
) : Tuple {
    override val values: List<*> = listOf(t)

    operator fun <T2> plus(t2: T2): Tuple2<T, T2> = Tuple2(t, t2)

    override fun toString(): String {
        return "(${values.joinToString(", ")})"
    }
}

data class Tuple2<out T1, out T2>(
    val t1: T1,
    val t2: T2,
) : Tuple {
    override val values: List<*> = listOf(t1, t2)

    operator fun <T3> plus(t3: T3): Tuple3<T1, T2, T3> = Tuple3(t1, t2, t3)

    override fun toString(): String {
        return "(${values.joinToString(", ")})"
    }
}

data class Tuple3<out T1, out T2, out T3>(
    val t1: T1,
    val t2: T2,
    val t3: T3,
) : Tuple {
    override val values: List<*> = listOf(t1, t2, t3)

    operator fun <T4> plus(t4: T4): Tuple = TODO("Tuple4(t1, t2, t3, t4)")

    override fun toString(): String {
        return "(${values.joinToString(", ")})"
    }
}

// ==================================================

fun <T> cartesianProduct(
    it: Iterable<T>,
): Sequence<Tuple1<T>> = sequence {
    for (t in it)
        yield(Tuple1(t))
}

fun <T1, T2> cartesianProduct(
    it1: Iterable<T1>,
    it2: Iterable<T2>,
): Sequence<Tuple2<T1, T2>> = sequence {
    for (t1 in it1)
        for (t2 in it2)
            yield(Tuple2(t1, t2))
}

fun <T1, T2, T3> cartesianProduct(
    it1: Iterable<T1>,
    it2: Iterable<T2>,
    it3: Iterable<T3>,
): Sequence<Tuple3<T1, T2, T3>> = sequence {
    for (t1 in it1)
        for (t2 in it2)
            for (t3 in it3)
                yield(Tuple3(t1, t2, t3))
}
