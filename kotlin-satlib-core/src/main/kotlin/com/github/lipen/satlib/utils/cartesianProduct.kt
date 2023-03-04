package com.github.lipen.satlib.utils

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
