package com.github.lipen.satlib.nexus.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import com.github.lipen.satlib.nexus.aig.cone
import com.github.lipen.satlib.nexus.aig.convertTwoAigsToDot
import com.github.lipen.satlib.nexus.aig.parseAig
import com.github.lipen.satlib.nexus.aig.shadow
import com.github.lipen.satlib.utils.writeln
import mu.KotlinLogging
import okio.buffer
import okio.sink
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.div
import kotlin.io.path.nameWithoutExtension
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.random.Random

private val logger = KotlinLogging.logger {}

class AigsToDotCommand : CliktCommand() {
    private val pathAigLeft: Path by argument(
        "LEFT_AIG",
        help = "File with left AIG in ASCII AIGER format (AAG)"
    ).path(canBeDir = false)
    private val pathAigRight: Path by argument(
        "RIGHT_AIG",
        help = "File with right AIG in ASCII AIGER format (AAG)"
    ).path(canBeDir = false)

    private val pathDot: Path by option(
        "-o", "--output",
        metavar = "<path>",
        help = "Path to the resulting DOT"
    ).path().required()

    private val pathPdf: Path? by option(
        "-p",
        "--pdf",
        metavar = "<path>",
        help = "Path to the resulting PDF (just a dir with --reuse-name, or a complete path without it)"
    ).path()

    private val reuseName: Boolean by option(
        "--reuse-name",
        help = "Reuse the name of the input AIG for the resulting DOT/PDF"
    ).flag(
        "--no-reuse-name",
        default = true,
        defaultForHelp = "yes",
    )

    private val rankByLayers: Boolean by option(
        "--rank-by-layers",
        help = "Rank nodes by layers (from a topological sort)"
    ).flag(
        "--no-rank-by-layers",
        default = true,
        defaultForHelp = "yes",
    )
    private val inputsOnTop: Boolean by option(
        "--inputs-on-top",
        help = "Position of inputs"
    ).flag(
        "--inputs-on-bottom",
        default = false,
        defaultForHelp = "bottom",
    )

    private val computeDisbalance: Boolean by option(
        "--disbalance",
        help = "Compute disbalance of AIG nodes"
    ).flag(
        "--no-disbalance",
        default = false,
        defaultForHelp = "no",
    )
    private val sampleSize: Int by option(
        "-n",
        "--sample-size",
        metavar = "<int>",
        help = "Sample size for disbalance computation"
    ).int().default(10000)
    private val randomSeed: Int by option(
        "--random-seed",
        metavar = "<int>",
        help = "Random seed"
    ).int().default(42)
    private val printDisbalance: Boolean by option(
        "--print-disbalance",
        help = "Print disbalance table"
    ).flag(
        "--no-print-disbalance",
        default = false,
        defaultForHelp = "no",
    )
    private val computeStats: Boolean by option(
        "--stats",
        help = "Compute cones and shadows of AIG nodes"
    ).flag(
        "--no-stats",
        default = false,
        defaultForHelp = "no",
    )

