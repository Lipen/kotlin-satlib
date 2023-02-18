package com.github.lipen.satlib.utils

interface Domains<out K : Tuple> : Iterable<K> {
    val domains: List<Domain<*>>

    override operator fun iterator(): Iterator<K>

    companion object {
        // constructor
        operator fun <V> invoke(
            d: Domain<V>,
        ): Domains1<V> = Domains1(d)

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

data class Domains1<out V>(
    val d: Domain<V>,
) : Domains<Tuple1<V>> {
    override val domains: List<Domain<*>> = listOf(d)

    override operator fun iterator(): Iterator<Tuple1<V>> {
        return cartesianProduct(d).iterator()
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
