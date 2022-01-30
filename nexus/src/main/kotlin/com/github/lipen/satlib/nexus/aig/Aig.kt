package com.github.lipen.satlib.nexus.aig

import com.github.lipen.satlib.nexus.utils.isOdd
import com.github.lipen.satlib.nexus.utils.secondsSince
import com.github.lipen.satlib.nexus.utils.timeNow
import com.github.lipen.satlib.nexus.utils.toposort

private val logger = mu.KotlinLogging.logger {}

class Aig(
    val inputs: List<AigInput>,
    val outputs: List<Ref>,
    val ands: List<AigAndGate>,
    val mapping: Map<Int, AigNode>, // {id:node}
    // val latches: List<AigLatch>,
    // TODO: val symbolTable...
) {
    init {
        require(inputs.intersect(ands).isEmpty())
        require(inputs.size + ands.size == mapping.size)
    }

    val size: Int = mapping.size

    fun node(id: Int): AigNode = mapping.getValue(id)
    fun input(id: Int): AigInput = node(id) as AigInput
    fun andGate(id: Int): AigAndGate = node(id) as AigAndGate

    fun dependencyGraph(
        origin: Collection<Int> = outputs.map { it.id },
    ): Map<Int, List<Int>> {
        logger.debug { "Building a dependency graph" }

        val deps: MutableMap<Int, List<Int>> = mutableMapOf()
        val queue = ArrayDeque(origin)

        while (queue.isNotEmpty()) {
            val id = queue.removeFirst()
            deps.computeIfAbsent(id) {
                node(id).children.map { it.id }.also {
                    queue.addAll(it)
                }
            }
        }

        return deps
    }

    fun layers(): Sequence<List<Int>> {
        return toposort(dependencyGraph())
    }

    override fun toString(): String {
        return "Aig(inputs: ${inputs.size}, outputs: ${outputs.size}, ands: ${ands.size})"
    }
}

data class Ref(
    val id: Int,
    val negated: Boolean,
) {
    init {
        require(id > 0)
    }

    override fun toString(): String {
        return "${if (negated) "~" else ""}@$id"
    }

    companion object {
        fun fromLiteral(lit: Int): Ref {
            require(lit > 0)
            return Ref(lit / 2, negated = isOdd(lit))
        }
    }
}

sealed interface AigNode {
    val id: Int
    val children: List<Ref>
}

data class AigInput(
    override val id: Int,
) : AigNode {
    init {
        require(id > 0)
    }

    override val children: List<Ref> = emptyList()
}

data class AigAndGate(
    override val id: Int,
    val left: Ref,
    val right: Ref,
) : AigNode {
    init {
        require(id > 0)
    }

    override val children: List<Ref> = listOf(left, right)
}

fun main() {
    val timeStart = timeNow()

    // val filename = "data/instances/examples/aag/and.aag"
    // val filename = "data/instances/examples/aag/halfadder.aag"
    val filename = "data/instances/manual/aag/eq.aag"
    // val filename = "data/instances/manual/aag/eq2.aag"

    val aig = parseAig(filename)
    logger.info("Result: $aig")

    println("Inputs:")
    for (input in aig.inputs) {
        println("  - $input")
    }
    println("Outputs:")
    for (output in aig.outputs) {
        println("  - $output")
    }
    println("Gates:")
    for (gate in aig.ands) {
        println("  - $gate")
    }

    val deps = aig.dependencyGraph()
    println("Deps:")
    for ((id, ds) in deps) {
        println("  - $id: ${ds.map { it }}")
    }

    for ((i, layer) in toposort(deps).withIndex()) {
        println("Layer #${i + 1} (${layer.size} nodes): $layer")
    }

    // val filenameDot = filename.substringBeforeLast(".") + ".dot"
    // File(filenameDot).sink().buffer().use {
    //     println("Dumping AIG to DOT '$filenameDot'")
    //     for (line in convertAigToDot(aig)) {
    //         it.writeln(line)
    //     }
    // }

    logger.info("All done in %.3f s".format(secondsSince(timeStart)))
}
