@file:Suppress("LocalVariableName")

package com.github.lipen.satlib.nexus

import com.github.lipen.satlib.nexus.aig.Aig
import com.github.lipen.satlib.nexus.aig.parseAig
import com.github.lipen.satlib.nexus.bdd.BDD
import com.github.lipen.satlib.nexus.utils.secondsSince
import com.github.lipen.satlib.nexus.utils.timeNow
import mu.KotlinLogging
import com.github.lipen.satlib.nexus.aig.Ref as AigRef
import com.github.lipen.satlib.nexus.bdd.Ref as BddRef

private val logger = KotlinLogging.logger {}

@Suppress("unused", "ObjectPropertyName")
private val _aigRef: AigRef? = null

@Suppress("unused", "ObjectPropertyName")
private val _bddRef: BddRef? = null

private fun BDD.mkVar(ref: AigRef): BddRef {
    val v = mkVar(ref.id)
    return if (ref.negated) -v else v
}

private data class PseudoNode(
    val id: Int,
    val children: List<AigRef>,
)

fun buildBddFromAig(
    aig: Aig,
) {
    logger.info("Building BDD from AIG...")

    logger.info { "AIG layers:" }
    for ((i, layer) in aig.layers.withIndex()) {
        logger.info { "#$i: $layer" }
    }

    val id2pseudoId: MutableMap<Int, Int> = mutableMapOf()
    val pseudoId2id: MutableMap<Int, Int> = mutableMapOf()

    val pseudoMapping: MutableMap<Int, PseudoNode> = mutableMapOf()
    var lastPseudoId = 0
    val pseudoLayers: List<MutableList<Int>> = aig.layers.indices.map { mutableListOf() }
    val identityAbove: MutableMap<Int, Int> = mutableMapOf()

    fun getOrCreateIdentity(level: Int, forId: Int): Int {
        if (forId in pseudoLayers[level]) {
            return forId
        }
        val x = getOrCreateIdentity(level - 1, forId)
        return identityAbove.getOrPut(x) {
            val identityNode = PseudoNode(++lastPseudoId, listOf(AigRef(x, negated = false)))
            pseudoMapping[identityNode.id] = identityNode
            pseudoLayers[level].add(identityNode.id)
            identityNode.id
        }
    }

    for ((i, layer) in aig.layers.withIndex()) {
        logger.info { "Processing layer #$i, lastId = $lastPseudoId" }
        for (id in layer) {
            val pseudoChildren = aig.node(id).children.map { child ->
                AigRef(getOrCreateIdentity(i - 1, id2pseudoId.getValue(child.id)), child.negated)
            }
            val pseudoNode = PseudoNode(++lastPseudoId, pseudoChildren)
            logger.info { "Created new pseudo node for id=$id: $pseudoNode" }
            pseudoMapping[pseudoNode.id] = pseudoNode
            pseudoLayers[i].add(pseudoNode.id)
            id2pseudoId[id] = pseudoNode.id
            pseudoId2id[pseudoNode.id] = id
        }
    }

    logger.info { "Pseudo layers:" }
    for ((i, layer) in pseudoLayers.withIndex()) {
        // logger.info { "#$i: $layer" }
        // logger.info { "#$i: [${layer.joinToString(", ") {id -> "${pseudoMapping[id]}" } }]" }
        logger.info { "#$i: (total ${layer.size}) [" }
        for (id in layer) {
            logger.info { "  - ${pseudoMapping[id]} (original id: ${pseudoId2id[id]})" }
        }
        logger.info { "]" }
    }

    var lastBddId = 0
    val pseudoIdToBddId: MutableMap<Int, Int> = mutableMapOf()
    for (layer in pseudoLayers) {
        for (id in layer) {
            pseudoIdToBddId[id] = ++lastBddId
        }
    }

    fun convertPseudoId(id: Int): Int {
        return pseudoIdToBddId.getValue(id)
    }

    fun convertPseudoAigRef(ref: AigRef): AigRef {
        val bddId = convertPseudoId(ref.id)
        return AigRef(bddId, ref.negated)
    }

    logger.info { "Pseudo layers (converted to BDD ids):" }
    for ((i, layer) in pseudoLayers.withIndex()) {
        logger.info { "#$i (total ${layer.size}): ${layer.map { id -> convertPseudoId(id) }}" }
    }

    val bdd = BDD(storageCapacity = 1 shl 24)
    logger.info { "BDD: $bdd" }

    val timeStart = timeNow()
    val nodeB: MutableMap<Int, BddRef> = mutableMapOf() // {id: BDD}

    for ((id, node) in pseudoMapping) {
        val timeStartBuildNode = timeNow()
        // logger.info { "Processing $node" }
        val f = when (node.children.size) {
            0 -> {
                // logger.info { "Input node => do nothing" }
                continue
            }
            1 -> {
                // logger.info { "Identity node" }
                val child = node.children[0]
                val f = bdd.applyEq(
                    bdd.mkVar(convertPseudoId(id)),
                    bdd.mkVar(convertPseudoAigRef(child))
                )
                // logger.info { "f = ID($child) = $f" }
                f
            }
            2 -> {
                // logger.info { "AND node" }
                val left = node.children[0]
                val right = node.children[1]
                val g = bdd.applyAnd(
                    bdd.mkVar(convertPseudoAigRef(left)),
                    bdd.mkVar(convertPseudoAigRef(right))
                )
                val f = bdd.applyEq(bdd.mkVar(convertPseudoId(id)), g)
                // logger.info { "f = ($id == AND($left, $right)) = $f" }
                f
            }
            else -> error("Bad number of children: ${node.children.size}")
        }
        nodeB[convertPseudoId(id)] = f
        logger.info {
            "[%.3fs] Processed $node in %.3fs. (size(f) = ${bdd.descendants(f).size}, bdd.size=${bdd.size})"
                .format(
                    secondsSince(timeStart),
                    secondsSince(timeStartBuildNode)
                )
        }
    }

    logger.info { "=".repeat(42) }

    logger.info { "Merging all AND node definitions..." }
    var bddAnds = bdd.one
    for (node in pseudoMapping.values.filter { it.children.size == 2 }.sortedBy { convertPseudoId(it.id) }) {
        val timeStartAdd = timeNow()
        val bddId = convertPseudoId(node.id)
        val b = nodeB.getValue(bddId)
        bddAnds = bdd.applyAnd(bddAnds, b)
        logger.info {
            "Added $node (bdd: $bddId, ${node.children.map { convertPseudoAigRef(it) }}) with b=$b in %.2fs, now bddAnds=$bddAnds, size(bddAnds)=${
                bdd.descendants(
                    bddAnds
                ).size
            }, bdd.size=${bdd.size}"
                .format(secondsSince(timeStartAdd))
        }
    }

    logger.info { "Merging all identity node definitions..." }
    var bddIds = bdd.one
    for (node in pseudoMapping.values.filter { it.children.size == 1 }.sortedBy { convertPseudoId(it.id) }) {
        val timeStartAdd = timeNow()
        val bddId = convertPseudoId(node.id)
        val b = nodeB.getValue(bddId)
        bddIds = bdd.applyAnd(bddIds, b)
        logger.info {
            "Added $node (bdd: $bddId, ${node.children.map { convertPseudoAigRef(it) }}) with b=$b in %.2fs, now bddIds=$bddIds, size(bddIds)=${
                bdd.descendants(
                    bddIds
                ).size
            }, bdd.size=${bdd.size}"
                .format(secondsSince(timeStartAdd))
        }
    }

    logger.info { "Final bddAnds=$bddAnds, size(bddAnds)=${bdd.descendants(bddAnds).size}" }
    logger.info { "Final bddIds=$bddIds, size(bddIds)=${bdd.descendants(bddIds).size}" }
    logger.info { "BDD.size = ${bdd.size}, BDD.realSize = ${bdd.realSize}" }

    // val Bs: MutableMap<Int, BddRef> = mutableMapOf()
    // val timeStart = timeNow()
    //
    // for ((i, layer) in pseudoLayers.withIndex()) {
    //     logger.info { "Processing pseudo-layer #$i" }
    //     val timeStartBuildB = timeNow()
    //     var B = bdd.one
    //     for (id in layer) {
    //         val timeStartProcessNode = timeNow()
    //         val node = pseudoMapping.getValue(id)
    //         // logger.info { "Processing $node" }
    //         when (node.children.size) {
    //             0 -> {
    //                 // logger.info { "Input node => do nothing" }
    //             }
    //             1 -> {
    //                 // logger.info { "Identity node" }
    //                 val child = node.children[0]
    //                 val f = bdd.applyEq(
    //                     bdd.mkVar(convertPseudoId(id)),
    //                     bdd.mkVar(convertPseudoAigRef(child))
    //                 )
    //                 // logger.info { "f = ID($child) = $f" }
    //                 B = bdd.applyAnd(B, f)
    //                 // logger.info { "B = $B" }
    //             }
    //             2 -> {
    //                 // logger.info { "AND node" }
    //                 val left = node.children[0]
    //                 val right = node.children[1]
    //                 val g = bdd.applyAnd(
    //                     bdd.mkVar(convertPseudoAigRef(left)),
    //                     bdd.mkVar(convertPseudoAigRef(right))
    //                 )
    //                 val f = bdd.applyEq(bdd.mkVar(convertPseudoId(id)), g)
    //                 // logger.info { "AND($left, $right) = $g" }
    //                 // logger.info { "f = ($id == AND($left, $right)) = $f" }
    //                 B = bdd.applyAnd(B, f)
    //                 // logger.info { "B = $B" }
    //             }
    //             else -> error("Bad number of children: ${node.children.size}")
    //         }
    //         logger.info {
    //             "[%.3fs] Processed $node in %.3fs. B[i=$i]=$B (size(B) = ${bdd.descendants(B).size}, bdd.size=${bdd.size})"
    //                 .format(
    //                     secondsSince(timeStart),
    //                     secondsSince(timeStartProcessNode)
    //                 )
    //         }
    //     }
    //     logger.info {
    //         "Built B[i=$i] = $B in %.3fs (size = ${bdd.descendants(B).size})"
    //             .format(secondsSince(timeStartBuildB))
    //     }
    //     Bs[i] = B
    //
    //     // logger.info { "Collecting garbage..." }
    //     // bdd.collectGarbage(roots = Bs.values)
    // }
    //
    // logger.info { "Bs:" }
    // for ((i, B) in Bs) {
    //     logger.info { "B[i = $i] = $B (size = ${bdd.descendants(B).size}" }
    // }
    // logger.info { "Total size of all Bs: ${bdd.descendants(Bs.values).size}" }
    // logger.info { "BDD.size = ${bdd.size}, BDD.realSize = ${bdd.realSize}" }
}

fun main() {
    val timeStart = timeNow()

    val filename = "data/instances/BubbleSort/fraag/BubbleSort_3_3.aag"
    logger.info { "filename = $filename" }
    val aig = parseAig(filename)
    logger.info { "aig = $aig" }
    buildBddFromAig(aig)

    logger.info("All done in %.3f s".format(secondsSince(timeStart)))
}
