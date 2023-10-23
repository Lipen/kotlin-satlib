@file:Suppress("UNUSED_VARIABLE", "LocalVariableName")

package examples.coloring

import com.github.lipen.satlib.core.IntVar
import com.github.lipen.satlib.core.convertIntVarDomainMap
import com.github.lipen.satlib.core.eq
import com.github.lipen.satlib.core.newIntVarDomainMap
import com.github.lipen.satlib.op.neqv
import com.github.lipen.satlib.solver.Solver
import com.github.lipen.satlib.solver.addClause
import com.github.lipen.satlib.utils.DomainMap
import com.github.lipen.satlib.utils.Domains
import com.github.lipen.satlib.utils.IntValueRangeDomain
import com.github.lipen.satlib.utils.Tuple1
import com.github.lipen.satlib.utils.Value
import com.github.lipen.satlib.utils.get
import com.github.lipen.satlib.utils.useWith
import examples.utils.secondsSince
import examples.utils.timeNow

private object GlobalsColoringMap {
    val solverProvider: () -> Solver = {
        // MiniSatSolver()
        // GlucoseSolver()
        // CryptoMiniSatSolver()
        // CadicalSolver()
        com.github.lipen.satlib.solver.jna.CadicalSolver()
    }

    init {
        solverProvider().close()
    }
}

@JvmInline
private value class Vertex(override val value: Int) : Value<Int> {
    override fun toString(): String {
        return "v$value"
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
    context["edges"] = edges.map { (a, b) ->
        if (a <= b) Pair(a, b) else Pair(b, a)
    }.distinct().map { (a, b) ->
        Pair(Vertex(a), Vertex(b))
    }

    // Variables
    run {
        val vertexDomain = IntValueRangeDomain(1..V, ::Vertex)
        val domains = Domains(vertexDomain)
        check(Vertex(1) in vertexDomain)
        check(Vertex(V) in vertexDomain)
        check(Vertex(V + 1) !in vertexDomain)

        val colorMap = newIntVarDomainMap(domains) { 1..k }
        println("colorMap = $colorMap")
        context["color"] = colorMap
    }

    // val color = context("color") {
    //     val domains = Domains(IntRangeDomain(1..V))
    //     newIntVarDomainMap(domains) { 1..k }
    // }
}

private fun Solver.declareConstraints() {
    println("Declaring constraints...")

    val V: Int = context["V"]
    val k: Int = context["k"]
    val edges: List<Pair<Vertex, Vertex>> = context["edges"]

    val color: DomainMap<Tuple1<Vertex>, IntVar> = context["color"]

    // (color[a] = c) -> (color[b] != c)
    for ((a, b) in edges) {
        neqv(color[a], color[b])
    }

    // [aux]
    // (color[1] = 1)
    addClause(color[Vertex(1)] eq 1)
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

    GlobalsColoringMap.solverProvider().useWith {
        declareVariables(V, k, edges)
        declareConstraints()

        println("Solving...")
        if (solve()) {
            println("SAT for k = $k in %.3fs".format(secondsSince(timeStart)))

            val model = getModel()
            val color = context.convertIntVarDomainMap<Tuple1<Vertex>>("color", model)

            println("Graph Coloring: ${(1..V).map { v -> color[Vertex(v)] }}")
        } else {
            println("UNSAT for k = $k in %.3fs".format(secondsSince(timeStart)))
        }
    }

    println()
    println("All done in %.3f s!".format(secondsSince(timeStart)))
}
