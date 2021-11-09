@file:Suppress("UNUSED_VARIABLE", "LocalVariableName")

package examples.coloring

import com.github.lipen.satlib.core.IntVarArray
import com.github.lipen.satlib.core.convertIntVarArray
import com.github.lipen.satlib.core.eq
import com.github.lipen.satlib.core.neq
import com.github.lipen.satlib.core.newIntVarArray
import com.github.lipen.satlib.op.imply
import com.github.lipen.satlib.solver.GlucoseSolver
import com.github.lipen.satlib.solver.Solver
import com.github.lipen.satlib.utils.useWith
import examples.utils.secondsSince
import examples.utils.timeNow

object GlobalsColoring {
    val solverProvider: () -> Solver = {
        // MiniSatSolver()
        GlucoseSolver()
        // CryptoMiniSatSolver()
        // CadicalSolver()
    }

    init {
        solverProvider().close()
    }
}

private fun Solver.declareVariables(
    V: Int, // number of vertices
    k: Int, // number of colors
    edges: List<Pair<Int, Int>>,
) {
    require(V > 0) { "V must be a positive number." }
    require(k > 0) { "k must be a positive number." }

    println("Declaring variables...")

    // Constants
    context["V"] = V
    context["k"] = k
    context["edges"] = edges.map { (a, b) -> if (a <= b) Pair(a, b) else Pair(b, a) }.distinct()

    // Variables
    val color = context("color") { newIntVarArray(V) { 1..k } }
}

private fun Solver.declareConstraints() {
    println("Declaring constraints...")

    val V: Int = context["V"]
    val k: Int = context["k"]
    val edges: List<Pair<Int, Int>> = context["edges"]

    val color: IntVarArray = context["color"]

    // (color[a] = c) -> (color[b] != c)
    for ((a, b) in edges) {
        for (c in 1..k) {
            imply(color[a] eq c, color[b] neq c)
        }
    }

    // [aux]
    // (color[1] = 1)
    addClause(color[1] eq 1)
}

fun main() {
    val timeStart = timeNow()

    val V = 10 // number of vertices
    val k = 3 // number of colors
    val edges: List<Pair<Int, Int>> = listOf(
        1 to 3,
        3 to 5,
        5 to 2,
        2 to 4,
        4 to 1,
        1 to 6,
        2 to 7,
        3 to 8,
        4 to 9,
        5 to 10,
        6 to 7,
        7 to 8,
        8 to 9,
        9 to 10,
        10 to 6,
    )

    GlobalsColoring.solverProvider().useWith {
        declareVariables(V, k, edges)
        declareConstraints()

        println("Solving...")
        if (solve()) {
            println("SAT for k = $k in %.3fs".format(secondsSince(timeStart)))

            val model = getModel()
            val color = context.convertIntVarArray("color", model)

            println("Graph Coloring: ${(1..V).map { v -> color[v] }}")
        } else {
            println("UNSAT for k = $k in %.3fs".format(secondsSince(timeStart)))
        }
    }

    println()
    println("All done in %.3f s!".format(secondsSince(timeStart)))
}
