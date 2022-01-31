package com.github.lipen.satlib.nexus.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import com.github.lipen.satlib.nexus.aig.parseAig
import com.github.lipen.satlib.nexus.eqgates.searchEqGates
import mu.KotlinLogging
import java.nio.file.Path

private val logger = KotlinLogging.logger {}

class EqGatesCommand : CliktCommand() {
    private val pathAig: Path by option(
        "-i", "--input",
        metavar = "<path>",
        help = "File with And-Inverter graph in ASCII AIGER format"
    ).path(canBeDir = false).required()

    private val solverType: SolverType by solverTypeOption()

    override fun run() {
        val aig = parseAig(pathAig)
        val solverProvider = solverType.solverProvider()
        searchEqGates(aig, solverProvider)
    }
}
