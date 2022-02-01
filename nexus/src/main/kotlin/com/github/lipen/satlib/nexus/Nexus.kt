package com.github.lipen.satlib.nexus

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.output.CliktHelpFormatter
import com.github.lipen.satlib.nexus.cli.AigToDotCommand
import com.github.lipen.satlib.nexus.cli.EqCheckCommand
import com.github.lipen.satlib.nexus.cli.EqGatesCommand
import com.soywiz.klock.measureTime
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class Nexus(private val args: Array<String> = arrayOf("?")) : CliktCommand() {
    init {
        context {
            helpFormatter = CliktHelpFormatter(
                maxWidth = 999,
                requiredOptionMarker = "*",
                showDefaultValues = true,
                showRequiredTag = true
            )
        }
        subcommands(
            EqGatesCommand(),
            EqCheckCommand(),
            AigToDotCommand(),
        )
    }

    override fun run() {
        logger.info {
            "Args: ./nexus " + args.joinToString(" ") {
                if (it.contains(" ")) "\"${it.replace("\"", "\\\"")}\"" else it
            }
        }
    }
}

fun main(args: Array<String>) {
    val runningTime = measureTime { Nexus(args).main(args) }
    logger.info("All done in %.3f seconds".format(runningTime.seconds))
}
