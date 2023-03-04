package com.github.lipen.satlib.utils

interface Tuple {
    val values: List<*>

    companion object {
        // constructor
        operator fun <T1> invoke(
            t1: T1,
        ): Tuple1<T1> = Tuple1(t1)

        // constructor
        operator fun <T1, T2> invoke(
            t1: T1,
            t2: T2,
        ): Tuple2<T1, T2> = Tuple2(t1, t2)

        // constructor
        operator fun <T1, T2, T3> invoke(
            t1: T1,
            t2: T2,
            t3: T3,
        ): Tuple3<T1, T2, T3> = Tuple3(t1, t2, t3)

        // constructor
        operator fun <T1, T2, T3, T4> invoke(
            t1: T1,
            t2: T2,
            t3: T3,
            t4: T4,
        ): Tuple4<T1, T2, T3, T4> = Tuple4(t1, t2, t3, t4)

        // constructor
        operator fun <T1, T2, T3, T4, T5> invoke(
            t1: T1,
            t2: T2,
            t3: T3,
            t4: T4,
            t5: T5,
        ): Tuple5<T1, T2, T3, T4, T5> = Tuple5(t1, t2, t3, t4, t5)

        // constructor
        operator fun <T1, T2, T3, T4, T5, T6> invoke(
            t1: T1,
            t2: T2,
            t3: T3,
            t4: T4,
            t5: T5,
            t6: T6,
        ): Tuple6<T1, T2, T3, T4, T5, T6> = Tuple6(t1, t2, t3, t4, t5, t6)

        // constructor
        operator fun <T1, T2, T3, T4, T5, T6, T7> invoke(
            t1: T1,
            t2: T2,
            t3: T3,
            t4: T4,
            t5: T5,
            t6: T6,
            t7: T7,
        ): Tuple7<T1, T2, T3, T4, T5, T6, T7> = Tuple7(t1, t2, t3, t4, t5, t6, t7)

        // constructor
        operator fun <T1, T2, T3, T4, T5, T6, T7, T8> invoke(
            t1: T1,
            t2: T2,
            t3: T3,
            t4: T4,
            t5: T5,
            t6: T6,
            t7: T7,
            t8: T8,
        ): Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> = Tuple8(t1, t2, t3, t4, t5, t6, t7, t8)

        // constructor
        operator fun <T1, T2, T3, T4, T5, T6, T7, T8, T9> invoke(
            t1: T1,
            t2: T2,
            t3: T3,
            t4: T4,
            t5: T5,
            t6: T6,
            t7: T7,
            t8: T8,
            t9: T9,
        ): Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9> = Tuple9(t1, t2, t3, t4, t5, t6, t7, t8, t9)
    }
}

val Tuple.size: Int get() = values.size

// ==================================================

data class Tuple1<out T1>(
    val t1: T1,
) : Tuple {
    override val values: List<*> = listOf(t1)

    override fun toString(): String {
        return "(${values.joinToString(", ")})"
    }
}

data class Tuple2<out T1, out T2>(
    val t1: T1,
    val t2: T2,
) : Tuple {
    override val values: List<*> = listOf(t1, t2)

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

    override fun toString(): String {
        return "(${values.joinToString(", ")})"
    }
}

data class Tuple4<out T1, out T2, out T3, out T4>(
    val t1: T1,
    val t2: T2,
    val t3: T3,
    val t4: T4,
) : Tuple {
    override val values: List<*> = listOf(t1, t2, t3, t4)

    override fun toString(): String {
        return "(${values.joinToString(", ")})"
    }
}

data class Tuple5<out T1, out T2, out T3, out T4, out T5>(
    val t1: T1,
    val t2: T2,
    val t3: T3,
    val t4: T4,
    val t5: T5,
) : Tuple {
    override val values: List<*> = listOf(t1, t2, t3, t4, t5)

    override fun toString(): String {
        return "(${values.joinToString(", ")})"
    }
}

data class Tuple6<out T1, out T2, out T3, out T4, out T5, out T6>(
    val t1: T1,
    val t2: T2,
    val t3: T3,
    val t4: T4,
    val t5: T5,
    val t6: T6,
) : Tuple {
    override val values: List<*> = listOf(t1, t2, t3, t4, t5, t6)

    override fun toString(): String {
        return "(${values.joinToString(", ")})"
    }
}

data class Tuple7<out T1, out T2, out T3, out T4, out T5, out T6, out T7>(
    val t1: T1,
    val t2: T2,
    val t3: T3,
    val t4: T4,
    val t5: T5,
    val t6: T6,
    val t7: T7,
) : Tuple {
    override val values: List<*> = listOf(t1, t2, t3, t4, t5, t6, t7)

    override fun toString(): String {
        return "(${values.joinToString(", ")})"
    }
}

data class Tuple8<out T1, out T2, out T3, out T4, out T5, out T6, out T7, out T8>(
    val t1: T1,
    val t2: T2,
    val t3: T3,
    val t4: T4,
    val t5: T5,
    val t6: T6,
    val t7: T7,
    val t8: T8,
) : Tuple {
    override val values: List<*> = listOf(t1, t2, t3, t4, t5, t6, t7, t8)

    override fun toString(): String {
        return "(${values.joinToString(", ")})"
    }
}

