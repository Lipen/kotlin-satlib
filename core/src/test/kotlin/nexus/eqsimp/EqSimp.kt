package nexus.eqsimp

import com.github.lipen.satlib.core.BoolVarArray
import com.github.lipen.satlib.core.convertBoolVarArray
import com.github.lipen.satlib.core.newBoolVarArray
import com.github.lipen.satlib.core.sign
import com.github.lipen.satlib.op.iffAnd
import com.github.lipen.satlib.solver.MiniSatSolver
import com.github.lipen.satlib.solver.Solver
import com.github.lipen.satlib.utils.useWith
import com.soywiz.klock.measureTimeWithResult
import examples.bf.toInt
import examples.utils.secondsSince
import examples.utils.timeNow
import mu.KotlinLogging
import nexus.aig.Aig
import nexus.aig.parseAig

private val log = KotlinLogging.logger {}

@Suppress("LocalVariableName")
private fun Solver.declareBase(aig: Aig) {
    context["aig"] = aig
    val nodes = aig.layers().flatten().toList()
    val V = context("V") { nodes.size }
    val id2index = (1..V).associateBy { nodes[it - 1] }
    val X = context("X") { aig.inputs.size }
    val Y = context("Y") { aig.outputs.size }

    log.info("V = $V, X = $Y, Y = $Y")

    val isUndecideGates = "true".toBoolean() // make non-input variables non-decision
    val isUnfreezeInputs = "true".toBoolean() // allow elimination of input variables
    // HAZARD! Do not unfreeze variables which you will be accessing in any way (model value OR assumption)
    val isUnfreezeGates = "false".toBoolean() // allow elimination of non-input variables (DO NOT!)
    val isAnyway = "false".toBoolean()

    val nodeValue = context("nodeValue") {
        if (isUnfreezeInputs || isUnfreezeGates || isUndecideGates || isAnyway) {
            if (this is MiniSatSolver) {
                newBoolVarArray(V) { (v) ->
                    if (v <= X) { // input
                        backend.newVariable(decision = true, frozen = !isUnfreezeInputs)
                    } else {
                        backend.newVariable(decision = !isUndecideGates, frozen = !isUnfreezeGates)
                    }
                }
            } /*else if (this is GlucoseSolver) {
                newBoolVarArray(V) { (v) ->
                    if (v <= X) { // input
                        backend.newVariable(decision = true, frozen = !isUnfreezeInputs)
                    } else {
                        backend.newVariable(decision = !isUndecide, frozen = !isUnfreezeGates)
                    }
                }
            }*/ else {
                log.warn("$this does not support customizing new variables")
                newBoolVarArray(V)
            }
        } else {
            newBoolVarArray(V)
        }
    }
    val inputValue = context("inputValue") {
        newBoolVarArray(X) { (x) ->
            val node = aig.inputs[x - 1]
            nodeValue[id2index.getValue(node.id)]
        }
    }
    val outputValue = context("outputValue") {
        newBoolVarArray(Y) { (y) ->
            val node = aig.outputs[y - 1]
            nodeValue[id2index.getValue(node.id)] sign !node.negated
        }
    }

    comment("AND gate semantics")
    for (v in (X + 1)..V) {
        val gate = aig.andGate(nodes[v - 1])
        iffAnd(
            nodeValue[v],
            nodeValue[id2index.getValue(gate.left.id)] sign !gate.left.negated,
            nodeValue[id2index.getValue(gate.right.id)] sign !gate.right.negated,
        )
    }

    log.info("Declared $numberOfVariables variables and $numberOfClauses clauses")
}

private fun Iterable<Boolean>.toBinaryString(): String =
    joinToString("") { it.toInt().toString() }

