package com.github.lipen.satlib.utils

interface DomainMap<K : Tuple, out V : Any> {
    val map: Map<K, V>
    val domains: Domains<K>

    operator fun contains(key: K): Boolean
    operator fun get(key: K): V
}

interface MutableDomainMap<K : Tuple, V : Any> : DomainMap<K, V> {
    override val map: MutableMap<K, V>

    operator fun set(key: K, value: V)

    companion object {
        // constructor
        operator fun <K : Tuple, V : Any> invoke(domains: Domains<K>): MutableDomainMap<K, V> {
            return MutableDomainMapImpl(domains)
        }

        // constructor with initialization
        inline operator fun <K : Tuple, V : Any> invoke(
            domains: Domains<K>,
            init: (K) -> V,
        ): MutableDomainMap<K, V> {
            return invoke<K, V>(domains).filledBy(init)
        }

        // ====================================================

        @JvmInline
        value class Builder<K : Tuple>(val domains: Domains<K>) {
            fun <V : Any> build(): MutableDomainMap<K, V> = MutableDomainMap(domains)
            inline fun <V : Any> build(init: (K) -> V): MutableDomainMap<K, V> = MutableDomainMap(domains, init)
        }

        fun <K : Tuple> builder(domains: Domains<K>): Builder<K> = Builder(domains)

        // ====================================================

        fun <V> builder(
            d: Domain<V>,
        ): Builder<Tuple1<V>> = builder(Domains(d))

        fun <V1, V2> builder(
            d1: Domain<V1>,
            d2: Domain<V2>,
        ): Builder<Tuple2<V1, V2>> = Builder(Domains(d1, d2))

        fun <V1, V2, V3> builder(
            d1: Domain<V1>,
            d2: Domain<V2>,
            d3: Domain<V3>,
        ): Builder<Tuple3<V1, V2, V3>> = Builder(Domains(d1, d2, d3))
    }
}

private data class MutableDomainMapImpl<K : Tuple, V : Any>(
    override val domains: Domains<K>,
    override val map: MutableMap<K, V> = mutableMapOf(),
) : MutableDomainMap<K, V> {
    override operator fun contains(key: K): Boolean {
        return domains.contains(key) && map.contains(key)
    }

    override operator fun get(key: K): V {
        // println("DomainMap::get($key)")
        if (!domains.contains(key)) {
            println("WARNING key '$key' is out of bounds: $domains")
        }
        return map.getValue(key)
    }

    override operator fun set(key: K, value: V) {
        // println("DomainMap::set($key, $value)")
        if (!domains.contains(key)) {
            println("WARNING key '$key' is out of bounds: $domains")
        }
        map[key] = value
    }
}

//region ===[ getting ]===

operator fun <K, V : Any> DomainMap<Tuple1<K>, V>.get(
    key: K,
): V {
    return get(Tuple(key))
}

operator fun <K1, K2, V : Any> DomainMap<Tuple2<K1, K2>, V>.get(
    key1: K1,
    key2: K2,
): V {
    return get(Tuple(key1, key2))
}

operator fun <K1, K2, K3, V : Any> DomainMap<Tuple3<K1, K2, K3>, V>.get(
    key1: K1,
    key2: K2,
    key3: K3,
): V {
    return get(Tuple(key1, key2, key3))
}

//endregion

//region ===[ setting ]===

operator fun <K, V : Any> MutableDomainMap<Tuple1<K>, V>.set(
    key: K,
    value: V,
) {
    set(Tuple(key), value)
}

operator fun <K1, K2, V : Any> MutableDomainMap<Tuple2<K1, K2>, V>.set(
    key1: K1,
    key2: K2,
    value: V,
) {
    set(Tuple(key1, key2), value)
}

operator fun <K1, K2, K3, V : Any> MutableDomainMap<Tuple3<K1, K2, K3>, V>.set(
    key1: K1,
    key2: K2,
    key3: K3,
    value: V,
) {
    set(Tuple(key1, key2, key3), value)
}

//endregion

//region ===[ filling ]===

inline fun <K : Tuple, V : Any> MutableDomainMap<K, V>.fillBy(init: (K) -> V) {
    for (k in domains) {
        map[k] = init(k)
    }
}

inline fun <M : MutableDomainMap<K, V>, K : Tuple, V : Any> M.filledBy(init: (K) -> V): M =
    apply { fillBy(init) }

//endregion

//region ===[ mapping ]===

fun <K : Tuple, V : Any, R : Any> DomainMap<K, V>.mapValues(
    transform: (Map.Entry<K, V>) -> R,
): DomainMap<K, R> {
    return MutableDomainMapImpl(domains, map.mapValues(transform).toMutableMap())
}

//endregion

//region ===[ other extensions ]===

fun <K : Tuple, V : Any> DomainMap<K, V>.asMut(): MutableDomainMap<K, V> = when (this) {
    is MutableDomainMap<K, V> -> this
    else -> error("This DomainMap cannot be converted to MutableDomainMap")
}

fun <K : Tuple, V : Any> DomainMap<K, V>.toMut(): MutableDomainMap<K, V> =
    MutableDomainMapImpl(domains, map.toMutableMap())

//endregion
