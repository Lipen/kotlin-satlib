package examples.bf

import com.github.lipen.satlib.core.DomainVarArray
import com.github.lipen.satlib.core.IntVar
import com.github.lipen.satlib.core.IntVarArray
import com.github.lipen.satlib.core.eq
import com.github.lipen.satlib.core.neq
import com.github.lipen.satlib.core.newBoolVarArray
import com.github.lipen.satlib.core.newDomainVar
import com.github.lipen.satlib.core.newDomainVarArray
import com.github.lipen.satlib.core.newIntVar
import com.github.lipen.satlib.core.sign
import com.github.lipen.satlib.op.atMostOne
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
import com.github.lipen.satlib.solver.solve
import com.github.lipen.satlib.utils.useWith
import com.soywiz.klock.PerformanceCounter
import java.io.File

private fun Solver.emptyBFVariables(tt: Map<Row, Boolean>): BFVariables =
    BFVariables(
        P = 0,
        X = tt.keys.first().size,
        U = tt.size,
        inputs = tt.keys.map { it.values },
        values = tt.values.toList(),
        nodeType = newDomainVarArray { NodeType.values().asIterable() },
        nodeInputVariable = IntVarArray.new { IntVar.empty() },
        nodeParent = IntVarArray.new { IntVar.empty() },
        nodeChild = IntVarArray.new { IntVar.empty() },
        nodeValue = newBoolVarArray()
    )

@Suppress("LocalVariableName")
private fun Solver.declareIncrementalVariables(P: Int, oldBF: BFVariables): BFVariables {
    val oldP = oldBF.P
    val X = oldBF.X
    val U = oldBF.U

    val nodeType: DomainVarArray<NodeType> = DomainVarArray.new(P) { (p) ->
        if (p <= oldP) oldBF.nodeType[p]
        else newDomainVar(NodeType.values().asIterable())
    }
    val nodeInputVariable: IntVarArray = IntVarArray.new(P) { (p) ->
        if (p <= oldP) oldBF.nodeInputVariable[p]
        else newIntVar(0..X)
    }
    val nodeParent: IntVarArray = IntVarArray.new(P) { (p) ->
        if (p <= oldP) oldBF.nodeParent[p]
        else newIntVar(domain = if (p == 1) listOf(0) else 1 until P)
    }
    val nodeChild: IntVarArray = IntVarArray.new(P) { (p) ->
        if (p <= oldP) {
            IntVar.new(((p + 1)..P) + 0) { c ->
                if (c <= oldP) oldBF.nodeChild[p] eq c else newLiteral()
            }
        } else {
            newIntVar(((p + 1)..P) + 0, encodeOneHot = false)
        }
    }
    val nodeValue = newBoolVarArray(P, U) { (p, u) ->
        if (p <= oldP) oldBF.nodeValue[p, u]
        else newLiteral()
    }

    return BFVariables(
        P = P, X = X, U = U,
        inputs = oldBF.inputs,
        values = oldBF.values,
        nodeType = nodeType,
        nodeInputVariable = nodeInputVariable,
        nodeParent = nodeParent,
        nodeChild = nodeChild,
        nodeValue = nodeValue
    )
}

