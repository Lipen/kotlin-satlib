@file:Suppress("LocalVariableName")

package com.github.lipen.satlib.nexus.eqcheck

import com.github.lipen.satlib.core.BoolVarArray
import com.github.lipen.satlib.core.Lit
import com.github.lipen.satlib.core.newBoolVarArray
import com.github.lipen.satlib.core.sign
import com.github.lipen.satlib.nexus.aig.Aig
import com.github.lipen.satlib.nexus.aig.AigAndGate
import com.github.lipen.satlib.nexus.aig.AigInput
import com.github.lipen.satlib.nexus.aig.Ref
import com.github.lipen.satlib.nexus.aig.parseAig
import com.github.lipen.satlib.nexus.encoding.encodeAigs
import com.github.lipen.satlib.nexus.utils.iffXor2
import com.github.lipen.satlib.nexus.utils.secondsSince
import com.github.lipen.satlib.nexus.utils.timeNow
import com.github.lipen.satlib.solver.MockSolver
import com.github.lipen.satlib.utils.lineSequence
import com.github.lipen.satlib.utils.useWith
import com.github.lipen.satlib.utils.writeln
import mu.KotlinLogging
import okio.buffer
import okio.sink
import okio.source
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.math.absoluteValue
import kotlin.random.Random

private val logger = KotlinLogging.logger {}
private val random = Random(42)

