package com.github.lipen.satlib.nexus.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.path
import com.github.lipen.satlib.nexus.aig.parseAig
import com.github.lipen.satlib.nexus.eqcheck.checkEquivalence
import mu.KotlinLogging
import java.nio.file.Path

private val logger = KotlinLogging.logger {}

class EqCheckCommand : CliktCommand() {
    private val pathAigLeft: Path by argument(
        "LEFT_AIG",
        help = "File with left AIG in ASCII AIGER format"
    ).path(canBeDir = false)
    private val pathAigRight: Path by argument(
        "RIGHT_AIG",
        help = "File with right AIG in ASCII AIGER format"
    ).path(canBeDir = false)

    private val method: String by option(
        "-m",
        "--method",
        help = "Method of equivalence check"
    ).choice("miter", "merge-eq", "merge-xor", "conj").required()

    private val solverType: SolverType by solverTypeOption()

    override fun run() {
        val aigLeft = parseAig(pathAigLeft)
        val aigRight = parseAig(pathAigRight)
        val solverProvider = solverType.solverProvider()
        checkEquivalence(aigLeft, aigRight, solverProvider, method)
    }
}
