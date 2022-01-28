package nexus.aig

@Suppress("LocalVariableName")
fun convertAigToDot(
    aig: Aig,
    rankByLayers: Boolean = true,
    eqIds: List<Pair<Int, Int>> = emptyList(),
): Sequence<String> = sequence {
    val SHAPE_PI = "invtriangle" // Primary Input
    val SHAPE_PO = "triangle" // Primary Output
    val SHAPE_AND = "oval" // And gate
    val SHAPE_INPUT = "box" // Input gate

    yield("digraph {")

    yield("// Primary Inputs")
    yield("{ rank=sink")
    for (i in aig.inputs.indices) {
        yield("  i$i [shape=$SHAPE_PI,color=blue];")
    }
    yield("}")

    val layers = aig.layers().toList()
    for ((i, layer) in layers.withIndex()) {
        yield("// Layer #${i + 1}")
        if (rankByLayers) {
            yield("{ rank=same")
        }
        for (id in layer) {
            val node = aig.node(id)
            val shape = when (node) {
                is AigInput -> SHAPE_INPUT
                is AigAndGate -> SHAPE_AND
            }
            yield("  $id [shape=$shape]; // $node")
        }
        if (rankByLayers) {
            yield("}")
        }
    }

    yield("// Primary Outputs")
    yield("{ rank=source")
    for (i in aig.outputs.indices) {
        yield("  o$i [shape=$SHAPE_PO,color=blue];")
    }
    yield("}")

    yield("// Input connections")
    for ((i, node) in aig.inputs.withIndex()) {
        yield("  ${node.id} -> i$i [arrowhead=none];")
    }

    yield("// Node connections")
    for (id in layers.flatten()) {
        val node = aig.node(id)
        for (child in node.children) {
            yield("  $id -> ${child.id} [arrowhead=${if (child.negated) "dot" else "none"}];")
        }
    }

    yield("// Output connections")
    for ((i, node) in aig.outputs.withIndex()) {
        yield("  o$i -> ${node.id} [arrowhead=${if (node.negated) "dot" else "none"}];")
    }

    if (eqIds.isNotEmpty()) {
        yield("// Equivalent gates connections")
        for ((a, b) in eqIds) {
            yield("  $a -> $b [arrowhead=none,color=red,penwidth=2];")
        }
    }

    yield("}")
}
