package nexus.aig

import com.github.lipen.satlib.utils.writeln
import examples.utils.secondsSince
import examples.utils.timeNow
import okio.buffer
import okio.sink
import java.io.File
import kotlin.math.absoluteValue

private val log = mu.KotlinLogging.logger {}

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

    fun dependency_graph(
        origin: Collection<Int> = outputs.map { it.id },
    ): Map<Int, List<Int>> {
        log.debug { "Building a dependency graph" }

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
        return toposort(dependency_graph())
    }

    fun eval(inputValues: Map<Int, Boolean>): Map<Int, Boolean> {
        val res: MutableMap<Int, Boolean> = inputValues.toMutableMap()

        TODO()
    }

    // fun eval(valuation: Map<Int, Boolean>) {
    //     // val visited = outputs.toMutableSet()
    //     // val stack = ArrayDeque(outputs)
    //     // val last = 0
    //     //
    //     // while (stack.isNotEmpty()) {
    //     //     val i = stack.removeLast()
    //     //     if (i !in visited) {
    //     //         visited.add(i)
    //     //         when (val node = mapping.getValue(i)) {
    //     //             is AigInput -> {
    //     //                 check(i in valuation)
    //     //             }
    //     //             is AigAndGate -> {
    //     //                 stack.addLast(node.left)
    //     //                 stack.addLast(node.right)
    //     //             }
    //     //         }
    //     //     }
    //     // }
    // }

    override fun toString(): String {
        return "Aig(in: ${inputs.size}, out: ${outputs.size}, ands: ${ands.size})"
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
    var left: Ref,
    var right: Ref,
) : AigNode {
    init {
        require(id > 0)
    }

    override val children: List<Ref> = listOf(left, right)
}

fun main() {
    val timeStart = timeNow()

    // val filename = "data/and.aag"
    // val filename = "data/halfadder.aag"
    val filename = "data/eq.aag"
    // val filename = "data/eq2.aag"
    val aig = parseAig(filename)
    log.info("Result: $aig")

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

    val deps = aig.dependency_graph()
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

    log.info("All done in %.3f s".format(secondsSince(timeStart)))
}
