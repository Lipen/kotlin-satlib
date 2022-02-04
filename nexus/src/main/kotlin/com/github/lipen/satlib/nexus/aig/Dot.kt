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
