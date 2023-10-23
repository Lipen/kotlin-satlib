package com.github.lipen.satlib.utils

interface Domains<out K : Tuple> : Iterable<K> {
    val domains: List<Domain<*>>

    override operator fun iterator(): Iterator<K>

    companion object {
        // constructor
        operator fun <V1> invoke(
            d1: Domain<V1>,
        ): Domains1<V1> = Domains1(d1)

        // constructor
        operator fun <V1, V2> invoke(
            d1: Domain<V1>,
            d2: Domain<V2>,
        ): Domains2<V1, V2> = Domains2(d1, d2)

        // constructor
        operator fun <V1, V2, V3> invoke(
            d1: Domain<V1>,
            d2: Domain<V2>,
            d3: Domain<V3>,
        ): Domains3<V1, V2, V3> = Domains3(d1, d2, d3)

        // constructor
        operator fun <V1, V2, V3, V4> invoke(
            d1: Domain<V1>,
            d2: Domain<V2>,
            d3: Domain<V3>,
            d4: Domain<V4>,
        ): Domains4<V1, V2, V3, V4> = Domains4(d1, d2, d3, d4)

        // constructor
        operator fun <V1, V2, V3, V4, V5> invoke(
            d1: Domain<V1>,
            d2: Domain<V2>,
            d3: Domain<V3>,
            d4: Domain<V4>,
            d5: Domain<V5>,
        ): Domains5<V1, V2, V3, V4, V5> = Domains5(d1, d2, d3, d4, d5)

        // constructor
        operator fun <V1, V2, V3, V4, V5, V6> invoke(
            d1: Domain<V1>,
            d2: Domain<V2>,
            d3: Domain<V3>,
            d4: Domain<V4>,
            d5: Domain<V5>,
            d6: Domain<V6>,
        ): Domains6<V1, V2, V3, V4, V5, V6> = Domains6(d1, d2, d3, d4, d5, d6)

        // constructor
        operator fun <V1, V2, V3, V4, V5, V6, V7> invoke(
            d1: Domain<V1>,
            d2: Domain<V2>,
            d3: Domain<V3>,
            d4: Domain<V4>,
            d5: Domain<V5>,
            d6: Domain<V6>,
            d7: Domain<V7>,
        ): Domains7<V1, V2, V3, V4, V5, V6, V7> = Domains7(d1, d2, d3, d4, d5, d6, d7)

        // constructor
        operator fun <V1, V2, V3, V4, V5, V6, V7, V8> invoke(
            d1: Domain<V1>,
            d2: Domain<V2>,
            d3: Domain<V3>,
            d4: Domain<V4>,
            d5: Domain<V5>,
            d6: Domain<V6>,
            d7: Domain<V7>,
            d8: Domain<V8>,
        ): Domains8<V1, V2, V3, V4, V5, V6, V7, V8> = Domains8(d1, d2, d3, d4, d5, d6, d7, d8)

        // constructor
        operator fun <V1, V2, V3, V4, V5, V6, V7, V8, V9> invoke(
            d1: Domain<V1>,
            d2: Domain<V2>,
            d3: Domain<V3>,
            d4: Domain<V4>,
            d5: Domain<V5>,
            d6: Domain<V6>,
            d7: Domain<V7>,
            d8: Domain<V8>,
            d9: Domain<V9>,
        ): Domains9<V1, V2, V3, V4, V5, V6, V7, V8, V9> = Domains9(d1, d2, d3, d4, d5, d6, d7, d8, d9)
    }
}

operator fun <K : Tuple> Domains<K>.contains(key: K): Boolean {
    if (key.values.size != domains.size) {
        return false
    }
    // Note: the cast of `d` to `Domain<Any?>` from `Domain<*>` is necessary,
    // because otherwise `Iterable<T>.contains` extension method from
    // the standard library is getting called, despite existence of
    // `Domain::contains` method in the `Domain<T>` interface.
    // Alternatively, this could be "fixed" by changing the type
    // of `Domains::domain` from `List<Domain<*>>` to `List<Domain<Any?>>`.
    return key.values.zip(domains).all { (v, d) -> v in (d as Domain<Any?>) }
}

// ==================================================

data class Domains1<out V1>(
    val d1: Domain<V1>,
) : Domains<Tuple1<V1>> {
    override val domains: List<Domain<*>> = listOf(d1)

    override operator fun iterator(): Iterator<Tuple1<V1>> {
        return cartesianProduct(d1).iterator()
    }
}

data class Domains2<out V1, out V2>(
    val d1: Domain<V1>,
    val d2: Domain<V2>,
) : Domains<Tuple2<V1, V2>> {
    override val domains: List<Domain<*>> = listOf(d1, d2)

    override operator fun iterator(): Iterator<Tuple2<V1, V2>> {
        return cartesianProduct(d1, d2).iterator()
    }
}

