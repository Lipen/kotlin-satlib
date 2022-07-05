package com.github.lipen.satlib.nexus.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import com.github.lipen.satlib.core.BoolVarArray
import com.github.lipen.satlib.core.Lit
import com.github.lipen.satlib.nexus.aig.Aig
import com.github.lipen.satlib.nexus.aig.parseAig
import com.github.lipen.satlib.nexus.encoding.encodeAigs
import com.github.lipen.satlib.nexus.encoding.encodeMiter
import com.github.lipen.satlib.nexus.eqcheck.disbalance
import com.github.lipen.satlib.nexus.eqcheck.loadPTable
import com.github.lipen.satlib.nexus.utils.declare
import com.github.lipen.satlib.solver.CadicalSolver
import com.github.lipen.satlib.utils.useWith
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import mu.KotlinLogging
import okio.use
import java.nio.file.Path
import kotlin.io.path.outputStream
import kotlin.random.Random

private val logger = KotlinLogging.logger {}

class MakeMiterCommand : CliktCommand() {
    private val pathAigLeft: Path by argument(
        "LEFT_AIG",
        help = "File with left AIG in ASCII AIGER format (AAG)"
    ).path(canBeDir = false)
    private val pathAigRight: Path by argument(
        "RIGHT_AIG",
        help = "File with right AIG in ASCII AIGER format (AAG)"
    ).path(canBeDir = false)

    private val pathOutput: Path by option(
        "-o", "--output",
        metavar = "<path>",
        help = "Path to the resulting miter"
    ).path(canBeDir = false).required()

    private val pathPTableLeft: Path? by option(
        "--p-table-left",
        metavar = "<path>",
        help = "File with precomputed p-table for left scheme"
    ).path(canBeDir = false)
    private val pathPTableRight: Path? by option(
        "--p-table-right",
        metavar = "<path>",
        help = "File with precomputed p-table for right scheme"
    ).path(canBeDir = false)

    private val sampleSize: Int by option(
        "-n",
        "--sample-size",
        metavar = "<int>",
        help = "Sample size"
    ).int().default(10000)
    private val randomSeed: Int by option(
        "--random-seed",
        metavar = "<int>",
        help = "Random seed"
    ).int().default(42)

    private val pathMeta: Path? by option(
        "--meta",
        metavar = "<path>",
        help = "Path to the resulting meta-information"
    ).path(canBeDir = false)

    override fun run() {
        val aigLeft = parseAig(pathAigLeft)
        val aigRight = parseAig(pathAigRight)

        CadicalSolver().useWith {
            declare(logger) {
                encodeAigs(aigLeft, aigRight)
                encodeMiter()
            }

            pathOutput.toFile().parentFile.mkdirs()
            dumpDimacs(pathOutput.toFile())

            if (pathMeta != null) {
                val inputValue: BoolVarArray = context["inputValue"]
                val andGateValueLeft: BoolVarArray = context["left.andGateValue"]
                val andGateValueRight: BoolVarArray = context["right.andGateValue"]
                val outputValueLeft: BoolVarArray = context["left.outputValue"]
                val outputValueRight: BoolVarArray = context["right.outputValue"]
                val xorValue: BoolVarArray = context["xorValue"]

                if (pathPTableLeft != null) {
                    loadPTable(aigLeft, pathPTableLeft!!)
                }
                if (pathPTableRight != null) {
                    loadPTable(aigRight, pathPTableRight!!)
                }

                @Serializable
                data class GateDescription(
                    val schema: String,
                    val id: Int,
                    val variable: Lit,
                    val children: List<Int>,
                    val parents: List<Int>,
                    val p: Double,
                    val dis: Double,
                )

                fun getGates(schema: String, aig: Aig, andGateValue: BoolVarArray): List<GateDescription> {
                    val random = Random(randomSeed)
                    val pTable = aig.computePTable(sampleSize, random)
                    return aig.andGates.map { gate ->
                        val id = gate.id
                        val v = andGateValue[aig.andGates.indexOf(gate) + 1]
                        val children = aig.children(id).map { it.toInt() }
                        val parents = aig.parents(id).map { it.toInt() }
                        val p = pTable.getValue(id)
                        val dis = disbalance(p)
                        GateDescription(
                            schema = schema,
                            id = id,
                            variable = v,
                            children = children,
                            parents = parents,
                            p = p,
                            dis = dis,
                        )
                    }
                }

                val gatesLeft = getGates("left", aigLeft, andGateValueLeft)
                val gatesRight = getGates("right", aigRight, andGateValueRight)
                val gates = gatesLeft + gatesRight

                @Serializable
                data class MetaInfo(
                    val inputs: List<Lit>,
                    val xors: List<Lit>,
                    val outputsLeft: List<Lit>,
                    val outputsRight: List<Lit>,
                    val outputIdsLeft: List<Int>,
                    val outputIdsRight: List<Int>,
                    val gates: List<GateDescription>,
                )

                val meta = MetaInfo(
                    inputs = inputValue.values,
                    xors = xorValue.values,
                    outputsLeft = outputValueLeft.values,
                    outputsRight = outputValueRight.values,
                    outputIdsLeft = aigLeft.outputIds,
                    outputIdsRight = aigRight.outputIds,
                    gates = gates,
                )

                val json = Json { prettyPrint = true }
                pathMeta!!.toFile().parentFile.mkdirs()
                pathMeta!!.outputStream().use { out ->
                    json.encodeToStream(meta, out)
                }
            }
        }

        // logger.info("Writing p-table to '$pathOutput'...")
        // pathOutput.parent.createDirectories()
        // val json = Json { prettyPrint = true }
        // pathOutput.outputStream().use { out ->
        //     json.encodeToStream(pTable, out)
        // }
    }
}
