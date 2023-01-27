package examples.we

import com.github.lipen.multiarray.MultiArray
import com.github.lipen.satlib.core.IntVar
import com.github.lipen.satlib.core.Lit
import com.github.lipen.satlib.core.convert
import com.github.lipen.satlib.core.eq
import com.github.lipen.satlib.core.newIntVar
import com.github.lipen.satlib.core.newIntVarArray
import com.github.lipen.satlib.op.imply
import com.github.lipen.satlib.solver.Solver
import com.github.lipen.satlib.solver.addClause
import com.github.lipen.satlib.solver.jni.GlucoseSolver
import com.github.lipen.satlib.utils.useWith

object GlobalsTheTask {
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

fun Solver.sum(x: IntVar, y: IntVar): IntVar {
    val min = x.domain.minOrNull()!! + y.domain.minOrNull()!!
    val max = x.domain.maxOrNull()!! + y.domain.maxOrNull()!!
    val s = newIntVar(min..max)

    // (x = a) & (y = b) -> (s = a+b)
    for (a in x.domain) {
        for (b in y.domain) {
            addClause(
                -(x eq a),
                -(y eq b),
                s eq a + b
            )
        }
    }

    return s
}

fun Solver.le(x: IntVar, k: Int): Lit {
    val z = newLiteral()

    // (x <= k) -> z
    for (a in x.domain) {
        if (a <= k) {
            // (x = a) -> z
            imply(x eq a, z)
        } else {
            // (x = a) -> ~z
            imply(x eq a, -z)
        }
    }

    return z
}

fun Solver.ge(x: IntVar, k: Int): Lit {
    val z = newLiteral()

    // (x >= k) -> z
    for (a in x.domain) {
        if (a >= k) {
            // (x = a) -> z
            imply(x eq a, z)
        } else {
            // (x = a) -> ~z
            imply(x eq a, -z)
        }
    }

    return z
}

@Suppress("LocalVariableName")
fun main() {
    GlobalsTheTask.solverProvider().useWith {
        // - Given: a graph with V vertices and E edges.
        // - Each edge can have an arbitrary weight from 0 to maxEdgeWeight.
        // - Weight of a vertex is defined to be a total weight of incident edges.
        // - Vertex weight must not exceed maxVertexWeight.
        //
        // --- Maximize the total edge weight.

        val V = 6
        val maxEdgeWeight = 3
        val maxVertexWeight = 5
        val edges: List<Pair<Int, Int>> = listOf(
            1 to 2,
            2 to 3,
            2 to 5,
            2 to 6,
            3 to 4,
            3 to 6,
            5 to 6,
        ).map { (a, b) ->
            // Normalize edge
            if (a <= b) Pair(a, b)
            else Pair(b, a)
        }.distinct()
        val E = edges.size

        println("V = $V, E = $E")
        println("maxEdgeWeight = $maxEdgeWeight, maxVertexWeight = $maxVertexWeight")
        println("edges = $edges")

        val edgeWeight = newIntVarArray(E) { 0..maxEdgeWeight }
        val vertexWeight = MultiArray.new(V) { (v) ->
            edges.asSequence()
                .mapIndexedNotNull { i, e ->
                    if (e.first == v || e.second == v) {
                        edgeWeight[i + 1]
                    } else {
                        null
                    }
                }
                .reduce { acc, w -> sum(acc, w) }
        }
        val totalEdgeWeight = edgeWeight.values.reduce { acc, w -> sum(acc, w) }

        // vertexWeight[v] <= maxVertexWeight
        for (v in 1..V) {
            addClause(le(vertexWeight[v], maxVertexWeight))
        }

        for (k in 1..totalEdgeWeight.domain.maxOrNull()!!) {
            // totalEdgeWeight >= k
            addClause(ge(totalEdgeWeight, k))

            if (solve()) {
                println("SAT for k = $k")

                val model = getModel()
                val edgeWeightVal = edgeWeight.convert(model)
                val vertexWeightVal = vertexWeight.convert(model)

                println("Edge weights: ${edges.zip(edgeWeightVal.values)}")
                println("Vertex weights: ${vertexWeightVal.values}")
            } else {
                println("UNSAT for k = $k")
                break
            }
        }
    }
}
