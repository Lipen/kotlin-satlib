package com.github.lipen.satlib.nexus.aig

fun Aig.cone(id: Int): List<Int> {
    // Returns a list of node ids

    val queue: ArrayDeque<Int> = ArrayDeque(listOf(id))
    val cone: MutableSet<Int> = mutableSetOf()

    while (queue.isNotEmpty()) {
        val x = queue.removeFirst()
        if (cone.add(x)) {
            queue.addAll(children(x).map { it.id })
        }
    }

    return cone.toList()
}

// Returns a list of input node ids
fun Aig.inputSupport(id: Int): List<Int> = cone(id).filter { it in inputIds }

fun Aig.shadow(id: Int): List<Int> {
    // Returns a list of node ids

    val queue: ArrayDeque<Int> = ArrayDeque(listOf(id))
    val shadow: MutableSet<Int> = mutableSetOf()

    while (queue.isNotEmpty()) {
        val x = queue.removeFirst()
        if (shadow.add(x)) {
            queue.addAll(parents(x).map { it.id })
        }
    }

    return shadow.toList()
}

// Returns a list of output node ids
fun Aig.outputSupport(id: Int): List<Int> = shadow(id).filter { it in outputIds }