fun dratMining(
    name: String,
    aigLeft: Aig,
    aigRight: Aig,
    solver: String,
) {
    logger.info { "name = $name" }

    val times = 5000
    var limitConflicts = 100_000

    // {index: [bool]}
    val inputValuesMap: MutableMap<Int, List<Boolean>> = mutableMapOf()
    // {index: {id: value}}
    val nodeValueLeftMap: MutableMap<Int, Map<Int, Boolean>> = mutableMapOf()
    // {index: {id: value}}
    val nodeValueRightMap: MutableMap<Int, Map<Int, Boolean>> = mutableMapOf()
    // {index: {lit: value}}
    val litValueLeftMap: MutableMap<Int, Map<Int, Boolean>> = mutableMapOf()
    // {index: {lit: value}}
    val litValueRightMap: MutableMap<Int, Map<Int, Boolean>> = mutableMapOf()
    // {index: {lit: value}}
    val litValueXorMap: MutableMap<Int, Map<Int, Boolean>> = mutableMapOf()
    // {index: {lit: value}}
    val litValueMap: MutableMap<Int, Map<Lit, Boolean>> = mutableMapOf()

    MockSolver().useWith {
        logger.info { "Encoding AIGs..." }
        encodeAigs(aigLeft, aigRight)

        val X: Int = context["X"]
        val Y: Int = context["Y"]
        val outputValueLeft: BoolVarArray = context["left.outputValue"]
        val outputValueRight: BoolVarArray = context["right.outputValue"]
        val xorValue = context("xorValue") {
            newBoolVarArray(Y)
        }

        val inputValueLeft: BoolVarArray = context["left.inputValue"]
        val inputValueRight: BoolVarArray = context["right.inputValue"]
        val andGateValueLeft: BoolVarArray = context["left.andGateValue"]
        val andGateValueRight: BoolVarArray = context["right.andGateValue"]

        fun nodeLit(
            id: Int,
            aig: Aig,
            inputValue: BoolVarArray,
            andGateValue: BoolVarArray,
        ): Lit {
            return when (val node = aig.node(id)) {
                is AigInput -> inputValue[aig.inputs.indexOf(node) + 1]
                is AigAndGate -> andGateValue[aig.andGates.indexOf(node) + 1]
            }
        }

        fun nodeLit(
            ref: Ref,
            aig: Aig,
            inputValue: BoolVarArray,
            andGateValue: BoolVarArray,
        ): Lit {
            return nodeLit(ref.id, aig, inputValue, andGateValue) sign !ref.negated
        }

        logger.info { "Poking inputs..." }
        repeat(times) { randomInputIndex ->
            if (randomInputIndex == 0 || (randomInputIndex + 1) % 1000 == 0) {
                logger.info { "iteration ${randomInputIndex + 1}/$times" }
            }

            // [bool]
            val inputValues = (1..aigLeft.inputs.size).map { random.nextBoolean() }
            // {id: value}
            val nodeValueLeft = aigLeft.eval(inputValues)
            // {id: value}
            val nodeValueRight = aigRight.eval(inputValues)

            // {lit: value}
            val litValueLeft = nodeValueLeft.mapKeys { (k, _) ->
                nodeLit(k, aigLeft, inputValueLeft, andGateValueLeft)
            }
            // {lit: value}
            val litValueRight = nodeValueRight.mapKeys { (k, _) ->
                nodeLit(k, aigRight, inputValueRight, andGateValueRight)
            }
            // {lit: value}
            val litValueXor = xorValue.values.mapIndexed { y0, lit ->
                val litLeft = nodeLit(aigLeft.outputs[y0], aigLeft, inputValueLeft, andGateValueLeft)
                val litRight = nodeLit(aigRight.outputs[y0], aigRight, inputValueRight, andGateValueRight)
                val valueLeft = litValueLeft[litLeft.absoluteValue]!! xor (litLeft < 0)
                val valueRight = litValueRight[litRight.absoluteValue]!! xor (litRight < 0)
                val value = valueLeft xor valueRight
                lit to value
            }.toMap()

            val litValue = (1..xorValue.values.last()).associateWith { lit ->
                when (lit.absoluteValue) {
                    in litValueLeft -> {
                        litValueLeft[lit.absoluteValue]!! xor (lit < 0)
                    }

                    in litValueRight -> {
                        litValueRight[lit.absoluteValue]!! xor (lit < 0)
                    }

                    in litValueXor -> {
                        litValueXor[lit.absoluteValue]!! xor (lit < 0)
                    }

                    else -> error("$lit is not found")
                }
            }

            // inputValuesMap[randomInputIndex] = inputValues
            // nodeValueLeftMap[randomInputIndex] = nodeValueLeft
            // nodeValueRightMap[randomInputIndex] = nodeValueRight
            // litValueLeftMap[randomInputIndex] = litValueLeft
            // litValueRightMap[randomInputIndex] = litValueRight
            // litValueXorMap[randomInputIndex] = litValueXor
            litValueMap[randomInputIndex] = litValue
        }
    }

    val savedLearnts: MutableList<List<Int>> = mutableListOf()

    repeat(10000) { iteration ->
        logger.info { "Global iteration #${iteration + 1}" }

        val pathCnf = Path("formula_$iteration.cnf")
        logger.info { "pathCnf = $pathCnf" }

        val pathDrat = Path("proof_$iteration.drat")
        logger.info { "pathDrat = $pathDrat" }

        val clauses: MutableList<List<Int>> = mutableListOf()
        MockSolver(
            __addClause = {
                clauses.add(it)
            },
            __dumpDimacs = { file ->
                file.sink().buffer().useWith {
                    writeln("p cnf $numberOfVariables $numberOfClauses")
                    for (clause in clauses) {
                        writeln("${clause.joinToString(" ")} 0")
                    }
                }
            }
        ).useWith {
            logger.info { "Encoding AIGs..." }
            encodeAigs(aigLeft, aigRight)

            val X: Int = context["X"]
            val Y: Int = context["Y"]
            val outputValueLeft: BoolVarArray = context["left.outputValue"]
            val outputValueRight: BoolVarArray = context["right.outputValue"]
            val xorValue = context("xorValue") {
                newBoolVarArray(Y)
            }

            comment("Miter XORs")
            for (y in 1..Y) {
                iffXor2(
                    xorValue[y],
                    outputValueLeft[y],
                    outputValueRight[y],
                )
            }

            comment("Miter OR")
            addClause((1..Y).map { y -> xorValue[y] })

            logger.info { "Adding ${savedLearnts.size} previously saved clauses" }
            for (clause in savedLearnts) {
                addClause(clause)
            }

            logger.info { "Dumping DIMACS to '$pathCnf'" }
            dumpDimacs(pathCnf.toFile())

            // val cmd = "$solver -c $limitConflicts --plain --inprocessing=false --no-binary $pathCnf $pathDrat"
            val cmd = "$solver -c $limitConflicts --unsat --no-binary $pathCnf $pathDrat"
            logger.info { "Running '$cmd'..." }
            val p = Runtime.getRuntime().exec(cmd)
            p.outputStream.close() // STDIN
            p.inputStream.close() // STDOUT
            p.waitFor()

            val drat = parseDrat(pathDrat)
            val numAdd = drat.count { it is DratEvent.Add }
            val numRemove = drat.count { it is DratEvent.Remove }
            logger.info { "Drat size: ${drat.size}" }
            logger.info { "Drat add: $numAdd" }
            logger.info { "Drat remove: $numRemove" }
            logger.info { "Drat final learnts: ${numAdd - numRemove}" }

            val learnts = drat
                .filterIsInstance<DratEvent.Add>()
                .map { it.clause.sortedBy { lit -> lit.absoluteValue } }
                .filter { it !in savedLearnts }
            logger.info { "Number of new learnts: ${learnts.size}" }

            val clauseUnsatCount = learnts.associateWithTo(mutableMapOf()) { 0 }
            // val clauseUnsatCount: MutableMap<List<Int>, Int> = mutableMapOf()

            logger.info { "Counting unsatisfied learnts..." }
            repeat(times) { randomInputIndex ->
                if (randomInputIndex == 0 || (randomInputIndex + 1) % 1000 == 0) {
                    logger.info { "iteration ${randomInputIndex + 1}/$times" }
                }

                val litValue = litValueMap[randomInputIndex]!!

                for (learnt in learnts) {
                    if (learnt.all { lit -> !litValue[lit.absoluteValue]!! xor (lit < 0) }) {
                        clauseUnsatCount.merge(learnt, 1, Int::plus)
                    }
                }
            }

            val goodLearnts = learnts.filter { clauseUnsatCount[it]!! > 0 }

            logger.info { "Statistics for top good learnts (of total ${goodLearnts.size}):" }
            for (learnt in goodLearnts.sortedByDescending { clauseUnsatCount[it]!! }.take(10)) {
                logger.info { "Clause $learnt, size = ${learnt.size}, SATs: ${times - clauseUnsatCount[learnt]!!}, UNSATs: ${clauseUnsatCount[learnt]!!}" }
            }

            logger.info { "Saving ${goodLearnts.size} learnts" }
            for (learnt in goodLearnts) {
                savedLearnts.add(learnt)
            }
            if (goodLearnts.isEmpty()) {
                limitConflicts *= 2
                logger.info { "Double the conflicts limit to $limitConflicts" }
            }
        }
    }
}

