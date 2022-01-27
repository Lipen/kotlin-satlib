package nexus.aig

fun convertAigToDot(aig: Aig): Sequence<String> = sequence {
    yield("digraph {")

    yield("// Inputs")
    yield("{ rank=sink")
    for (i in aig.inputs.indices) {
        yield("  I$i [shape=invtriangle,color=blue];")
    }
    yield("}")

    val layers = aig.layers().toList()
    for ((i, layer) in layers.withIndex()) {
        yield("// Layer #${i + 1}")
        yield("{ rank=same")
        for (id in layer) {
            when (val node = aig.node(id)) {
                is AigInput -> {
                    yield("  $id [shape=box]; // $node")
                }
                is AigAndGate -> {
                    yield("  $id [shape=oval]; // $node")
                }
            }
        }
        yield("}")
    }

    yield("// Outputs")
    yield("{ rank=source")
    for (i in aig.outputs.indices) {
        yield("  O$i [shape=triangle,color=blue];")
    }
    yield("}")

    yield("// Input connections")
    for ((i, node) in aig.inputs.withIndex()) {
        yield("  ${node.id} -> I$i [arrowhead=none];")
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
        yield("  O$i -> ${node.id} [arrowhead=${if (node.negated) "dot" else "none"}];")
    }

    yield("}")
}
