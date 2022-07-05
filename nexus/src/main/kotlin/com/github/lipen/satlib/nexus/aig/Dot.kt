package com.github.lipen.satlib.nexus.aig

@Suppress("LocalVariableName")
fun convertAigToDot(
    aig: Aig,
    rankByLayers: Boolean = true,
    eqIds: List<Pair<Int, Int>> = emptyList(),
    nodeLabel: Map<Int, String> = emptyMap(),
    nodeAddStyle: Map<Int, String> = emptyMap(),
): Sequence<String> = sequence {
    val STYLE_PI = "shape=invtriangle,color=blue" // Primary Input style
    val STYLE_PO = "shape=triangle,color=blue" // Primary Output style
    val STYLE_AND = "shape=oval" // And gate style
    val STYLE_INPUT = "shape=box" // Input gate style
    val STYLE_EDGE = "arrowhead=none" // Positive edge style
    val STYLE_EDGE_NEGATED = "arrowhead=none,style=dashed" // Complemented edge style

    yield("digraph {")

    yield("// Primary Inputs")
    yield("{ rank=sink")
    for (i in aig.inputs.indices) {
        yield("  i$i [$STYLE_PI];")
    }
    yield("}")

    val layers = aig.layers
    for ((i, layer) in layers.withIndex()) {
        yield("// Layer #${i + 1}")
        if (rankByLayers) {
            yield("{ rank=same")
        }
        for (id in layer) {
            val node = aig.node(id)
            val style = when (node) {
                is AigInput -> STYLE_INPUT
                is AigAndGate -> STYLE_AND
            }
            // yield("  $id [$style]; // $node")
            val label = nodeLabel[id]
            val labelS = if (label == null) "" else ",label=\"${label.replace("\"", "\\\"")}\""
            val additionalStyle = nodeAddStyle[id]
            val addStyleS = if (additionalStyle == null) "" else ",$additionalStyle"
            yield("  $id [$style$labelS$addStyleS]; // $node")
        }
        if (rankByLayers) {
            yield("}")
        }
    }

    yield("// Primary Outputs")
    yield("{ rank=source")
    for (i in aig.outputs.indices) {
        yield("  o$i [$STYLE_PO];")
    }
    yield("}")

    yield("// Input connections")
    for ((i, node) in aig.inputs.withIndex()) {
        yield("  ${node.id} -> i$i [$STYLE_EDGE];")
    }

    yield("// Node connections")
    for (id in layers.flatten()) {
        val node = aig.node(id)
        for (child in node.children) {
            val style = if (child.negated) STYLE_EDGE_NEGATED else STYLE_EDGE
            yield("  $id -> ${child.id} [$style];")
        }
    }

    yield("// Output connections")
    for ((i, node) in aig.outputs.withIndex()) {
        val style = if (node.negated) STYLE_EDGE_NEGATED else STYLE_EDGE
        yield("  o$i -> ${node.id} [$style];")
    }

    if (eqIds.isNotEmpty()) {
        yield("// Equivalent gates connections")
        for ((a, b) in eqIds) {
            yield("  $a -> $b [arrowhead=none,color=red,penwidth=2];")
        }
    }

    yield("}")
}

