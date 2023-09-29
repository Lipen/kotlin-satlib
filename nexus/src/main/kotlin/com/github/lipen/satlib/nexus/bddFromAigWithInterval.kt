@file:Suppress("LocalVariableName")

package com.github.lipen.satlib.nexus

import com.github.lipen.satlib.nexus.aig.Aig
import com.github.lipen.satlib.nexus.aig.parseAig
import com.github.lipen.satlib.nexus.bdd.BDD
import com.github.lipen.satlib.nexus.utils.pow
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

private fun buildBddFromAigWithInterval(
    aig: Aig,
) {
    logger.info("Building BDD from AIG with interval...")

    with(BDD(storageCapacity = 1 shl 23)) {
        logger.info { "BDD: $this" }

        val id2node: MutableMap<Int, BddRef> = mutableMapOf() // {id: BDD}
        val id2var: MutableMap<Int, Int> = mutableMapOf()
        val doGC = false
        var maxLastBddSize = 100_000
        var numVars = 0

        // println("=== Distributing BDD variables for gates...")
        // for (layer in aig.layers.drop(1).asReversed()) {
        //     for (id in layer.asReversed()) {
        //         id2var[id] = ++numVars
        //         println("id2var[$id] = ${id2var[id]}")
        //     }
        // }

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

        println("=== Building BDD for interval...")
        val ps = aig.inputIds.map { id2var[it]!! }
        println("ps = $ps")
        println("ps.size = ${ps.size}")
        check(ps.size <= 32)
        val coeffs = (1..ps.size).map { i -> 2.pow(ps.size - i) }
        println("coeffs = $coeffs")
        val lower = 0
        val upper = 0
        // val lower = 2.pow(ps.size) - 1
        // val upper = 2.pow(ps.size) - 1
        // val upper = 2.pow(ps.size - 1) - 1
        // val upper = 2.pow(ps.size) - 1
        val intervalLower: BddRef = pseudoBooleanGreaterThenOrEqual(coeffs, ps, lower)
        val intervalUpper: BddRef = pseudoBooleanLessThenOrEqual(coeffs, ps, upper)
        val interval = applyAnd(intervalLower, intervalUpper)
        println("intervalLower = $intervalLower (size=${size(intervalLower)}, var=${variable(intervalLower)})")
        println("intervalUpper = $intervalUpper (size=${size(intervalUpper)}, var=${variable(intervalUpper)})")
        println("interval = $interval (size=${size(interval)}, var=${variable(interval)})")
        println("interval = ${toBracketString(interval)}")
        nonGarbage.add(interval)

        println("=== Building BDD nodes for inputs...")
        for (inputId in aig.inputIds) {
            val inputVar: BddRef = mkVar(id2var[inputId]!!)
            val node: BddRef = applyAnd(inputVar, interval)
            // val node: BddRef = inputVar
            id2node[inputId] = node
            println("id2node[$inputId] = $node (size=${size(node)}, var=${variable(node)})")
        }

        println("=== Building BDD nodes for gates...")
        for ((layerId, layer) in aig.layers.withIndex().drop(1)) {
            println("= Layer $layerId/${aig.layers.size - 1} of size=${layer.size}")
            val timeLayerStart = timeNow()
            for (id in layer) {
                val timeGateStart = timeNow()
                val gate = aig.andGate(id)

                val left: BddRef = id2node[gate.left.id]!!.let { if (gate.left.negated) -it else it }
                val right: BddRef = id2node[gate.right.id]!!.let { if (gate.right.negated) -it else it }
                val node = applyAnd(left, right)

                id2node[id] = node
                println(
                    "id2node[$id] = $node (size=${size(node)}, var=${variable(node)})" +
                        ", gate=$gate" +
                        ", left=$left (size=${size(left)})" +
                        ", right=$right (size=${size(right)})" +
                        ", time=%.3fs"
                            .format(secondsSince(timeGateStart))
                )
            }
            println("Built layer $layerId/${aig.layers.size - 1} in %.3fs".format(secondsSince(timeLayerStart)))

            if (doGC && realSize > maxLastBddSize) {
                println("Collecting garbage...")
                val timeGCStart = timeNow()
                collectGarbage(id2node.values)
                println("GC in %.3fs".format(secondsSince(timeGCStart)))
            }

            maxLastBddSize = (max(maxLastBddSize, size) * 0.5).roundToInt()
            println("size = ${size}, realSize = ${realSize}, maxLastBddSize = $maxLastBddSize")

            println("Done processing layer $layerId/${aig.layers.size - 1} in %.3fs".format(secondsSince(timeLayerStart)))
            println()
        }

        // if (doGC) {
        //     println("Collecting garbage...")
        //     val timeGCStart = timeNow()
        //     collectGarbage(id2node.values)
        //     println("GC in %.3fs".format(secondsSince(timeGCStart)))
        // }

        logger.info { "= Outputs: (${aig.outputs.size})" }
        for (output in aig.outputs) {
            val node = id2node[output.id]!!.let { if (output.negated) -it else it }
            logger.info { "Output $output = $node (size=${size(node)})" }
            if (size(node) < 2000) {
                println("$node = ${toBracketString(node)}")
            }
        }
        logger.info {
            "Total size of all outputs: ${
                descendants(aig.outputs.map { out ->
                    id2node[out.id]!!.let { if (out.negated) -it else it }
                }).size
            }"
        }

        logger.info { "BDD.size = $size, BDD.realSize = $realSize" }
    }
}

fun main() {
    val timeStart = timeNow()

    val filename = "data/instances/BubbleSort/fraag/BubbleSort_3_2.aag"
    // val filename = "data/instances/BubbleSort/fraag/BubbleSort_4_3.aag"
    // val filename = "data/instances/BubbleSort/fraag/BubbleSort_7_4.aag"
    // val filename = "data/instances/miters/fraag/BvP_4_3-aigmiter.aag"
    // val filename = "data/instances/miters/fraag/BvP_4_4-aigmiter.aag"
    // val filename = "data/instances/miters/fraag/BvP_5_4-aigmiter.aag"
    // val filename = "data/instances/miters/fraag/BvP_6_4-aigmiter.aag"
    // val filename = "data/instances/miters/fraag/BvP_7_4-aigmiter.aag"
    // val filename = "data/instances/miters/fraag/BvP_8_4-aigmiter.aag"
    // val filename = "data/instances/IWLS93/aag/C6288.aag" // 16-bit multiplier
    // val filename = "data/aag/g2.aag"
    logger.info { "filename = $filename" }
    val aig = parseAig(filename)
    logger.info { "aig = $aig" }

    buildBddFromAigWithInterval(aig)

    logger.info("All done in %.3f s".format(secondsSince(timeStart)))
}
