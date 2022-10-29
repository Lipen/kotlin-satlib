@file:Suppress("LocalVariableName")

package com.github.lipen.satlib.nexus

import com.github.lipen.satlib.nexus.aig.Aig
import com.github.lipen.satlib.nexus.aig.parseAig
import com.github.lipen.satlib.nexus.bdd.BDD
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

private fun buildBddFromAig(
    aig: Aig,
) {
    logger.info("Building BDD from AIG...")

    val bdd = BDD(storageCapacity = 1 shl 24)
    logger.info { "BDD: $bdd" }

    val id2node: MutableMap<Int, BddRef> = mutableMapOf() // {id: BDD}
    val gateBdd: MutableMap<Int, BddRef> = mutableMapOf() // {id: BDD}
    val aliases: MutableList<Int> = mutableListOf()
    val doGC = false
    var maxLastBddSize = 100_000

    println("=== Building BDD nodes for inputs...")
    for ((x0, inputId) in aig.inputIds.withIndex()) {
        val node = bdd.mkVar(x0 + 1)
        id2node[inputId] = node
        gateBdd[inputId] = node
        println("gateBdd[$inputId] = $node")
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
            val node: BddRef = bdd.applyAnd(left, right)
            id2node[id] = node
            gateBdd[id] = node
            // if (bdd.size(node) > 1_000) {
            //     println("Replacing large BDD with a variable $id")
            //     id2node[id] = bdd.mkVar(id)
            //     aliases.add(id)
            // }
            println(
                "gateBdd[$id] = $node (size=${bdd.size(node)})" +
                    ", gate=$gate" +
                    ", left=$left (size=${bdd.size(left)})" +
                    ", right=$right (size=${bdd.size(right)})" +
                    ", time=%.3fs"
                        .format(secondsSince(timeGateStart))
            )
        }
        println("Built layer $layerId/${aig.layers.size - 1} in %.3fs".format(secondsSince(timeLayerStart)))

        if (doGC && bdd.realSize > maxLastBddSize) {
            println("Building front...")
            val seen: MutableList<Int> = mutableListOf()
            val front: MutableList<Int> = mutableListOf()
            for (prevLayerId in (1..layerId).reversed()) {
                val prevLayer = aig.layers[prevLayerId]
                for (id in prevLayer) {
                    if (aig.parents(id).isEmpty()) {
                        // skip gates without parents
                    } else if (aig.parents(id).all { it.id in front || it.id in seen }) {
                        // skip gates whose parents (all) are already in front
                    } else {
                        // println("Adding ${aig.andGate(id)} from layer $prevLayerId to front because it has parents=${aig.parents(id)}")
                        front.add(id)
                    }
                    seen.add(id)
                }
                // println("Building front: after processing layer $prevLayerId, front=$front")
            }
            // println("Built front of size=${front.size}: ${front.sorted()}")
            println("Built front of size=${front.size}")

            println("Collecting garbage...")
            val timeGCStart = timeNow()
            bdd.collectGarbage(
                front.map { id2node[it]!! } +
                    aig.inputIds.map { id2node[it]!! } +
                    aliases.map { id2node[it]!! } +
                    aliases.map { gateBdd[it]!! }
            )
            println("GC in %.3fs".format(secondsSince(timeGCStart)))

            for (id in id2node.keys.toList()) {
                if (id !in aig.inputIds && id !in front && id !in aliases) {
                    id2node.remove(id)
                }
            }
        }

        maxLastBddSize = (max(maxLastBddSize, bdd.size) * 0.5).roundToInt()
        println("BDD.size = ${bdd.size}, BDD.realSize = ${bdd.realSize}, maxLastBddSize = $maxLastBddSize")

        println("Done processing layer $layerId/${aig.layers.size - 1} in %.3fs".format(secondsSince(timeLayerStart)))
        println()
    }

    if (doGC) {
        println("Collecting garbage...")
        val timeGCStart = timeNow()
        bdd.collectGarbage(aig.outputIds.map { id2node[it]!! } + aig.inputIds.map { id2node[it]!! } + aliases.map { id2node[it]!! } + aliases.map { gateBdd[it]!! })
        println("GC in %.3fs".format(secondsSince(timeGCStart)))

        for (id in id2node.keys.toList()) {
            if (id !in aig.inputIds && id !in aig.outputIds && id !in aliases) {
                id2node.remove(id)
            }
        }
    }

    logger.info { "= Aliases: (${aliases.size})" }
    for (id in aliases) {
        val node = gateBdd[id]!!
        logger.info { "Alias for $id = $node (size=${bdd.size(node)})" }
    }

    logger.info { "= Outputs: (${aig.outputs.size})" }
    for (output in aig.outputs) {
        val node = id2node[output.id]!!.let { if (output.negated) -it else it }
        logger.info { "Output ${output.id} = $node (size=${bdd.size(node)})" }
    }

    logger.info { "BDD.size = ${bdd.size}, BDD.realSize = ${bdd.realSize}" }
}

fun main() {
    val timeStart = timeNow()

    // val filename = "data/instances/BubbleSort/fraag/BubbleSort_3_2.aag"
    // val filename = "data/instances/BubbleSort/fraag/BubbleSort_7_4.aag"
    val filename = "data/instances/miters/fraag/BvP_5_4-aigmiter.aag"
    logger.info { "filename = $filename" }
    val aig = parseAig(filename)
    logger.info { "aig = $aig" }
    buildBddFromAig(aig)

    logger.info("All done in %.3f s".format(secondsSince(timeStart)))
}
