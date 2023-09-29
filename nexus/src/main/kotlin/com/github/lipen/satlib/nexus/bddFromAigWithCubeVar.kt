@file:Suppress("LocalVariableName")

package com.github.lipen.satlib.nexus

import com.github.lipen.satlib.nexus.aig.Aig
import com.github.lipen.satlib.nexus.aig.parseAig
import com.github.lipen.satlib.nexus.bdd.BDD
import com.github.lipen.satlib.nexus.bdd.cube
import com.github.lipen.satlib.nexus.utils.secondsSince
import com.github.lipen.satlib.nexus.utils.timeNow
import mu.KotlinLogging
import kotlin.math.max
import kotlin.math.roundToInt
import com.github.lipen.satlib.nexus.aig.Ref as AigRef
import com.github.lipen.satlib.nexus.bdd.Ref as BddRef

private val logger = KotlinLogging.logger {}

@Suppress("unused", "ObjectPropertyName")
private val _aigRef: AigRef? = null

@Suppress("unused", "ObjectPropertyName")
private val _bddRef: BddRef? = null

private fun buildBddFromAigWithCube(
    aig: Aig,
) {
    logger.info("Building BDD from AIG with cube...")

    val bdd = BDD(storageCapacity = 1 shl 22)
    logger.info { "BDD: $bdd" }

    val id2node: MutableMap<Int, BddRef> = mutableMapOf() // {id: BDD}
    val id2var: MutableMap<Int, Int> = mutableMapOf()
    val doGC = true
    var maxLastBddSize = 100_000
    var numVars = 0

    println("=== Distributing BDD variables for gates...")
    for (layer in aig.layers.drop(1).asReversed()) {
        for (id in layer.asReversed()) {
            id2var[id] = ++numVars
            println("id2var[$id] = ${id2var[id]}")
        }
    }

    println("=== Distributing BDD variables for inputs...")
    for ((x0, inputId) in aig.inputIds.withIndex()) {
        val v = ++numVars
        id2var[inputId] = v
        println("id2var[$inputId] = $v for input x${x0 + 1}/${aig.inputIds.size}")
    }

    // println("=== Distributing BDD variables for gates...")
    // for (layer in aig.layers.drop(1)) {
    //     for (id in layer) {
    //         id2var[id] = ++numVars
    //         println("id2var[$id] = ${id2var[id]}")
    //     }
    // }

    println("=== Building BDD for cube...")
    val cube: BddRef = bdd.cube(aig.inputIds.map { id2var[it]!! })
    println("cube = $cube (size=${bdd.size(cube)}, var=${bdd.variable(cube)})")
    bdd.nonGarbage.add(cube)

    println("=== Building BDD nodes for inputs...")
    for (inputId in aig.inputIds) {
        val inputVar: BddRef = bdd.mkVar(id2var[inputId]!!)
        // val node: BddRef = bdd.applyAnd(inputVar, cube)
        val node: BddRef = inputVar
        // TODO: depend on cube: if x in cube, then node = x else node = 0
        id2node[inputId] = node
        println("id2node[$inputId] = $node (size=${bdd.size(node)}, var=${bdd.variable(node)})")
    }

    println("=== Building BDD nodes for gates...")
    for ((layerId, layer) in aig.layers.withIndex().drop(1)) {
        println("= Layer $layerId/${aig.layers.size - 1} of size=${layer.size}")
        val timeLayerStart = timeNow()
        for (id in layer) {
            val timeGateStart = timeNow()
            val gate = aig.andGate(id)

            // val left: BddRef = id2node[gate.left.id]!!.let { if (gate.left.negated) -it else it }
            // val right: BddRef = id2node[gate.right.id]!!.let { if (gate.right.negated) -it else it }
            // val node = bdd.applyAnd(left, right)

            val left: BddRef = id2node[gate.left.id]!!
            val right: BddRef = id2node[gate.right.id]!!
            val leftVar = bdd.mkVar(id2var[gate.left.id]!!).let { if (gate.left.negated) -it else it }
            val rightVar = bdd.mkVar(id2var[gate.right.id]!!).let { if (gate.right.negated) -it else it }
            val def: BddRef = bdd.applyEq(bdd.mkVar(id2var[id]!!), bdd.applyAnd(leftVar, rightVar))
            val node: BddRef = bdd.applyAnd(def, bdd.applyAnd(left, right))
            // val node: BddRef = bdd.applyAnd(bdd.applyAnd(bdd.applyAnd(def, cube), left), right)
            // val node: BddRef = bdd.applyAnd(bdd.applyAnd(bdd.applyAnd(def, left), right), cube)
            // val node: BddRef = bdd.applyAnd(bdd.applyAnd(def, left), right)

            id2node[id] = node
            println(
                "id2node[$id] = $node (size=${bdd.size(node)}, var=${bdd.variable(node)})" +
                    ", gate=$gate" +
                    ", left=$left (size=${bdd.size(left)})" +
                    ", right=$right (size=${bdd.size(right)})" +
                    ", time=%.3fs"
                        .format(secondsSince(timeGateStart))
            )
        }
        println("Built layer $layerId/${aig.layers.size - 1} in %.3fs".format(secondsSince(timeLayerStart)))

        if (doGC && bdd.realSize > maxLastBddSize) {
            println("Collecting garbage...")
            val timeGCStart = timeNow()
            bdd.collectGarbage(id2node.values)
            println("GC in %.3fs".format(secondsSince(timeGCStart)))
        }

        maxLastBddSize = (max(maxLastBddSize, bdd.size) * 0.5).roundToInt()
        println("BDD.size = ${bdd.size}, BDD.realSize = ${bdd.realSize}, maxLastBddSize = $maxLastBddSize")

        println("Done processing layer $layerId/${aig.layers.size - 1} in %.3fs".format(secondsSince(timeLayerStart)))
        println()
    }

    if (doGC) {
        println("Collecting garbage...")
        val timeGCStart = timeNow()
        bdd.collectGarbage(aig.outputIds.map { id2node[it]!! })
        println("GC in %.3fs".format(secondsSince(timeGCStart)))
    }

    logger.info { "= Outputs: (${aig.outputs.size})" }
    for (output in aig.outputs) {
        val node = id2node[output.id]!!.let { if (output.negated) -it else it }
        logger.info { "Output $output = $node (size=${bdd.size(node)})" }
        if (bdd.size(node) < 1000) {
            println("$node = ${bdd.toBracketString(node)}")
        }
    }
    logger.info {
        "Total size of all outputs: ${
            bdd.descendants(aig.outputs.map { out ->
                id2node[out.id]!!.let { if (out.negated) -it else it }
            }).size
        }"
    }

    logger.info { "BDD.size = ${bdd.size}, BDD.realSize = ${bdd.realSize}" }
}

fun main() {
    val timeStart = timeNow()

    // val filename = "data/instances/BubbleSort/fraag/BubbleSort_3_2.aag"
    // val filename = "data/instances/BubbleSort/fraag/BubbleSort_4_3.aag"
    // val filename = "data/instances/BubbleSort/fraag/BubbleSort_7_4.aag"
    val filename = "data/instances/miters/fraag/BvP_4_3-aigmiter.aag"
    // val filename = "data/instances/miters/fraag/BvP_4_4-aigmiter.aag"
    // val filename = "data/instances/miters/fraag/BvP_5_4-aigmiter.aag"
    // val filename = "data/instances/miters/fraag/BvP_6_4-aigmiter.aag"
    // val filename = "data/instances/miters/fraag/BvP_7_4-aigmiter.aag"
    // val filename = "data/instances/miters/fraag/BvP_8_4-aigmiter.aag"
    // val filename = "data/instances/IWLS93/aag/C6288.aag" // 16-bit multiplier
    logger.info { "filename = $filename" }
    val aig = parseAig(filename)
    logger.info { "aig = $aig" }

    buildBddFromAigWithCube(aig)

    logger.info("All done in %.3f s".format(secondsSince(timeStart)))
}