    override fun run() {
        val aigLeft = parseAig(pathAigLeft)
        val aigRight = parseAig(pathAigRight)

        logger.info("Writing DOT to '$pathDot'...")
        pathDot.parent.createDirectories()
        pathDot.sink().buffer().use {
            val lines = if (computeDisbalance) {
                fun getP(t: Int, f: Int): Double {
                    return t.toDouble() / (t + f)
                }

                fun hex(x: Double): String {
                    require(x in 0.0..1.0)
                    return "%02x".format(min(255, (x * 256).roundToInt()))
                }

                val random = Random(randomSeed)
                logger.info("Computing disbalance table for left AIG using sampleSize=$sampleSize and randomSeed=$randomSeed...")
                val tfTableLeft = aigLeft.computeTFTable(sampleSize, random)
                logger.info("Computing disbalance table for right AIG using sampleSize=$sampleSize and randomSeed=$randomSeed...")
                val tfTableRight = aigRight.computeTFTable(sampleSize, random)

                if (printDisbalance) {
                    println("Table for $aigLeft:")
                    for ((id, tf) in tfTableLeft.entries.sortedBy { (_, tf) -> val (t, f) = tf; getP(t, f) }) {
                        val (t, f) = tf
                        println("  - $id: t=$t, f=$f, s=%.3f".format(getP(t, f)))
                    }
                    println("Table for $aigRight:")
                    for ((id, tf) in tfTableRight.entries.sortedBy { (_, tf) -> val (t, f) = tf; getP(t, f) }) {
                        val (t, f) = tf
                        println("  - $id: t=$t, f=$f, s=%.3f".format(getP(t, f)))
                    }
                }

                val nodeLabelLeft = tfTableLeft.mapValues { (id, p) ->
                    val (t, f) = p
                    if (computeStats) {
                        val cone = aigLeft.cone(id)
                        val shadow = aigLeft.shadow(id)
                        "\\N : %.3f\ncone: ${cone.size} (${cone.filter { it in aigLeft.inputIds }.size})\nshad: ${shadow.size} (${shadow.filter { it in aigLeft.outputIds }.size})".format(
                            getP(t, f)
                        )
                    } else {
                        "\\N: %.3f".format(getP(t, f))
                    }
                }
                val nodeLabelRight = tfTableRight.mapValues { (id, p) ->
                    val (t, f) = p
                    if (computeStats) {
                        val cone = aigRight.cone(id)
                        val shadow = aigRight.shadow(id)
                        "\\N : %.3f\ncone: ${cone.size} (${cone.filter { it in aigRight.inputIds }.size})\nshad: ${shadow.size} (${shadow.filter { it in aigRight.outputIds }.size})".format(
                            getP(t, f)
                        )
                    } else {
                        "\\N: %.3f".format(getP(t, f))
                    }
                }
                val nodeAddStyleLeft = tfTableLeft.mapValues { (_, p) ->
                    val (t, f) = p
                    val saturation = getP(t, f) // saturation
                    check(saturation in 0.0..1.0)
                    val mid = 0.25
                    val power = 3
                    if (saturation > mid) {
                        "style=filled,fillcolor=\"#00ff00${hex(((saturation - mid) / (1 - mid)).pow(power))}\""
                    } else {
                        "style=filled,fillcolor=\"#ff0000${hex(((mid - saturation) / mid).pow(power))}\""
                    }
                }
                val nodeAddStyleRight = tfTableRight.mapValues { (_, p) ->
                    val (t, f) = p
                    val saturation = getP(t, f) // saturation
                    check(saturation in 0.0..1.0)
                    val mid = 0.25
                    val power = 3
                    if (saturation > mid) {
                        "style=filled,fillcolor=\"#00ff00${hex(((saturation - mid) / (1 - mid)).pow(power))}\""
                    } else {
                        "style=filled,fillcolor=\"#ff0000${hex(((mid - saturation) / mid).pow(power))}\""
                    }
                }

                convertTwoAigsToDot(
                    aigLeft,
                    aigRight,
                    rankByLayers = rankByLayers,
                    inputsOnTop = inputsOnTop,
                    nodeLabelLeft = nodeLabelLeft,
                    nodeLabelRight = nodeLabelRight,
                    nodeAddStyleLeft = nodeAddStyleLeft,
                    nodeAddStyleRight = nodeAddStyleRight,
                )
            } else {
                convertTwoAigsToDot(
                    aigLeft,
                    aigRight,
                    rankByLayers = rankByLayers,
                    inputsOnTop = inputsOnTop,
                )
            }

            for (line in lines) {
                it.writeln(line)
            }
        }

        if (pathPdf != null) {
            val pathPdf = if (reuseName) {
                pathPdf!! / (pathDot.nameWithoutExtension + ".pdf")
            } else {
                pathPdf!!
            }
            logger.info("Rendering DOT to '$pathPdf'")
            pathPdf.parent.createDirectories()
            Runtime.getRuntime().exec("dot -Tpdf $pathDot -o $pathPdf").waitFor()
        }
    }
}
