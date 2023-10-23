package com.github.lipen.satlib.nexus.aig

import com.github.lipen.satlib.core.Lit
import com.github.lipen.satlib.core.newBoolVarArray
import com.github.lipen.satlib.core.sign
import com.github.lipen.satlib.nexus.utils.cartesianProduct
import com.github.lipen.satlib.nexus.utils.isOdd
import com.github.lipen.satlib.nexus.utils.secondsSince
import com.github.lipen.satlib.nexus.utils.timeNow
import com.github.lipen.satlib.nexus.utils.toBinaryString
import com.github.lipen.satlib.nexus.utils.toposort
import com.github.lipen.satlib.op.iffAnd
import com.github.lipen.satlib.solver.Solver
import com.github.lipen.satlib.solver.jni.MiniSatSolver
import com.github.lipen.satlib.solver.solve
import com.github.lipen.satlib.utils.useWith
import com.github.lipen.satlib.utils.writeln
import com.soywiz.klock.measureTimeWithResult
import okio.buffer
import okio.sink
import java.nio.file.Paths
import kotlin.io.path.nameWithoutExtension
import kotlin.random.Random

private val logger = mu.KotlinLogging.logger {}

class Aig(
    val inputs: List<AigInput>,
    val outputs: List<Ref>,
    val andGates: List<AigAndGate>,
    val mapping: Map<Int, AigNode>, // {id: node}
    // val latches: List<AigLatch>,
    // TODO: val symbolTable...
) {
    init {
        require(inputs.intersect(andGates).isEmpty())
        require(inputs.size + andGates.size == mapping.size)
    }

    val size: Int = mapping.size

    val inputIds: List<Int> = inputs.map { it.id }
    val outputIds: List<Int> = outputs.map { it.id }
    val andGateIds: List<Int> = andGates.map { it.id }

    val layers: List<List<Int>> = toposort(dependencyGraph()).map { layer -> layer.sorted() }.toList()

    private val parentsTable: Map<Int, List<Ref>> =
        mapping.keys.associateWith { mutableListOf<Ref>() }.also {
            for (layer in layers) {
                for (id in layer) {
                    for (child in children(id)) {
                        it.getValue(child.id).add(Ref(id, child.negated))
                    }
                }
            }
        }

    fun node(id: Int): AigNode = mapping.getValue(id)
    fun input(id: Int): AigInput = node(id) as AigInput
    fun andGate(id: Int): AigAndGate = node(id) as AigAndGate

    fun children(id: Int): List<Ref> = node(id).children
    fun parents(id: Int): List<Ref> = parentsTable.getValue(id)
    fun layerIndex(id: Int): Int = layers.indexOfFirst { it.contains(id) }

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

    fun eval(inputValues: List<Boolean>): Map<Int, Boolean> {
        // {id: value}
        val nodeValue: MutableMap<Int, Boolean> = mutableMapOf()

        // Add input values
        for ((i, input) in inputs.withIndex()) {
            nodeValue[input.id] = inputValues[i]
        }

        // Walk by layers and compute gate values
        for ((i, layer) in layers.withIndex()) {
            // 0-th layer contains only inputs
            if (i == 0) continue

            for (id in layer) {
                val node = node(id)
                check(node is AigAndGate)
                val left = nodeValue.getValue(node.left.id) xor node.left.negated
                val right = nodeValue.getValue(node.right.id) xor node.right.negated
                nodeValue[id] = left and right
            }
        }

        check(nodeValue.size == size)
        return nodeValue
        // return outputs.map { nodeValue.getValue(it.id) xor it.negated }
    }

    fun computeTFTable(n: Int, random: Random): Map<Int, Pair<Int, Int>> {
        // Returns {id: (countTrue, countFalse)}

        val tableTrue = mutableMapOf<Int, Int>()
        val tableFalse = mutableMapOf<Int, Int>()

        for (id in mapping.keys) {
            tableTrue[id] = 0
            tableFalse[id] = 0
        }

        for (i in 1..n) {
            val inputValues = (1..inputs.size).map { random.nextBoolean() }
            val nodeValue = eval(inputValues)
            for (id in mapping.keys) {
                if (nodeValue.getValue(id)) {
                    tableTrue.merge(id, 1, Int::plus)
                    // tableTrue[id] = tableTrue[id]!! + 1
                } else {
                    tableFalse.merge(id, 1, Int::plus)
                    // tableFalse[id] = tableFalse[id]!! + 1
                }
            }
        }

        return mapping.keys.associateWith { Pair(tableTrue[it]!!, tableFalse[it]!!) }
    }

    internal var precomputedPTable: Map<Int, Double>? = null

    fun computePTable(n: Int, random: Random): Map<Int, Double> {
        if (precomputedPTable != null) return precomputedPTable!!

        return computeTFTable(n, random).mapValues { (_, tf) ->
            val (t, f) = tf
            t.toDouble() / (t + f).toDouble()
        }
    }

    override fun toString(): String {
        return "Aig(inputs: ${inputs.size}, outputs: ${outputs.size}, ands: ${andGates.size})"
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

    fun toInt(): Int {
        return if (negated) -id else id
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

@Suppress("LocalVariableName")
fun main() {
    val timeStart = timeNow()

    // val filename = "data/instances/examples/aag/and.aag"
    // val filename = "data/instances/examples/aag/halfadder.aag"
    // val filename = "data/instances/manual/aag/eq.aag"
    // val filename = "data/instances/manual/aag/eq2.aag"
    // val filename = "data/instances/ISCAS/aag/c17.aag"
    // val filename = "data/instances/ISCAS/aag/c432.aag"
    val filename = "data/instances/BubbleSort/aag/BubbleSort_3_3.aag"

    val dumpToDot = false

    val aig = parseAig(filename)
    logger.info { aig }

    if (dumpToDot) {
        val path = Paths.get(filename)
        val pathDot = Paths.get("data/dot", path.nameWithoutExtension + ".dot")
        pathDot.sink().buffer().use {
            println("Dumping AIG to DOT '$pathDot'")
            for (line in convertAigToDot(aig)) {
                it.writeln(line)
            }
        }
        val pathPdf = Paths.get("data/pdf", pathDot.nameWithoutExtension + ".pdf")
        Runtime.getRuntime().exec("dot -Tpdf $pathDot -o $pathPdf")
    }

    // println("Inputs:")
    // for (input in aig.inputs) {
    //     println("  - $input")
    // }
    // println("Outputs:")
    // for (output in aig.outputs) {
    //     println("  - $output")
    // }
    // println("Gates:")
    // for (gate in aig.ands) {
    //     println("  - $gate")
    // }
    //
    // val deps = aig.dependencyGraph()
    // println("Deps:")
    // for ((id, ds) in deps) {
    //     println("  - $id: ${ds.map { it }}")
    // }
    // for ((i, layer) in toposort(deps).withIndex()) {
    //     println("Layer #${i + 1} (${layer.size} nodes): $layer")
    // }

    for ((i, layer) in aig.layers.withIndex()) {
        println("Layer #${i + 1} (${layer.size} nodes): $layer")
    }

    fun s(t: Int, f: Int): Double {
        return t.toDouble() / (t + f)
    }

    val random = Random(42)
    val n = 10000
    val table = aig.computeTFTable(n, random)
    println("table for n = $n:")
    val sortedTable = table.toList().sortedBy { (_, p) -> val (t, f) = p; -s(t, f) }
    for ((id, p) in sortedTable.take(10)) {
        val (t, f) = p
        val s = s(t, f)
        println("  - $id: t=$t, f=$f, s=%.3f".format(s))
    }
    println("    ...")
    for ((id, p) in sortedTable.takeLast(10)) {
        val (t, f) = p
        val s = s(t, f)
        println("  - $id: t=$t, f=$f, s=%.3f".format(s))
    }

    MiniSatSolver().useWith {
        val timeStartDeclare = timeNow()
        val startNumberOfVariables = numberOfVariables
        val startNumberOfClauses = numberOfClauses

        logger.info("Declaring variables and constraints...")

        /* Constants */

        context["aig"] = aig
        val X = context("X") { aig.inputs.size }
        val Y = context("Y") { aig.outputs.size }
        val G = context("V") { aig.andGates.size }
        logger.info("X = $Y, Y = $Y, G = $G")

        fun input(x: Int): AigInput = aig.inputs[x - 1]
        fun output(y: Int): Ref = aig.outputs[y - 1]
        fun andGate(g: Int): AigAndGate = aig.andGates[g - 1] // FIXME: order?

        /* Variables */

        val inputValue = context("inputValue") {
            newBoolVarArray(X)
        }
        val andGateValue = context("andGateValue") {
            newBoolVarArray(G)
        }

        fun nodeValue(id: Int): Lit {
            val node = aig.node(id)
            return when (node) {
                is AigInput -> inputValue[aig.inputs.indexOf(node) + 1]
                is AigAndGate -> andGateValue[aig.andGates.indexOf(node) + 1]
            }
        }

        fun nodeValue(ref: Ref): Lit = nodeValue(ref.id) sign !ref.negated

        val outputValue = context("outputValue") {
            newBoolVarArray(Y) { (y) ->
                val output = output(y)
                nodeValue(output)
            }
        }

        /* Constraints */

        comment("AND gate semantics")
        for (g in 1..G) {
            val gate = andGate(g)
            iffAnd(
                andGateValue[g],
                nodeValue(gate.left),
                nodeValue(gate.right),
            )
        }

        // ==========

        val diffVars = numberOfVariables - startNumberOfVariables
        val diffClauses = numberOfClauses - startNumberOfClauses
        logger.info {
            "Declared $diffVars variables and $diffClauses clauses in %.3f s"
                .format(secondsSince(timeStartDeclare))
        }

        // ==========

        fun Solver.calculateConjugatedTable(lits: List<Lit>): List<Boolean> {
            require(lits.size in 1..10)
            return lits.map { listOf(false, true) }.cartesianProduct().map { signs ->
                val (isSat, timeSolve) = measureTimeWithResult {
                    solve(lits.zip(signs) { v, s -> v sign s })
                }
                // println("${signs.toBinaryString()}: ${if (isSat) "  SAT" else "UNSAT"} in %.3fs".format(timeSolve.seconds))
                isSat
            }.toList()
        }

        // val lits = listOf(
        //     andGateValue[1],
        //     andGateValue[2],
        //     andGateValue[3],
        //     andGateValue[4],
        //     andGateValue[5],
        // )
        // val lits = aig.layers[1].map { andGateValue[aig.andGateIds.indexOf(it) + 1] }
        val lits = sortedTable.reversed().take(10).map { (id, _) ->
            andGateValue[aig.andGateIds.indexOf(id) + 1]
        }
        println("Lits (${lits.size}): $lits")
        val conjugatedTable = calculateConjugatedTable(lits)
        println("Conjugated table: ${conjugatedTable.toBinaryString()}")
        println("Saturation: ${conjugatedTable.count { it }}/${conjugatedTable.size}")
    }

    logger.info("All done in %.3fs".format(secondsSince(timeStart)))
}
