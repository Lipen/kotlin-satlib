package examples.bf

import com.github.lipen.satlib.core.newBoolVarArray
import com.github.lipen.satlib.core.newDomainVarArray
import com.github.lipen.satlib.core.newIntVarArray
import com.github.lipen.satlib.core.sign
import com.github.lipen.satlib.op.iff
import com.github.lipen.satlib.op.imply
import com.github.lipen.satlib.op.implyAnd
import com.github.lipen.satlib.op.implyImply
import com.github.lipen.satlib.op.implyImplyIff
import com.github.lipen.satlib.op.implyImplyIffAnd
import com.github.lipen.satlib.op.implyImplyIffOr
import com.github.lipen.satlib.op.implyImplyImply
import com.github.lipen.satlib.op.runWithTimeout
import com.github.lipen.satlib.solver.Solver
import com.github.lipen.satlib.utils.useWith
import com.soywiz.klock.PerformanceCounter

@Suppress("LocalVariableName")
private fun Solver.declareVariables(P: Int, truthTable: Map<Row, Boolean>): BFVariables {
    val X = truthTable.keys.first().size
    val U = truthTable.size
    val nodeType = newDomainVarArray(P) { NodeType.values().asIterable() }
    val nodeInputVariable = newIntVarArray(P) { 0..X }
    val nodeParent = newIntVarArray(P) { (p) ->
        if (p == 1) listOf(0)
        else 1 until p
    }
    val nodeChild = newIntVarArray(P) { (p) -> ((p + 1)..P) + 0 }
    val nodeValue = newBoolVarArray(P, U)

    return BFVariables(
        P = P, X = X, U = U,
        inputs = truthTable.keys.map { it.values },
        values = truthTable.values.toList(),
        nodeType = nodeType,
        nodeInputVariable = nodeInputVariable,
        nodeParent = nodeParent,
        nodeChild = nodeChild,
        nodeValue = nodeValue
    )
}

