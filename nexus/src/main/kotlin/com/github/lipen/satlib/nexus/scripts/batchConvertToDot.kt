package com.github.lipen.satlib.nexus.scripts

import com.github.lipen.satlib.utils.writeln
import com.github.lipen.satlib.nexus.aig.convertAigToDot
import com.github.lipen.satlib.nexus.aig.parseAig
import okio.buffer
import okio.sink
import kotlin.io.path.Path
import kotlin.io.path.div
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.pathString

fun main() {
    val directory = Path("data/instances/ISCAS/aag")
    // val directory = Path("data/instances/ITC99/aag")
    val outputDot = Path("data/dot")
    val outputPdf = outputDot.resolveSibling("pdf")

    for (path in directory.listDirectoryEntries("*.aag")) {
        try {
            val aig = parseAig(path.pathString)
            val pathDot = outputDot / (path.nameWithoutExtension + ".dot")
            pathDot.sink().buffer().use {
                println("Dumping AIG to DOT '$pathDot'")
                for (line in convertAigToDot(aig)) {
                    it.writeln(line)
                }
            }
            val pathPdf = outputPdf / (path.nameWithoutExtension + ".pdf")
            Runtime.getRuntime().exec("dot -Tpdf $pathDot -o $pathPdf")

            val pathDotNoRank = outputDot / (path.nameWithoutExtension + "-norank.dot")
            pathDotNoRank.sink().buffer().use {
                println("Dumping AIG to DOT without ranking by layers '$pathDot'")
                for (line in convertAigToDot(aig, rankByLayers = false)) {
                    it.writeln(line)
                }
            }
            val pathPdfNoRank = outputPdf / (path.nameWithoutExtension + "-norank.pdf")
            Runtime.getRuntime().exec("dot -Tpdf $pathDotNoRank -o $pathPdfNoRank")
        } catch (e: Exception) {
            println("Caught $e")
        }
    }
}
