package com.github.lipen.satlib.nexus.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import com.github.lipen.satlib.nexus.aig.parseAig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import mu.KotlinLogging
import okio.use
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.outputStream
import kotlin.random.Random

private val logger = KotlinLogging.logger {}

class ComputePTableCommand : CliktCommand() {
    private val pathAig: Path by option(
        "-i", "--input",
        metavar = "<path>",
        help = "File with And-Inverter graph in ASCII AIGER format"
    ).path(canBeDir = false).required()

    private val pathOutput: Path by option(
        "-o", "--output",
        metavar = "<path>",
        help = "Path to the resulting p-table"
    ).path(canBeDir = false).required()

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

    override fun run() {
        val aig = parseAig(pathAig)

        logger.info("Computing p-table using n=$sampleSize and randomSeed=$randomSeed...")
        val random = Random(randomSeed)
        val pTable = aig.computePTable(sampleSize, random)
        logger.info("Computed p-table of size ${pTable.size}")

        logger.info("Writing p-table to '$pathOutput'...")
        pathOutput.parent.createDirectories()
        val json = Json { prettyPrint = true }
        pathOutput.outputStream().use { out ->
            json.encodeToStream(pTable, out)
        }
    }
}
