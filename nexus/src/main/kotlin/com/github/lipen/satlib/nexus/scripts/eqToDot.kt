package com.github.lipen.satlib.nexus.scripts

import com.github.lipen.satlib.utils.writeln
import com.github.lipen.satlib.nexus.aig.convertAigToDot
import com.github.lipen.satlib.nexus.aig.parseAig
import okio.buffer
import okio.sink
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.pathString

@Suppress("Reformat")
fun main() {
    // val input = Path("data/instances/ISCAS/aag/c432.aag")
    // val eqivalentPairs = listOf(37 to 72, 38 to 74, 40 to 78, 43 to 83, 46 to 88, 49 to 91, 52 to 93, 55 to 95, 56 to 97, 71 to 73, 71 to 75, 71 to 79, 71 to 84, 71 to 89, 71 to 92, 71 to 94, 71 to 96, 71 to 98, 73 to 75, 73 to 79, 73 to 84, 73 to 89, 73 to 92, 73 to 94, 73 to 96, 73 to 98, 75 to 79, 75 to 84, 75 to 89, 75 to 92, 75 to 94, 75 to 96, 75 to 98, 79 to 84, 79 to 89, 79 to 92, 79 to 94, 79 to 96, 79 to 98, 84 to 89, 84 to 92, 84 to 94, 84 to 96, 84 to 98, 89 to 92, 89 to 94, 89 to 96, 89 to 98, 92 to 94, 92 to 96, 92 to 98, 94 to 96, 94 to 98, 96 to 98, 109 to 135, 110 to 138, 112 to 141, 114 to 146, 116 to 151, 118 to 154, 120 to 156, 122 to 158, 123 to 160, 134 to 136, 134 to 139, 134 to 142, 134 to 147, 134 to 152, 134 to 155, 134 to 157, 134 to 159, 134 to 161, 136 to 139, 136 to 142, 136 to 147, 136 to 152, 136 to 155, 136 to 157, 136 to 159, 136 to 161, 139 to 142, 139 to 147, 139 to 152, 139 to 155, 139 to 157, 139 to 159, 139 to 161, 142 to 147, 142 to 152, 142 to 155, 142 to 157, 142 to 159, 142 to 161, 147 to 152, 147 to 155, 147 to 157, 147 to 159, 147 to 161, 152 to 155, 152 to 157, 152 to 159, 152 to 161, 155 to 157, 155 to 159, 155 to 161, 157 to 159, 157 to 161, 159 to 161, 227 to 232, 233 to 236)

    val input = Path("data/instances/BubbleSort/aag/BubbleSort_4_3.aag")
    val eqivalentPairs = listOf(13 to 19, 13 to 21, 16 to 17, 18 to 20, 19 to 21, 23 to 24, 48 to 54, 48 to 56, 51 to 52, 53 to 55, 54 to 56, 58 to 59, 83 to 89, 83 to 91, 86 to 87, 88 to 98, 88 to 100, 89 to 91, 90 to 92, 93 to 96, 97 to 99, 98 to 100, 102 to 103, 105 to 106, 153 to 158, 153 to 160, 154 to 157, 158 to 160, 159 to 161, 163 to 164, 188 to 193, 188 to 195, 190 to 192, 193 to 195, 194 to 196, 198 to 199)

    val output = Path("data/dot/${input.nameWithoutExtension}-eq.dot")
    val aig = parseAig(input.pathString)
    val nodes = aig.layers().flatten().toList()
    val eqIds = eqivalentPairs.map { (a, b) -> nodes[a - 1] to nodes[b - 1] }
    output.parent.createDirectories()
    output.sink().buffer().use {
        println("Dumping AIG to DOT '$output'")
        for (line in convertAigToDot(aig, true, eqIds)) {
            it.writeln(line)
        }
    }
}
