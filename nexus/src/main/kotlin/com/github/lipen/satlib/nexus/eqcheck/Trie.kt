package com.github.lipen.satlib.nexus.eqcheck

import com.github.lipen.satlib.nexus.utils.toBinaryString

class Node(
    val cube: List<Boolean>,
    var left: Node? = null,
    var right: Node? = null,
) {
    val leaves: Int by lazy {
        if (isLeaf()) {
            1
        } else {
            (left?.leaves ?: 0) + (right?.leaves ?: 0)
        }
    }

    fun isLeaf(): Boolean = (left == null) && (right == null)

    fun dfs(): Sequence<Node> = sequence {
        yield(this@Node)
        if (left != null) yieldAll(left!!.dfs())
        if (right != null) yieldAll(right!!.dfs())
    }

    fun dfsLimited(limit: Int): Sequence<Node> = sequence {
        if (leaves > limit) {
            val left = left
            if (left != null) yieldAll(left.dfsLimited(limit))
            val right = right
            if (right != null) yieldAll(right.dfsLimited(limit))
        } else {
            yield(this@Node)
        }
    }

    fun pprint(n: Int = 0) {
        val indent = "  ".repeat(n)
        if (n > 0) {
            println(cube.toBinaryString() + " ($leaves leaves)")
        } else {
            println("root ($leaves leaves)")
        }
        if (left != null) {
            print("${indent}- 0:")
            left!!.pprint(n + 1)
        }
        if (right != null) {
            print("${indent}- 1:")
            right!!.pprint(n + 1)
        }
    }
}

fun buildTrie(cubes: Iterable<List<Boolean>>): Node {
    val root = Node(emptyList())

    for ((index, cube) in cubes.withIndex()) {
        if ((index + 1) % 10000 == 0) {
            println("Cube ${index + 1}/${cubes.count()}: ${cube.toBinaryString()}")
        }
        var current: Node = root
        for ((i, p) in cube.withIndex()) {
            // `p==false` corresponds to `left`
            // `p==true` corresponds to `right`
            when (p) {
                false -> {
                    if (current.left == null) {
                        current.left = Node(cube.slice(0..i))
                        // current.left = Node(current.cube + p)
                        // .also { println("New node with ${it.cube.toBinaryString()}") }
                    }
                    current = current.left!!
                }
                true -> {
                    if (current.right == null) {
                        current.right = Node(cube.slice(0..i))
                        // current.right = Node(current.cube + p)
                        // .also { println("New node with ${it.cube.toBinaryString()}") }
                    }
                    current = current.right!!
                }
            }
        }
    }

    return root
}

fun main() {
    val cubes = listOf(
        "1001",
        "0101",
        "0111",
        "0011",
    ).map {
        it.map { c ->
            when (c) {
                '0' -> false
                '1' -> true
                else -> error("Bad char '$c'")
            }
        }
    }
    val root = buildTrie(cubes)
    println("Trie:")
    root.pprint()

    println("DFS:")
    for (node in root.dfs()) {
        println("  - ${node.cube.toBinaryString()} (leaves: ${node.leaves})")
    }
}