@Suppress("LocalVariableName", "UnnecessaryVariable")
fun convertTwoAigsToDot(
    aigLeft: Aig,
    aigRight: Aig,
    rankByLayers: Boolean = true,
    inputsOnTop: Boolean = false,
    nodeLabelLeft: Map<Int, String> = emptyMap(),
    nodeLabelRight: Map<Int, String> = emptyMap(),
    nodeAddStyleLeft: Map<Int, String> = emptyMap(),
    nodeAddStyleRight: Map<Int, String> = emptyMap(),
): Sequence<String> = sequence {
    require(aigLeft.inputs == aigRight.inputs) {
        "Inputs of both AIGs must be equal"
    }
    require(aigLeft.outputs.size == aigRight.outputs.size) {
        "AIGs must have equal number of outputs (L:${aigLeft.outputs}, R:${aigRight.outputs})"
    }

    val STYLE_PI = "shape=invtriangle,color=blue" // Primary Input style
    val STYLE_PO = "shape=triangle,color=blue" // Primary Output style
    val STYLE_AND = "shape=oval" // And gate style
    val STYLE_INPUT = "shape=box" // Input gate style
    val STYLE_EDGE = "arrowhead=none" // Positive edge style
    val STYLE_EDGE_NEGATED = "arrowhead=none,style=dashed" // Complemented edge style
    val PREFIX_LEFT = "L"
    val PREFIX_RIGHT = "R"

    yield("digraph {")
    yield("  rankdir=${if (inputsOnTop) "BT" else "TB"};")

    yield("// Primary Inputs")
    yield("{ rank=sink; rankdir=LR;")
    for (i in aigLeft.inputs.indices) {
        yield("  i$i [$STYLE_PI];")
    }
    yield("  edge[style=invis];")
    yield("  " + aigLeft.inputs.indices.joinToString(" -> ") { i -> "i$i" } + ";")
    yield("}")

    val layersLeft = aigLeft.layers
    val layersRight = aigRight.layers

    if (rankByLayers) {
        yield("// Layer #1")
        yield("{ rank=same")
        for (id in layersLeft.first()) {
            val node = aigLeft.node(id)
            check(node is AigInput)
            val style = STYLE_INPUT
            val label = nodeLabelLeft[id]
            val labelS = if (label == null) "" else ",label=\"${label.replace("\"", "\\\"")}\""
            val additionalStyle = nodeAddStyleLeft[id]
            val addStyleS = if (additionalStyle == null) "" else ",$additionalStyle"
            yield("  ${PREFIX_LEFT}$id [$style$labelS$addStyleS]; // $node")
        }
        for (id in layersRight.first()) {
            val node = aigRight.node(id)
            check(node is AigInput)
            val style = STYLE_INPUT
            val label = nodeLabelRight[id]
            val labelS = if (label == null) "" else ",label=\"${label.replace("\"", "\\\"")}\""
            val additionalStyle = nodeAddStyleRight[id]
            val addStyleS = if (additionalStyle == null) "" else ",$additionalStyle"
            yield("  ${PREFIX_RIGHT}$id [$style$labelS$addStyleS]; // $node")
        }
        yield("}")
    }

    for ((i, layer) in layersLeft.withIndex()) {
        if (rankByLayers && i == 0) continue // first layer already processed above
        yield("// Layer (${PREFIX_LEFT}) #${i + 1}")
        if (rankByLayers) {
            yield("{ rank=same")
        }
        for (id in layer) {
            val node = aigLeft.node(id)
            val style = when (node) {
                is AigInput -> STYLE_INPUT
                is AigAndGate -> STYLE_AND
            }
            val label = nodeLabelLeft[id]
            val labelS = if (label == null) "" else ",label=\"${label.replace("\"", "\\\"")}\""
            val additionalStyle = nodeAddStyleLeft[id]
            val addStyleS = if (additionalStyle == null) "" else ",$additionalStyle"
            yield("  ${PREFIX_LEFT}$id [$style$labelS$addStyleS]; // $node")
        }
        if (rankByLayers) {
            yield("}")
        }
    }
    for ((i, layer) in layersRight.withIndex()) {
        if (rankByLayers && i == 0) continue // first layer already processed above
        yield("// Layer (${PREFIX_RIGHT}) #${i + 1}")
        if (rankByLayers) {
            yield("{ rank=same")
        }
        for (id in layer) {
            val node = aigRight.node(id)
            val style = when (node) {
                is AigInput -> STYLE_INPUT
                is AigAndGate -> STYLE_AND
            }
            val label = nodeLabelRight[id]
            val labelS = if (label == null) "" else ",label=\"${label.replace("\"", "\\\"")}\""
            val additionalStyle = nodeAddStyleRight[id]
            val addStyleS = if (additionalStyle == null) "" else ",$additionalStyle"
            yield("  ${PREFIX_RIGHT}$id [$style$labelS$addStyleS]; // $node")
        }
        if (rankByLayers) {
            yield("}")
        }
    }

    yield("// Primary Outputs")
    yield("{ rank=source; rankdir=LR;")
    for (i in aigLeft.outputs.indices) {
        yield("  ${PREFIX_LEFT}o$i [$STYLE_PO];")
    }
    for (i in aigLeft.outputs.indices) {
        yield("  ${PREFIX_RIGHT}o$i [$STYLE_PO];")
    }
    yield("  edge[style=invis];")
    yield("  " + aigLeft.outputs.indices.joinToString(" -> ") { i -> "${PREFIX_LEFT}o$i" } + ";")
    yield("  ${PREFIX_LEFT}o${aigLeft.outputs.lastIndex} -> ${PREFIX_RIGHT}o0;")
    yield("  " + aigRight.outputs.indices.joinToString(" -> ") { i -> "${PREFIX_RIGHT}o$i" } + ";")
    yield("}")

    yield("// Input connections")
    for ((i, node) in aigLeft.inputs.withIndex()) {
        yield("  ${PREFIX_LEFT}${node.id} -> i$i [$STYLE_EDGE];")
    }
    for ((i, node) in aigRight.inputs.withIndex()) {
        yield("  ${PREFIX_RIGHT}${node.id} -> i$i [$STYLE_EDGE];")
    }

    yield("// Node connections")
    for (id in layersLeft.flatten()) {
        val node = aigLeft.node(id)
        for (child in node.children) {
            val style = if (child.negated) STYLE_EDGE_NEGATED else STYLE_EDGE
            yield("  ${PREFIX_LEFT}$id -> ${PREFIX_LEFT}${child.id} [$style];")
        }
    }
    for (id in layersRight.flatten()) {
        val node = aigRight.node(id)
        for (child in node.children) {
            val style = if (child.negated) STYLE_EDGE_NEGATED else STYLE_EDGE
            yield("  ${PREFIX_RIGHT}$id -> ${PREFIX_RIGHT}${child.id} [$style];")
        }
    }

    yield("// Output connections")
    for ((i, node) in aigLeft.outputs.withIndex()) {
        val style = if (node.negated) STYLE_EDGE_NEGATED else STYLE_EDGE
        yield("  ${PREFIX_LEFT}o$i -> ${PREFIX_LEFT}${node.id} [$style];")
    }
    for ((i, node) in aigRight.outputs.withIndex()) {
        val style = if (node.negated) STYLE_EDGE_NEGATED else STYLE_EDGE
        yield("  ${PREFIX_RIGHT}o$i -> ${PREFIX_RIGHT}${node.id} [$style];")
    }

    yield("}")
}
