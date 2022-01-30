package com.github.lipen.satlib.nexus.utils

import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

fun <T> toposort(deps: Map<T, Collection<T>>): Sequence<List<T>> = sequence {
    logger.debug { "Performing a topological sort" }

    // Local mutable data
    val data: MutableMap<T, MutableList<T>> = deps.mapValuesTo(mutableMapOf()) { (_, v) -> v.toMutableList() }

    // Find all items without explicit deps and add them explicitly to the map
    for (item in (deps.values.flatten() - deps.keys)) {
        data[item] = mutableListOf()
    }

    while (true) {
        // New layer is a list of items without dependencies
        val layer = data.mapNotNull { (id, dep) -> if (dep.isEmpty()) id else null }

        // New layer can be empty in two cases:
        //  (1) `data` is empty (this is OK)
        //  (2) there is a circular dependency in `data` (we check for it outside the loop)
        if (layer.isEmpty()) break

        // Return
        yield(layer)

        // Remove keys without deps (the last layer)
        data.keys.removeAll(layer)
        // Reduce deps
        data.values.forEach { it.removeAll(layer) }
    }

    check(data.isEmpty()) { "Circular dependency detected: $data" }
}

fun main() {
    val deps = mapOf(
        1 to listOf(2, 3),
        2 to listOf(3, 4),
        3 to emptyList(),
        4 to emptyList(),
    )
    for ((i, layer) in toposort(deps).withIndex()) {
        println("Layer #${i + 1}: ${layer.map { mapOf(it to deps[it]) }}")
    }
}
