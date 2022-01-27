package nexus.scripts

import com.github.lipen.satlib.utils.writeln
import nexus.aig.convertAigToDot
import nexus.aig.parseAig
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
        } catch (e: Exception) {
            println("Caught $e")
        }
    }
}