data class Domains3<out V1, out V2, out V3>(
    val d1: Domain<V1>,
    val d2: Domain<V2>,
    val d3: Domain<V3>,
) : Domains<Tuple3<V1, V2, V3>> {
    override val domains: List<Domain<*>> = listOf(d1, d2, d3)

    override operator fun iterator(): Iterator<Tuple3<V1, V2, V3>> {
        return cartesianProduct(d1, d2, d3).iterator()
    }
}

data class Domains4<out V1, out V2, out V3, out V4>(
    val d1: Domain<V1>,
    val d2: Domain<V2>,
    val d3: Domain<V3>,
    val d4: Domain<V4>,
) : Domains<Tuple4<V1, V2, V3, V4>> {
    override val domains: List<Domain<*>> = listOf(d1, d2, d3, d4)

    override operator fun iterator(): Iterator<Tuple4<V1, V2, V3, V4>> {
        return cartesianProduct(d1, d2, d3, d4).iterator()
    }
}

data class Domains5<out V1, out V2, out V3, out V4, out V5>(
    val d1: Domain<V1>,
    val d2: Domain<V2>,
    val d3: Domain<V3>,
    val d4: Domain<V4>,
    val d5: Domain<V5>,
) : Domains<Tuple5<V1, V2, V3, V4, V5>> {
    override val domains: List<Domain<*>> = listOf(d1, d2, d3, d4, d5)

    override operator fun iterator(): Iterator<Tuple5<V1, V2, V3, V4, V5>> {
        return cartesianProduct(d1, d2, d3, d4, d5).iterator()
    }
}

data class Domains6<out V1, out V2, out V3, out V4, out V5, out V6>(
    val d1: Domain<V1>,
    val d2: Domain<V2>,
    val d3: Domain<V3>,
    val d4: Domain<V4>,
    val d5: Domain<V5>,
    val d6: Domain<V6>,
) : Domains<Tuple6<V1, V2, V3, V4, V5, V6>> {
    override val domains: List<Domain<*>> = listOf(d1, d2, d3, d4, d5, d6)

    override operator fun iterator(): Iterator<Tuple6<V1, V2, V3, V4, V5, V6>> {
        return cartesianProduct(d1, d2, d3, d4, d5, d6).iterator()
    }
}

data class Domains7<out V1, out V2, out V3, out V4, out V5, out V6, out V7>(
    val d1: Domain<V1>,
    val d2: Domain<V2>,
    val d3: Domain<V3>,
    val d4: Domain<V4>,
    val d5: Domain<V5>,
    val d6: Domain<V6>,
    val d7: Domain<V7>,
) : Domains<Tuple7<V1, V2, V3, V4, V5, V6, V7>> {
    override val domains: List<Domain<*>> = listOf(d1, d2, d3, d4, d5, d6, d7)

    override operator fun iterator(): Iterator<Tuple7<V1, V2, V3, V4, V5, V6, V7>> {
        return cartesianProduct(d1, d2, d3, d4, d5, d6, d7).iterator()
    }
}

data class Domains8<out V1, out V2, out V3, out V4, out V5, out V6, out V7, out V8>(
    val d1: Domain<V1>,
    val d2: Domain<V2>,
    val d3: Domain<V3>,
    val d4: Domain<V4>,
    val d5: Domain<V5>,
    val d6: Domain<V6>,
    val d7: Domain<V7>,
    val d8: Domain<V8>,
) : Domains<Tuple8<V1, V2, V3, V4, V5, V6, V7, V8>> {
    override val domains: List<Domain<*>> = listOf(d1, d2, d3, d4, d5, d6, d7, d8)

    override operator fun iterator(): Iterator<Tuple8<V1, V2, V3, V4, V5, V6, V7, V8>> {
        return cartesianProduct(d1, d2, d3, d4, d5, d6, d7, d8).iterator()
    }
}

data class Domains9<out V1, out V2, out V3, out V4, out V5, out V6, out V7, out V8, out V9>(
    val d1: Domain<V1>,
    val d2: Domain<V2>,
    val d3: Domain<V3>,
    val d4: Domain<V4>,
    val d5: Domain<V5>,
    val d6: Domain<V6>,
    val d7: Domain<V7>,
    val d8: Domain<V8>,
    val d9: Domain<V9>,
) : Domains<Tuple9<V1, V2, V3, V4, V5, V6, V7, V8, V9>> {
    override val domains: List<Domain<*>> = listOf(d1, d2, d3, d4, d5, d6, d7, d8, d9)

    override operator fun iterator(): Iterator<Tuple9<V1, V2, V3, V4, V5, V6, V7, V8, V9>> {
        return cartesianProduct(d1, d2, d3, d4, d5, d6, d7, d8, d9).iterator()
    }
}
