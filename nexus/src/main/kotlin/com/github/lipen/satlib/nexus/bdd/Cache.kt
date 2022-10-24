package com.github.lipen.satlib.nexus.bdd

private val logger = mu.KotlinLogging.logger {}

internal class Cache<K, V : Any>(
    val name: String,
    val map: MutableMap<K, V> = mutableMapOf(), // WeakHashMap(),
) {
    var hits: Int = 0
        private set
    var misses: Int = 0
        private set

    inline fun getOrCompute(key: K, default: (K) -> V): V {
        val v = map[key]
        return if (v == null) {
            // logger.debug { "cache miss for '$name' on $key" }
            misses++
            default(key).also { map[key] = it }
        } else {
            // logger.debug { "cache hit for '$name' on $key" }
            hits++
            v
        }
    }
}
