@file:Suppress("LocalVariableName")

package com.github.lipen.satlib.nexus

import com.github.lipen.satlib.nexus.aig.Aig
import com.github.lipen.satlib.nexus.aig.parseAig
import com.github.lipen.satlib.nexus.bdd.BDD
import com.github.lipen.satlib.nexus.utils.secondsSince
import com.github.lipen.satlib.nexus.utils.timeNow
import com.github.lipen.satlib.nexus.utils.withIndex
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

private fun buildBddFromAigs(
    aigLeft: Aig,
    aigRight: Aig,
) {
    logger.info("Building BDD from AIGs...")

    check(aigLeft.inputs == aigRight.inputs)

    val bdd = BDD(storageCapacity = 1 shl 24)
    logger.info { "BDD: $bdd" }

    var vars = aigLeft.inputs.size
    val doGC = false
    var maxLastBddSize = 100_000

    fun processAig(
        aig: Aig,
        id2node: MutableMap<Int, BddRef>, // {id: node}
        realBdd: MutableMap<Int, BddRef>, // {id: node}
        alias: MutableMap<Int, Int>, // {var: id}
    ) {
        println("= Building BDD nodes for inputs...")
        for ((x, inputId) in aig.inputIds.withIndex(start = 1)) {
            val node = bdd.mkVar(x)
            bdd.nonGarbage.add(node)
            id2node[inputId] = node
            realBdd[inputId] = node
            println("Node for input $x with id=$inputId is $node")
        }

        println("= Building BDD nodes for gates...")
        for ((layerId, layer) in aig.layers.withIndex().drop(1)) {
            println("= Layer $layerId/${aig.layers.size - 1} of size=${layer.size}")
            val timeLayerStart = timeNow()
            for (id in layer) {
                val timeGateStart = timeNow()
                val gate = aig.andGate(id)
                val left: BddRef = id2node[gate.left.id]!!.let { if (gate.left.negated) -it else it }
                val right: BddRef = id2node[gate.right.id]!!.let { if (gate.right.negated) -it else it }
                val node: BddRef = bdd.applyAnd(left, right)
                bdd.nonGarbage.add(node)
                realBdd[id] = node
                if (bdd.size(node) > 5_000) {
                    val v = ++vars
                    println("Replacing large BDD with a variable v=$v, id=$id")
                    val aliasedNode = bdd.mkVar(v)
                    bdd.nonGarbage.add(aliasedNode)
                    id2node[id] = aliasedNode
                    alias[v] = id
                } else {
                    id2node[id] = node
                }
                println(
                    "gateBdd[$id] = $node (size=${bdd.size(node)})" +
                        ", gate=$gate" +
                        ", left=$left (size=${bdd.size(left)})" +
                        ", right=$right (size=${bdd.size(right)})" +
                        ", time=%.3fs".format(secondsSince(timeGateStart))
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
                    front.map { id2node[it]!! } //+
                    // aig.inputIds.map { id2node[it]!! } +
                    // alias.values.map { id2node[it]!! } +
                    // alias.values.map { realBdd[it]!! }
                )
                println("GC in %.3fs".format(secondsSince(timeGCStart)))

                for (id in id2node.keys.toList()) {
                    if (id !in aig.inputIds && id !in front && id !in alias && id !in aig.outputIds) {
                        id2node.remove(id)
                    }
                }
            }

            maxLastBddSize = (max(maxLastBddSize, bdd.size) * 0.5).roundToInt()
            println("BDD.size = ${bdd.size}, BDD.realSize = ${bdd.realSize}, maxLastBddSize = $maxLastBddSize")

            println("Done processing layer $layerId/${aig.layers.size - 1} in %.3fs".format(secondsSince(timeLayerStart)))
            println()
        }
    }

    val id2nodeLeft: MutableMap<Int, BddRef> = mutableMapOf() // {id: BDD}
    val id2nodeRight: MutableMap<Int, BddRef> = mutableMapOf() // {id: BDD}
    val realBddLeft: MutableMap<Int, BddRef> = mutableMapOf() // {id: BDD}
    val realBddRight: MutableMap<Int, BddRef> = mutableMapOf() // {id: BDD}
    val aliasLeft: MutableMap<Int, Int> = mutableMapOf()
    val aliasRight: MutableMap<Int, Int> = mutableMapOf()

    println("=== LEFT")
    processAig(aigLeft, id2nodeLeft, realBddLeft, aliasLeft)
    println("=== RIGHT")
    processAig(aigRight, id2nodeRight, realBddRight, aliasRight)

    // println("Collecting garbage...")
    // val timeGCStart = timeNow()
    // bdd.collectGarbage((aigLeft.outputIds.map { id2nodeLeft[it]!! } + aigLeft.inputIds.map { id2nodeLeft[it]!! } + aliasesLeft.map { id2nodeLeft[it]!! }).distinct())
    // println("GC in %.3fs".format(secondsSince(timeGCStart)))
    //
    // for (id in id2nodeLeft.keys.toList()) {
    //     if (id !in aigLeft.inputIds && id !in aigLeft.outputIds && id !in aliasesLeft) {
    //         id2nodeLeft.remove(id)
    //     }
    // }

    for ((v, id) in aliasLeft) {
        val node: BddRef = realBddLeft[id]!!
        logger.info { "LEFT: Alias for v=$v with id=$id is $node (size=${bdd.size(node)})" }
    }
    for ((v, id) in aliasLeft) {
        val node: BddRef = realBddLeft[id]!!
        logger.info { "RIGHT: Alias for v=$v with id=$id is $node (size=${bdd.size(node)})" }
    }

    logger.info { "=== LEFT" }
    for ((i, output) in aigLeft.outputs.withIndex(start = 1)) {
        val node: BddRef = id2nodeLeft[output.id]!!.let { if (output.negated) -it else it }
        logger.info { "Output $i with ref=$output is $node (size=${bdd.size(node)})" }
    }
    logger.info { "=== RIGHT" }
    for ((i, output) in aigRight.outputs.withIndex(start = 1)) {
        val node: BddRef = id2nodeRight[output.id]!!.let { if (output.negated) -it else it }
        logger.info { "Output $i with ref=$output is $node (size=${bdd.size(node)})" }
    }
    logger.info { "=== COMPARISON of OUTPUTS" }
    for ((i, outs) in aigLeft.outputs.zip(aigRight.outputs).withIndex(start = 1)) {
        val (outputLeft, outputRight) = outs
        val nodeLeft: BddRef = id2nodeLeft[outputLeft.id]!!.let { if (outputLeft.negated) -it else it }
        val nodeRight: BddRef = id2nodeRight[outputRight.id]!!.let { if (outputRight.negated) -it else it }
        if (nodeLeft == nodeRight) {
            logger.info { "Output $i: $nodeLeft == $nodeRight" }
        } else {
            logger.warn { "Output $i: $nodeLeft != $nodeRight" }
        }
    }

    val xorOutputs = aigLeft.outputIds.zip(aigRight.outputIds).map { (l, r) ->
        val left: BddRef = id2nodeLeft[l]!!
        val right: BddRef = id2nodeRight[r]!!
        bdd.applyXor(left, right)
    }.toMutableList()
    bdd.nonGarbage.addAll(xorOutputs)
    logger.info { "=== XOR OUTPUTS BEFORE SUBSTITUTION" }
    for ((i, node: BddRef) in xorOutputs.withIndex(start = 1)) {
        logger.info { "Xor output $i with is $node (size=${bdd.size(node)})" }
    }

    // logger.info { "=== Back-substitute for LEFT:" }
    // for ((v, id) in aliasLeft.entries.reversed()) {
    //     val real = realBddLeft[id]!!
    //     logger.info { "Alias for v=$v with id=$id is $real (size=${bdd.size(real)})" }
    //     logger.info { "Pre-substitute node is ${id2nodeLeft[id]} (size=${bdd.size(id2nodeLeft[id]!!)})" }
    //     val node = bdd.compose(id2nodeLeft[id]!!, v, real)
    //     id2nodeLeft[id] = node
    //     logger.info { "Substituted $real for v=$v with id=$id: now node is $node (size=${bdd.size(node)})" }
    // }
    // logger.info { "=== Back-substitute for RIGHT:" }
    // for ((v, id) in aliasRight.entries.reversed()) {
    //     val real = realBddRight[id]!!
    //     logger.info { "Alias for v=$v with id=$id is $real (size=${bdd.size(real)})" }
    //     logger.info { "Pre-substitute node is ${id2nodeRight[id]} (size=${bdd.size(id2nodeRight[id]!!)})" }
    //     val node = bdd.compose(id2nodeRight[id]!!, v, real)
    //     id2nodeRight[id] = node
    //     logger.info { "Substituted $real for v=$v with id=$id: now node is $node (size=${bdd.size(node)})" }
    // }

    for ((i, xorOutput) in xorOutputs.withIndex(start = 1)) {
        var node: BddRef = xorOutput
        run {
            logger.info { "=== Back-substitute LEFT for xor-output $i with ref=$node:" }
            for ((v, id) in aliasLeft.entries.reversed()) {
                val real = realBddLeft[id]!!
                logger.info { "Alias for v=$v with id=$id is $real (size=${bdd.size(real)})" }
                node = bdd.compose(node, v, real)
                logger.info { "Substituted $real for v=$v with id=$id: now node is $node (size=${bdd.size(node)})" }
            }
        }
        run {
            logger.info { "=== Back-substitute RIGHT for xor-output $i with ref=$node:" }
            for ((v, id) in aliasRight.entries.reversed()) {
                val real = realBddRight[id]!!
                node = bdd.compose(node, v, real)
                logger.info { "Substituted $real for v=$v with id=$id: now node is $node (size=${bdd.size(node)})" }
            }
        }
        xorOutputs[i - 1] = node

        if (doGC) {
            println("Collecting garbage...")
            val timeGCStart = timeNow()
            // bdd.collectGarbage(xorOutputs + aliasLeft.values.map { id2nodeLeft[it]!! } + aliasLeft.values.map { realBddLeft[it]!! } + aliasRight.values.map { id2nodeRight[it]!! } + aliasRight.values.map { realBddRight[it]!! })
            bdd.collectGarbage(emptyList())
            println("GC in %.3fs".format(secondsSince(timeGCStart)))
        }
    }

    logger.info { "=== XOR OUTPUTS AFTER SUBSTITUTION" }
    for ((i, node: BddRef) in xorOutputs.withIndex(start = 1)) {
        logger.info { "Xor output $i with is $node (size=${bdd.size(node)})" }
    }

    logger.info { "BDD.size = ${bdd.size}, BDD.realSize = ${bdd.realSize}" }
}

fun main() {
    val timeStart = timeNow()

    val left = "BubbleSort"
    val right = "PancakeSort"
    val param = "5_4"
    val name = "BvP_$param"
    val aag = "fraag" // "aag" or "fraag"

    val nameLeft = "${left}_${param}"
    val nameRight = "${right}_${param}"
    val filenameLeft = "data/instances/$left/$aag/$nameLeft.aag"
    val filenameRight = "data/instances/$right/$aag/$nameRight.aag"

    val aigLeft = parseAig(filenameLeft)
    val aigRight = parseAig(filenameRight)

    buildBddFromAigs(aigLeft, aigRight)

    logger.info("All done in %.3f s".format(secondsSince(timeStart)))
}