private fun Solver.declareReduction(vars: BFVariables): Unit = with(vars) {
    /* Structural constraints */

    comment("Child=>parent relation")
    // (nodeChild[p] = c) => (nodeParent[c] = p)
    // (nodeChild[p] = 0) => AND{c}( nodeParent[c] != p )
    for (p in 1..P) {
        for (c in (p + 1)..P)
            imply(
                nodeChild[p] eq c,
                nodeParent[c] eq p
            )
        implyAnd(nodeChild[p] eq 0) {
            for (c in (p + 1)..P)
                yield(nodeParent[c] neq p)
        }
    }

    comment("BFS")
    // nodeParent[j + 1] >= nodeParent[j]
    for (j in 3 until P)
        for (i in 2 until j)
            for (s in 1 until i)
                imply(
                    nodeParent[j] eq i,
                    nodeParent[j + 1] neq s
                )

    /* TERMINAL nodes constraints */

    comment("Only terminal nodes have associated input variables")
    // (nodeType[p] = TERMINAL) <=> (nodeInputVariable[p] != 0)
    for (p in 1..P)
        iff(
            nodeType[p] eq NodeType.TERMINAL,
            nodeInputVariable[p] neq 0
        )

    comment("Terminals do not have children")
    // (nodeType[p] = TERMINAL) => (nodeChild[p] = 0)
    for (p in 1..P)
        imply(
            nodeType[p] eq NodeType.TERMINAL,
            nodeChild[p] eq 0
        )

    /* AND/OR nodes constraints */

    comment("AND/OR nodes cannot have numbers P-1 or P")
    // (nodeType[P] != AND/OR) & (nodeType[P-1] != AND/OR)
    for (t in listOf(NodeType.AND, NodeType.OR)) {
        if (P >= 1) addClause(nodeType[P] neq t)
        if (P >= 2) addClause(nodeType[P - 1] neq t)
    }

    comment("AND/OR: left child cannot have number P")
    // (nodeType[p] = AND/OR) => (nodeChild[p] != P)
    for (t in listOf(NodeType.AND, NodeType.OR))
        for (p in 1 until (P - 1))
            imply(
                nodeType[p] eq t,
                nodeChild[p] neq P
            )

    comment("AND/OR nodes have left child")
    // (nodeType[p] = AND/OR) => (nodeChild[p] != 0)
    for (t in listOf(NodeType.AND, NodeType.OR))
        for (p in 1 until (P - 1))
            imply(
                nodeType[p] eq t,
                nodeChild[p] neq 0
            )

    comment("AND/OR: parent-child relation")
    // (nodeType[p] = AND/OR) & (nodeParent[c] = p) & (nodeParent[c+1] = p) => (nodeChild[p] = c)
    for (t in listOf(NodeType.AND, NodeType.OR))
        for (p in 1..P)
            for (c in (p + 1) until P)
                implyImplyImply(
                    nodeType[p] eq t,
                    nodeParent[c] eq p,
                    nodeParent[c + 1] eq p,
                    nodeChild[p] eq c
                )

    comment("AND/OR: right child follows the left one")
    // (nodeType[p] = AND/OR) & (nodeChild[p] = c) => (nodeParent[c+1] = p)
    for (t in listOf(NodeType.AND, NodeType.OR))
        for (p in 1..P)
            for (c in (p + 1) until P)
                implyImply(
                    nodeType[p] eq t,
                    nodeChild[p] eq c,
                    nodeParent[c + 1] eq p
                )

    /* NOT nodes constraints */

    comment("NOT nodes cannot have number P")
    // (nodeType[P] != NOT)
    if (P >= 1) addClause(nodeType[P] neq NodeType.NOT)

    comment("NOT nodes have left child")
    // (nodeType[p] = NOT) => (nodeChild[p] != 0)
    for (p in 1 until P)
        imply(
            nodeType[p] eq NodeType.NOT,
            nodeChild[p] neq 0
        )

    comment("NOT: parent's child is the current node")
    // (nodeType[p] = NOT) & (nodeParent[c] = p) => (nodeChild[p] = c)
    for (p in 1..P)
        for (c in (p + 1)..P)
            implyImply(
                nodeType[p] eq NodeType.NOT,
                nodeParent[c] eq p,
                nodeChild[p] eq c
            )

    if (GlobalsBF.IS_FORBID_DOUBLE_NEGATION) {
        comment("NOT: forbid double negation")
        // (nodeType[p] = NOT) & (nodeChild[p] = c) => (nodeType[c] != NOT)
        for (p in 1..P)
            for (c in (p + 1)..P)
                implyImply(
                    nodeType[p] eq NodeType.NOT,
                    nodeChild[p] eq c,
                    nodeType[c] neq NodeType.NOT
                )
    }

    /* Node values constraints */

    comment("Root has value from the truth table")
    // nodeValue[1, u] <=> tt.values[u]
    for (u in 1..U)
        addClause(nodeValue[1, u] sign values[u - 1])

    comment("Terminal nodes have value from associated input variables")
    // (nodeInputVariable[p] = x) => (nodeValue[p, u] <=> tt.inputs[u,x]
    for (p in 1..P)
        for (x in 1..X)
            for (u in 1..U)
                imply(
                    nodeInputVariable[p] eq x,
                    nodeValue[p, u] sign inputs[u - 1][x - 1]
                )

    comment("AND: value is calculated as a conjunction of children values")
    // (nodeType[p] = AND) & (nodeChild[p] = c) => (nodeValue[p, u] <=> nodeValue[c, u] & nodeValue[c+1, u])
    for (p in 1..P)
        for (c in (p + 1) until P)
            for (u in 1..U)
                implyImplyIffAnd(
                    nodeType[p] eq NodeType.AND,
                    nodeChild[p] eq c,
                    nodeValue[p, u],
                    nodeValue[c, u],
                    nodeValue[c + 1, u]
                )

    comment("OR: value is calculated as a disjunction of children values")
    // (nodeType[p] = OR) & (nodeChild[p] = c) => (nodeValue[p, u] <=> nodeValue[c, u] & nodeValue[c+1, u])
    for (p in 1..P)
        for (c in (p + 1) until P)
            for (u in 1..U)
                implyImplyIffOr(
                    nodeType[p] eq NodeType.OR,
                    nodeChild[p] eq c,
                    nodeValue[p, u],
                    nodeValue[c, u],
                    nodeValue[c + 1, u]
                )

    comment("NOT: value is calculated as a negation of a child value")
    // (nodeType[p] = NOT) & (nodeChild[p] = c) => (nodeValue[p, u] <=> ~nodeValue[c, u])
    for (p in 1..P)
        for (c in (p + 1)..P)
            for (u in 1..U)
                implyImplyIff(
                    nodeType[p] eq NodeType.NOT,
                    nodeChild[p] eq c,
                    nodeValue[p, u],
                    -nodeValue[c, u]
                )
}