private fun Solver.declareIncrementalReduction(vars: BFVariables): Int = with(vars) {
    val activation = newLiteral()

    // nodes have exactly one parent
    for (p in 2..P) {
        val lits = (1 until p).map { par -> nodeParent[p] eq par }
        if (lits.isNotEmpty()) {
            atMostOne(lits)
            addClause(lits + (-activation))
        }
    }

    // nodes have exactly one child
    for (p in 1..P) {
        val lits = (((p + 1)..P) + 0).map { c -> nodeChild[p] eq c }
        if (lits.isNotEmpty()) {
            atMostOne(lits)
            addClause(lits + (-activation))
        }
    }

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

    if (GlobalsBF.IS_ENCODE_BFS) {
        comment("BFS")
        // nodeParent[j + 1] >= nodeParent[j]
        for (j in 3 until P)
            for (i in 2 until j)
                for (s in 1 until i)
                    imply(
                        nodeParent[j] eq i,
                        nodeParent[j + 1] neq s
                    )
    }

    /* TERMINAL nodes constraints */

    comment("Only terminal nodes have associated input variables")
    // (nodeType[p] = TERMINAL) <=> (nodeInputVariable[p] != 0)
    iff(
        nodeType[P] eq NodeType.TERMINAL,
        nodeInputVariable[P] neq 0
    )

    comment("Terminals do not have children")
    // (nodeType[p] = TERMINAL) => (nodeChild[p] = 0)
    imply(
        nodeType[P] eq NodeType.TERMINAL,
        nodeChild[P] eq 0
    )

    /* AND/OR nodes constraints */

    comment("AND/OR nodes cannot have numbers P-1 or P")
    // (nodeType[P] != AND/OR) & (nodeType[P-1] != AND/OR)
    for (t in listOf(NodeType.AND, NodeType.OR)) {
        if (P >= 1) imply(activation, nodeType[P] neq t)
        if (P >= 2) imply(activation, nodeType[P - 1] neq t)
    }

    comment("AND/OR: left child cannot have number P")
    // (nodeType[p] = AND/OR) => (nodeChild[p] != P)
    for (t in listOf(NodeType.AND, NodeType.OR))
        for (p in 1 until (P - 1))
            implyImply(
                activation,
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
    if (P >= 1) imply(activation, nodeType[P] neq NodeType.NOT)

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

    return activation
}

private fun Solver.makeInductionStep(
    P: Int,
    tt: Map<Row, Boolean>,
    timeout: Double = GlobalsBF.timeout,
    previousAssumption: Int?,
    old: BFVariables?,
): Pair<BFAssignment?, Pair<Int, BFVariables>?> {
    require(P >= 1)
    val timeStart = PerformanceCounter.reference
    val nvarStart = numberOfVariables
    val nconStart = numberOfClauses

    if (previousAssumption != null) {
        addClause(-previousAssumption)
    }

    val vars: BFVariables = if (old != null) {
        declareIncrementalVariables(P, old)
    } else {
        declareIncrementalVariables(P, emptyBFVariables(tt))
    }
    val activation = declareIncrementalReduction(vars)
    val nvarDiff = numberOfVariables - nvarStart
    val nconDiff = numberOfClauses - nconStart
    println(
        "Done declaring variables ($nvarDiff) and constraints ($nconDiff) in %.3f s."
            .format(timeSince(timeStart).seconds)
    )

    // with(vars) {
    //     for (p in 1..P) {
    //         comment("nodeType[p = $p]: ${nodeType[p].literals}")
    //     }
    //     for (p in 1..P) {
    //         comment("nodeInputVariable[p = $p]: ${nodeInputVariable[p].literals}")
    //     }
    //     for (p in 1..P) {
    //         comment("nodeParent[p = $p]: ${nodeParent[p].literals}")
    //     }
    //     for (p in 1..P) {
    //         comment("nodeChild[p = $p]: ${nodeChild[p].literals}")
    //     }
    //     for (p in 1..P) {
    //         comment("nodeValue[p = $p]: ${(1..U).map { u -> nodeValue[p, u] }}")
    //     }
    //     comment("rootValue: ${(1..U).map { u -> nodeValue[1, u] }}")
    // }

    val cnf = File("out/cnf-${ttToBinaryString(tt)}-P$P.dimacs")
    dumpDimacs(cnf)
    cnf.appendText("$activation 0\n")

    println("Solving...")
    val timeStartSolving = PerformanceCounter.reference
    val isSat =
        if (timeout > 0) runWithTimeout((timeout * 1000).toLong()) { solve(activation) }
        else solve(activation)
    val solvingTime = timeSince(timeStartSolving)

    return if (isSat) {
        println("SAT for P = $P in %.3f s".format(solvingTime.seconds))
        Pair(
            BFAssignment.fromModel(getModel(), vars)
                .also {
                    val f = it.toLogic()
                    println("f = ${f.toPrettyString()}")
                    if (isBooleanFunctionCompliesWithTruthTable(f, tt)) {
                        println("Check: Ok")
                    } else {
                        error("Boolean function does not comply with truth table")
                    }
                },
            null
        )
    } else {
        println("UNSAT for P = $P in %.3f s".format(solvingTime.seconds))
        Pair(null, Pair(activation, vars))
    }
}

fun solveIncrementally(
    tt: Map<Row, Boolean>,
    Pmax: Int = GlobalsBF.Pmax,
    timeout: Double = GlobalsBF.timeout,
    quite: Boolean = false,
): BFAssignment? {
    val timeStart = PerformanceCounter.reference
    var result: Pair<BFAssignment?, Pair<Int, BFVariables>?>
    var prevStep: Pair<Int?, BFVariables?> = Pair(null, null)
    println("Searching BF by Incremental strategy for the truth table '${ttToBinaryString(tt)}'...")
    GlobalsBF.solverProvider().useWith {
        for (P in 1..Pmax) {
            println("\nTrying P = $P")
            result = makeInductionStep(
                P = P,
                tt = tt,
                timeout = timeout - timeSince(timeStart).seconds,
                previousAssumption = prevStep.first,
                old = prevStep.second
            )
            val assignment = result.first
            if (assignment != null) {
                if (!quite) {
                    println("Solution found!")
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
            prevStep = result.second!!
        }
        return null
    }
}
