@file:Suppress("LocalVariableName")

package examples.ham

import com.github.lipen.satlib.core.IntVarArray
import com.github.lipen.satlib.core.Model
import com.github.lipen.satlib.core.convert
import com.github.lipen.satlib.core.convertIntVarArray
import com.github.lipen.satlib.core.eq
import com.github.lipen.satlib.core.neq
import com.github.lipen.satlib.core.newIntVarArray
import com.github.lipen.satlib.solver.jni.CadicalSolver
import com.github.lipen.satlib.solver.jni.GlucoseSolver
import com.github.lipen.satlib.solver.jni.MiniSatSolver
import com.github.lipen.satlib.op.allSolutions
import com.github.lipen.satlib.op.exactlyOne
import com.github.lipen.satlib.op.imply
import com.github.lipen.satlib.op.runWithTimeout
import com.github.lipen.satlib.solver.Solver
import com.github.lipen.satlib.solver.addClause
import com.github.lipen.satlib.utils.pairs
import com.github.lipen.satlib.utils.useWith
import examples.utils.secondsSince
import examples.utils.timeNow
import examples.utils.timeSince
import java.io.File
import kotlin.math.sqrt

private val log = mu.KotlinLogging.logger {}

object GlobalsHamiltonian {
    val solverProvider: () -> Solver = {
        // MiniSatSolver()
        // GlucoseSolver()
        // CryptoMiniSatSolver()
        CadicalSolver()
    }

    init {
        solverProvider().close()
    }
}

private fun <A, B> Pair<A, B>.reversed(): Pair<B, A> = Pair(second, first)

data class Edge(val a: Int, val b: Int) {
    fun reversed(): Edge = Edge(b, a)
}

data class Graph(
    val V: Int,
    val edges: List<Edge>,
) {
    operator fun contains(edge: Edge): Boolean {
        return edge in edges || edge.reversed() in edges
    }
}

private fun Solver.declareVariables(graph: Graph) {
    context["graph"] = graph
    val V = graph.V
    val pathNode = context("pathNode") {
        newIntVarArray(V) { 1..V }
    }
}

private fun Solver.declareConstraints(isLoop: Boolean = false) {
    val graph: Graph = context["graph"]
    val V = graph.V
    val pathNode: IntVarArray = context["pathNode"]

    // EO_{j}(pathNode[i] = j)
    // already defined via IntVar

    comment("Each node has some place in the path")
    // EO_{i}(pathNode[i] = j)
    for (j in 1..V)
        exactlyOne {
            for (i in 1..V)
                yield(pathNode[i] eq j)
        }

    comment("Non-adjacent nodes i and j cannot be adjacent in the path")
    // (pathNode[k] = i) => (pathNode[k+1] != j)  for each (i,j) \notin E(G)
    for (i in 1..V)
        for (j in 1..V)
            if (Edge(i, j) !in graph) {
                for (k in 1 until V) {
                    imply(
                        pathNode[k] eq i,
                        pathNode[k + 1] neq j
                    )
                }
                if (isLoop) {
                    imply(
                        pathNode[1] eq i,
                        pathNode[V] neq j
                    )
                }
            }

    comment("Symmetry break: forbid reverse solutions (first and last nodes are increasing)")
    for (a in 1..V)
        for (b in 1 until a)
            imply(
                pathNode[1] eq a,
                pathNode[V] neq b
            )

    if (isLoop) {
        comment("Symmetry break: first node of the loop is 1")
        addClause(pathNode[1] eq 1)
    }
}

fun solve(
    graph: Graph,
    isLoop: Boolean = false,
    isAll: Boolean = false,
    isDump: Boolean = false,
    timeout: Double? = null, // seconds
) {
    GlobalsHamiltonian.solverProvider().useWith {
        val V = graph.V

        declareVariables(graph)
        declareConstraints(isLoop = isLoop)
        log.debug("Variables: $numberOfVariables, clauses: $numberOfClauses")

        if (isDump) {
            val cnfFile = File("cnf-ham-$V.cnf")
            log.debug("Dumping CNF to '${cnfFile.path}'...")
            dumpDimacs(cnfFile)
        }

        if (isAll) {
            println("Searching for all solutions for V = $V...")

            val pathNodeVar: IntVarArray = context["pathNode"]
            // val essential = pathNodeVar.values.flatMap { it.literals }
            val refutation: (Model) -> List<Int> = { model ->
                val pathNode = pathNodeVar.convert(model)
                (1..V).map { i ->
                    pathNodeVar[i] neq pathNode[i]
                }
            }

            var count = 0
            val timeStart = timeNow()
            var timeLast = timeNow()

            for (model in allSolutions(refutation)) {
                val timeSolution = timeSince(timeLast)
                count++
                // println("SAT for V = $V")
                val path = context.convertIntVarArray("pathNode", model)
                println("found solution for V = $V in %.3fs: ${path.values}".format(timeSolution.seconds))
                timeLast = timeNow()
            }

            val timeDelta = timeSince(timeStart)
            if (count == 0) {
                println("UNSAT for V = $V in %.3fs".format(timeDelta.seconds))
            } else {
                println("SAT for V = $V with $count solution(s) in %.3fs".format(timeDelta.seconds))
            }
        } else {
            println("Solving for V = $V...")

            val timeStart = timeNow()
            val isSat = if (timeout == null) {
                solve()
            } else {
                runWithTimeout((timeout * 1000).toLong()) {
                    solve()
                }
            }

            if (isSat) {
                val model = getModel()
                val pathNode = context.convertIntVarArray("pathNode", model)
                println("solution: ${pathNode.values}")
            }

            val timeDelta = timeSince(timeStart)
            println("${if (isSat) "SAT" else "UNSAT"} for V = $V in %.3f s".format(timeDelta.seconds))

            if (this is MiniSatSolver) {
                log.debug("Decisions: ${backend.numberOfDecisions}")
                log.debug("Conflicts: ${backend.numberOfConflicts}")
                log.debug("Propagations: ${backend.numberOfPropagations}")
            }
            if (this is GlucoseSolver) {
                log.debug("Decisions: ${backend.numberOfDecisions}")
                log.debug("Conflicts: ${backend.numberOfConflicts}")
                log.debug("Propagations: ${backend.numberOfPropagations}")
            }
            if (this is CadicalSolver) {
                log.debug("Decisions: ${backend.numberOfDecisions}")
                log.debug("Conflicts: ${backend.numberOfConflicts}")
                log.debug("Propagations: ${backend.numberOfPropagations}")
            }
        }
    }
}

/**
 * Determine whether the number [x] is a perfect square.
 */
private fun isSquare(x: Int): Boolean {
    require(x >= 0) { "value must be non-negative" }
    val s = sqrt(x.toDouble()).toInt()
    return s * s == x
}

/**
 * Generate a graph with [V] vertices, such that
 * the sum of each pair of adjacent vertices is a perfect square.
 */
private fun generateGraph(V: Int): Graph {
    val edges: MutableList<Edge> = mutableListOf()
    for ((i, j) in (1..V).pairs()) {
        if (isSquare(i + j)) {
            edges.add(Edge(i, j))
        }
    }
    log.debug("Generated a perfect-squares graph with V = $V, E = ${edges.size}")
    return Graph(V, edges)
}

fun main() {
    val timeStart = timeNow()

    for (V in 2..36) {
        solve(generateGraph(V), isLoop = true, isAll = false)
        println("=".repeat(42))
    }

    for (V in 2..30) {
        solve(generateGraph(V), isLoop = false, isAll = true)
        println("=".repeat(42))
    }

    println("All done in %.3f s!".format(secondsSince(timeStart)))
}
