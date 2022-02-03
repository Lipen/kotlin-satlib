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

fun Aig.inputSupport(id: Int): List<Int> {
    // Returns a list of input node ids
    return cone(id).filter { it in inputIds }
}

fun Aig.shadow(id: Int): List<Int> {
    // Returns a list of node ids

    val queue: ArrayDeque<Int> = ArrayDeque(listOf(id))
    val shadow: MutableSet<Int> = mutableSetOf()

    while (queue.isNotEmpty()) {
        val x = queue.removeFirst()
        if (shadow.add(x)) {
            queue.addAll(parentsTable.getValue(x))
        }
    }

    return shadow.toList()
}

fun Aig.outputSupport(id: Int): List<Int> {
    // Returns a list of output node ids
    return shadow(id).filter { it in outputIds }
}