fun main() {
    val timeStart = timeNow()

    //region [samples]

    // val filename = "data/and.aag"
    // val filename = "data/halfadder.aag"
    // val filename = "data/eq.aag"
    // val filename = "data/eq2.aag"
    // val filename = "data/BubbleSort_4_3.aag"
    // val filename = "data/BubbleSort_6_4.aag"
    // val filename = "data/BubbleSort_7_4.aag"

    //endregion

    // region [instances with B]

    // val filename = "data/instances/ISCAS/aag/prolog.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s1196.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s1238.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s1269.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s13207.1.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s13207.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s1423.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s1488.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s1494.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s1512.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s15850.1.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s15850.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s208.1.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s27.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s298.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s3271.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s3330.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s3384.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s344.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s349.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s35932.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s382.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s38417.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s38584.1.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s38584.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s386.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s400.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s420.1.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s444.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s4863.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s499.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s510.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s526.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s526n.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s5378.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s635.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s641.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s6669.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s713.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s820.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s832.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s838.1.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s9234.1.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s9234.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s938.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s953.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s967.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s991.aag" // with B

    // endregion

    //region [invalid instances]

    // val filename = "data/instances/ISCAS/aag/c2670.aag" // with 0 output

    //endregion

    //region [valid ISCAS instances]

    // val filename = "data/instances/ISCAS/aag/c17.aag" // 1s, 0+0
    // val filename = "data/instances/ISCAS/aag/c432.aag" // 1s, 110+0
    // val filename = "data/instances/ISCAS/aag/c499.aag" // 5s, 0+0
    // val filename = "data/instances/ISCAS/aag/c880.aag" // 3s, 0+0
    val filename = "data/instances/ISCAS/aag/c1355.aag" // 8s, 0+0
    // val filename = "data/instances/ISCAS/aag/c1908.aag" // 5s, 1+1
    // val filename = "data/instances/ISCAS/aag/c3540.aag" // [long] 99s, 4+3
    // val filename = "data/instances/ISCAS/aag/c5315.aag" // [long] 217s, 21+0
    // val filename = "data/instances/ISCAS/aag/c6288.aag" // [long] ~?ks, ?+?
    // val filename = "data/instances/ISCAS/aag/c7552.aag" // [long] ?, ?+?

    //endregion

    val aig = parseAig(filename)
    log.info("aig = $aig")

    // for ((i, layer) in aig.layers().withIndex()) {
    //     log.info("Layer #${i + 1} (${layer.size} nodes): $layer")
    // }

    // CryptoMiniSatSolver().useWith {
    // GlucoseSolver().useWith {
    MiniSatSolver().useWith {
        // CadicalSolver().useWith {
        declareBase(aig)
        backend.thaw()

        val V: Int = context["V"]
        val X: Int = context["X"]
        val Y: Int = context["Y"]
        val nodeValueVar: BoolVarArray = context["nodeValue"]
        val inputValueVar: BoolVarArray = context["inputValue"]
        val outputValueVar: BoolVarArray = context["outputValue"]

        for (v in (X + 1)..V) {
            backend.freeze(nodeValueVar[v])
        }

        val (isSat, timeSolve) = measureTimeWithResult { solve() }
        println("${if (isSat) "SAT" else "UNSAT"} in %.3fs".format(timeSolve.seconds))

        if (isSat) {
            val model = getModel()
            val nodeValue = context.convertBoolVarArray("nodeValue", model)
            val inputValue = context.convertBoolVarArray("inputValue", model)
            val outputValue = context.convertBoolVarArray("outputValue", model)

            // println("model = ${model.data.toBinaryString()}")
            // println("nodeValue = ${nodeValue.values.toBinaryString()}")
            // println("input values: ${inputValue.values.toBinaryString()}")
            // println("output values: ${outputValue.values.toBinaryString()}")

            println("Searching for equivalent gates...")
            val equivalentPairs: MutableList<Pair<Int, Int>> = mutableListOf()
            val antiEquivalentPairs: MutableList<Pair<Int, Int>> = mutableListOf()
            for (v1 in (X + 1)..V) {
                if (backend.isEliminated(nodeValueVar[v1])) {
                    log.warn("Variable $v1 is eliminated")
                }
                for (v2 in (v1 + 1)..V) {
                    val (table, timeTableSolve) = measureTimeWithResult {
                        listOf(
                            Pair(false, false),
                            Pair(false, true),
                            Pair(true, false),
                            Pair(true, true)
                        ).map { (x1, x2) ->
                            val assumptions = listOf(
                                nodeValueVar[v1] sign x1,
                                nodeValueVar[v2] sign x2,
                            )
                            val (isSubSat, timeSubSolve) = measureTimeWithResult { solve(assumptions) }
                            // println("${if (isSubSat) "SAT" else "UNSAT"} assuming ($x1, $x2) in %.3fs".format(timeSubSolve.seconds))
                            isSubSat
                        }
                    }
                    // println("(v1,v2)=($v1,$v2) :: ${table.toBinaryString()}")
                    if (table.toBinaryString() == "1001") {
                        println(" -- Found equivalent gates: $v1 and $v2 in %.3f ms".format(timeTableSolve.milliseconds))
                        equivalentPairs.add(Pair(v1, v2))
                    }
                    if (table.toBinaryString() == "0110") {
                        println(" -- Found anti-equivalent gates: $v1 and $v2 in %.3f ms".format(timeTableSolve.milliseconds))
                        antiEquivalentPairs.add(Pair(v1, v2))
                    }
                }
                // Unfreeze variable `v1` that we won't use anymore
                backend.setFrozen(nodeValueVar[v1], false)
            }
            println("Done searching for equivalent gates")
            println("Total equivalent pairs (EQ+XOR): ${equivalentPairs.size} + ${antiEquivalentPairs.size} = ${equivalentPairs.size + antiEquivalentPairs.size} of total ${aig.ands.size} ANDs")

            // for (v in 1..V) {
            //     if (backend.isEliminated(nodeValueVar[v])) {
            //         println("Variable $v is eliminated")
            //     }
            // }
        } else {
            error("Unexpected UNSAT")
        }
    }

    log.info("All done in %.3f s".format(secondsSince(timeStart)))
}