sealed class DratEvent {
    data class Add(val clause: List<Int>) : DratEvent()
    data class Remove(val clause: List<Int>) : DratEvent()
}

fun parseDrat(path: Path): List<DratEvent> {
    logger.info { "Parsing DRAT from '$path'..." }
    val result = mutableListOf<DratEvent>()
    path.source().buffer().use {
        for (line in it.lineSequence()) {
            if (line.isBlank()) continue

            if (line.startsWith("d ")) {
                val clause = parseClause(line.substring(2))
                result.add(DratEvent.Remove(clause))
            } else {
                val clause = parseClause(line)
                result.add(DratEvent.Add(clause))
            }
        }
    }
    return result
}

fun parseClause(s: String): List<Int> {
    val lits = s.split(" ").map { it.toInt() }
    check(lits.last() == 0) { "lits = $lits, s = '$s'" }
    return lits.dropLast(1)
}

fun main() {
    val timeStart = timeNow()

    val left = "BubbleSort"
    val right = "PancakeSort"
    val param = "10_4"
    val name = "${left.first().uppercase()}v${right.first().uppercase()}_$param"
    val aag = "fraag"

    val nameLeft = "${left}_${param}"
    val nameRight = "${right}_${param}"
    val filenameLeft = "data/instances/$left/$aag/$nameLeft.aag"
    val filenameRight = "data/instances/$right/$aag/$nameRight.aag"

    // val left = "column"
    // val right = "karatsuba"
    // val param = "16"
    // val name = "${left.first().uppercase()}v${right.first().uppercase()}_$param"
    // val aag = "aag" // "aag" or "fraag"
    //
    // val nameLeft = "${left}${param}x${param}"
    // val nameRight = "${right}${param}x${param}"
    // val filenameLeft = "data/instances/mult/$aag/$nameLeft.aag"
    // val filenameRight = "data/instances/mult/$aag/$nameRight.aag"
    // // val filenameCnf = "data/instances/mult/lec_cnf/lec_$name.cnf"

    val aigLeft = parseAig(filenameLeft)
    val aigRight = parseAig(filenameRight)
    val solver = "cadical"

    dratMining(name, aigLeft, aigRight, solver)

    logger.info("All done in %.3f s".format(secondsSince(timeStart)))
}