data class Tuple9<out T1, out T2, out T3, out T4, out T5, out T6, out T7, out T8, out T9>(
    val t1: T1,
    val t2: T2,
    val t3: T3,
    val t4: T4,
    val t5: T5,
    val t6: T6,
    val t7: T7,
    val t8: T8,
    val t9: T9,
) : Tuple {
    override val values: List<*> = listOf(t1, t2, t3, t4, t5, t6, t7, t8, t9)

    override fun toString(): String {
        return "(${values.joinToString(", ")})"
    }
}

// ==================================================

fun <T1> cartesianProduct(
    it1: Iterable<T1>,
): Sequence<Tuple1<T1>> = sequence {
    for (t1 in it1)
        yield(Tuple(t1))
}

fun <T1, T2> cartesianProduct(
    it1: Iterable<T1>,
    it2: Iterable<T2>,
): Sequence<Tuple2<T1, T2>> = sequence {
    for (t1 in it1)
        for (t2 in it2)
            yield(Tuple(t1, t2))
}

fun <T1, T2, T3> cartesianProduct(
    it1: Iterable<T1>,
    it2: Iterable<T2>,
    it3: Iterable<T3>,
): Sequence<Tuple3<T1, T2, T3>> = sequence {
    for (t1 in it1)
        for (t2 in it2)
            for (t3 in it3)
                yield(Tuple(t1, t2, t3))
}

fun <T1, T2, T3, T4> cartesianProduct(
    it1: Iterable<T1>,
    it2: Iterable<T2>,
    it3: Iterable<T3>,
    it4: Iterable<T4>,
): Sequence<Tuple4<T1, T2, T3, T4>> = sequence {
    for (t1 in it1)
        for (t2 in it2)
            for (t3 in it3)
                for (t4 in it4)
                    yield(Tuple(t1, t2, t3, t4))
}

fun <T1, T2, T3, T4, T5> cartesianProduct(
    it1: Iterable<T1>,
    it2: Iterable<T2>,
    it3: Iterable<T3>,
    it4: Iterable<T4>,
    it5: Iterable<T5>,
): Sequence<Tuple5<T1, T2, T3, T4, T5>> = sequence {
    for (t1 in it1)
        for (t2 in it2)
            for (t3 in it3)
                for (t4 in it4)
                    for (t5 in it5)
                        yield(Tuple(t1, t2, t3, t4, t5))
}

fun <T1, T2, T3, T4, T5, T6> cartesianProduct(
    it1: Iterable<T1>,
    it2: Iterable<T2>,
    it3: Iterable<T3>,
    it4: Iterable<T4>,
    it5: Iterable<T5>,
    it6: Iterable<T6>,
): Sequence<Tuple6<T1, T2, T3, T4, T5, T6>> = sequence {
    for (t1 in it1)
        for (t2 in it2)
            for (t3 in it3)
                for (t4 in it4)
                    for (t5 in it5)
                        for (t6 in it6)
                            yield(Tuple(t1, t2, t3, t4, t5, t6))
}

fun <T1, T2, T3, T4, T5, T6, T7> cartesianProduct(
    it1: Iterable<T1>,
    it2: Iterable<T2>,
    it3: Iterable<T3>,
    it4: Iterable<T4>,
    it5: Iterable<T5>,
    it6: Iterable<T6>,
    it7: Iterable<T7>,
): Sequence<Tuple7<T1, T2, T3, T4, T5, T6, T7>> = sequence {
    for (t1 in it1)
        for (t2 in it2)
            for (t3 in it3)
                for (t4 in it4)
                    for (t5 in it5)
                        for (t6 in it6)
                            for (t7 in it7)
                                yield(Tuple(t1, t2, t3, t4, t5, t6, t7))
}

fun <T1, T2, T3, T4, T5, T6, T7, T8> cartesianProduct(
    it1: Iterable<T1>,
    it2: Iterable<T2>,
    it3: Iterable<T3>,
    it4: Iterable<T4>,
    it5: Iterable<T5>,
    it6: Iterable<T6>,
    it7: Iterable<T7>,
    it8: Iterable<T8>,
): Sequence<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> = sequence {
    for (t1 in it1)
        for (t2 in it2)
            for (t3 in it3)
                for (t4 in it4)
                    for (t5 in it5)
                        for (t6 in it6)
                            for (t7 in it7)
                                for (t8 in it8)
                                    yield(Tuple(t1, t2, t3, t4, t5, t6, t7, t8))
}

fun <T1, T2, T3, T4, T5, T6, T7, T8, T9> cartesianProduct(
    it1: Iterable<T1>,
    it2: Iterable<T2>,
    it3: Iterable<T3>,
    it4: Iterable<T4>,
    it5: Iterable<T5>,
    it6: Iterable<T6>,
    it7: Iterable<T7>,
    it8: Iterable<T8>,
    it9: Iterable<T9>,
): Sequence<Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> = sequence {
    for (t1 in it1)
        for (t2 in it2)
            for (t3 in it3)
                for (t4 in it4)
                    for (t5 in it5)
                        for (t6 in it6)
                            for (t7 in it7)
                                for (t8 in it8)
                                    for (t9 in it9)
                                        yield(Tuple(t1, t2, t3, t4, t5, t6, t7, t8, t9))
}