fun solveFor(P: Int, tt: Map<Row, Boolean>, timeout: Double = 0.0): BFAssignment? {
    require(P >= 1)
    // DimacsFileSolver("cryptominisat5 %s --maxtime 30", createTempFile()).useWith {
    // DimacsFileSolver("cryptominisat5 %s --maxtime 10", File("cnf-P$P")).useWith {
    // DimacsFileSolver("cryptominisat5 %s", File("cnf-P$P")).useWith {
    // DimacsFileSolver("D:/dev/tools/minisat/build/release/bin/minisat %s result; cat result", File("cnf")).useWith {
    // DimacsFileSolver("python run_minisat.py %s result-P$P", File("cnf-P$P")).useWith {
    // CryptoMiniSatSolver().useWith {
    GlobalsBF.solverProvider().useWith {
        val timeStart = PerformanceCounter.reference
        val nvarStart = numberOfVariables
        val nconStart = numberOfClauses

        val vars = declareVariables(P, tt)
        declareReduction(vars)

        val nvarDiff = numberOfVariables - nvarStart
        val nconDiff = numberOfClauses - nconStart
        println(
            "Done declaring variables ($nvarDiff) and constraints ($nconDiff) in %.3f s."
                .format(timeSince(timeStart).seconds)
        )

        with(vars) {
            for (p in 1..P) {
                comment("nodeType[p = $p]: ${nodeType[p].literals}")
            }
            for (p in 1..P) {
                comment("nodeInputVariable[p = $p]: ${nodeInputVariable[p].literals}")
            }
            for (p in 1..P) {
                comment("nodeParent[p = $p]: ${nodeParent[p].literals}")
            }
            for (p in 1..P) {
                comment("nodeChild[p = $p]: ${nodeChild[p].literals}")
            }
            for (p in 1..P) {
                comment("nodeValue[p = $p]: ${(1..U).map { u -> nodeValue[p, u] }}")
            }
            comment("rootValue: ${(1..U).map { u -> nodeValue[1, u] }}")
        }

        // val cnf = File("out/cnf-${ttToBinaryString(tt)}-P$P.dimacs")
        // println("Dumping cnf to <$cnf>...")
        // (this as? MiniSatSolver)?.run {
        //     dumpDimacs(cnf)
        //     // backend.toDimacs(cnf.resolveSibling(cnf.nameWithoutExtension + "-minisat.dimacs"))
        // }

        println("Solving...")
        val timeStartSolving = PerformanceCounter.reference
        val isSat = if (timeout > 0) runWithTimeout((timeout * 1000).toLong()) { solve() } else solve()
        val timeSolving = timeSince(timeStartSolving)

        return if (isSat) {
            println("SAT for P = $P in %.3f s".format(timeSolving.seconds))
            BFAssignment.fromModel(getModel(), vars)
                .also {
                    val f = it.toLogic()
                    println("f = ${f.toPrettyString()}")
                    if (isBooleanFunctionCompliesWithTruthTable(f, tt)) {
                        println("Check: Ok")
                    } else {
                        error("Boolean function does not comply with truth table")
                    }
                }
        } else {
            println("UNSAT for P = $P in %.3f s".format(timeSolving.seconds))
            null
        }
    }
}

fun solveIteratively(
    tt: Map<Row, Boolean>,
    Pmax: Int = GlobalsBF.Pmax,
    timeout: Double = GlobalsBF.timeout,
    quite: Boolean = false,
): BFAssignment? {
    println("Searching BF by Iterative strategy for the truth table '${ttToBinaryString(tt)}'...")
    val timeStart = PerformanceCounter.reference
    for (P in 1..Pmax) {
        println("\nTrying P = $P")
        val assignment = solveFor(P, tt, timeout = timeout - timeSince(timeStart).seconds)
        if (assignment != null) {
            if (!quite) {
                println("Solution found in %.3f s!".format(timeSince(timeStart).seconds))
                println("P = $P, X = ${assignment.X}, U = ${assignment.U}")
                println("nodeType = ${assignment.nodeType}")
                println("nodeInputVariable = ${assignment.nodeInputVariable}")
                println("nodeParent = ${assignment.nodeParent}")
                println("nodeChild = ${assignment.nodeChild}")
                println("nodeValue:")
                for (p in 1..P) {
                    println(
                        (1..tt.size).joinToString("") { u ->
                            assignment.nodeValue[p, u].toInt().toString()
                        }
                    )
                }
                val f = assignment.toLogic()
                println("f = ${f.toPrettyString()}")
                if (isBooleanFunctionCompliesWithTruthTable(f, tt)) {
                    println("Check: Ok")
                } else {
                    error("Boolean function does not comply with truth table")
                }
            }
            return assignment
        }
        if (timeSince(timeStart).seconds >= timeout) {
            return null
        }
    }
    return null
}
