package com.github.lipen.satlib.nexus.utils

/**
 * Returns the cartesian product of iterables.
 * Resulting tuples (as `List<T>`) are emitted lazily in lexicographic sort order.
 */
internal fun <T> Collection<Iterable<T>>.cartesianProduct(): Sequence<List<T>> =
    when (size) {
        0 -> emptySequence()
        else -> fold(sequenceOf(listOf())) { acc, iterable ->
            acc.flatMap { list ->
                iterable.asSequence().map { element -> list + element }
            }
        }
    }
