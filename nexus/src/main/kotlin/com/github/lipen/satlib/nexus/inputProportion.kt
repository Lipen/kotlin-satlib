package com.github.lipen.satlib.nexus

import com.github.lipen.satlib.solver.MiniSatSolver
import com.github.lipen.satlib.utils.useWith
import okio.Buffer
import okio.BufferedSink
import okio.BufferedSource
import okio.buffer
import okio.source
import java.io.File
import kotlin.math.absoluteValue
import kotlin.random.Random
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource

@OptIn(ExperimentalTime::class)
fun main() {
    val timeStart = TimeSource.Monotonic.markNow()

    // val cnfFile = File("data/q-CvK-12/merged.cnf")
    // val cnfFile = File("data/q-DvW-12/merged.cnf")
    val multType = "CvK"
    // val multType = "CvW"
    // val multType = "DvW"
    val cnfFile = File("data/q-${multType}-12/original.cnf")
    val cnfFile2 = File("data/qq/lec_${multType}_12.cnf_aaai_add_clauses_with_learnts_out.txt")
    println("Reading CNF from '$cnfFile'...")
    val cnf = CNF.from(cnfFile)
    val random = Random(42)
    val numInputs = 24

    MiniSatSolver().useWith {
        println("Allocating ${cnf.numVars} variables...")
        repeat(cnf.numVars) {
            newLiteral()
        }

        println("Adding ${cnf.clauses.size} clauses")
        for (clause in cnf.clauses) {
            addClause(clause)
        }

        println("Reading derived clauses from '$cnfFile2'...")
        val cnf2 = CNF.from(cnfFile2)
        check(cnf2.numVars <= cnf.numVars)
        println("Adding ${cnf2.clauses.size} derived clauses")
        for (clause in cnf2.clauses) {
            addClause(clause)
        }

        println("Pre-solving...")
        check(solve()) { "Must be SAT" }

        val numQueries = 1000
        var numSat = 0
        var numUnsat = 0

        println("Querying inputs $numQueries times...")
        for (i in 0 until numQueries) {
            println()
            check(numInputs <= 31)
            val f = random.nextInt(0, 1 shl numInputs)
            val fs = f.toString(2).padStart(numInputs, '0')
            val bits = fs.map { c ->
                when (c) {
                    '0' -> false
                    '1' -> true
                    else -> error("Bad bit '$c'")
                }
            }
            // first bit is the first input
            // last bit is the last input
            // `f == 1 == 00...00` corresponds to (x_1=0, x_2=0, ..., x_{N-1}=0, x_N=1)
            // `f == 2 == 00...10` corresponds to (x_1=0, x_2=0, ..., x_{N-1}=1, x_N=0)

            val cube = (1..numInputs).map { x -> if (bits[x - 1]) x else -x }
            println("f = $f = $fs, cube = $cube")
            val result = solve(cube)
            if (result) {
                println("SAT")
                numSat += 1
            } else {
                println("UNSAT")
                numUnsat += 1
            }
            println("p = $numSat/${(numSat+numUnsat)} = ${numSat.toDouble()/(numSat+numUnsat)}")
        }

        check(numSat + numUnsat == numQueries)
        println()
        println("Final number of SAT: $numSat of $numQueries")
    }

    println()
    println("All done in ${timeStart.elapsedNow()}")
}

typealias Clause = List<Int>

class CNF(
    val clauses: List<Clause>,
    val numVars: Int = determineMaxVariable(clauses),
) {
    fun writeDimacs(sink: BufferedSink, includeHeader: Boolean = true) {
        if (includeHeader) {
            sink.writeUtf8("p cnf $numVars ${clauses.size}\n")
        }
        for (clause in clauses) {
            for (lit in clause) {
                sink.writeUtf8(lit.toString()).writeUtf8(" ")
            }
            sink.writeUtf8("0\n")
        }
    }

    fun toDimacsString(includeHeader: Boolean = true): String {
        val buffer = Buffer()
        writeDimacs(buffer, includeHeader)
        return buffer.readUtf8()
    }

    companion object {
        fun from(source: BufferedSource): CNF {
            val clauses = parseDimacs(source).toList()
            return CNF(clauses)
        }

        fun from(file: File): CNF {
            return file.source().buffer().use { from(it) }
        }

        fun fromFile(filename: String): CNF {
            return from(File(filename))
        }

        fun fromString(string: String): CNF {
            return from(Buffer().writeUtf8(string))
        }
    }
}

fun determineMaxVariable(clauses: List<Clause>): Int {
    return clauses.maxOfOrNull { clause -> clause.maxOfOrNull { lit -> lit.absoluteValue } ?: 0 } ?: 0
}

private val RE_SPACE: Regex = """\s+""".toRegex()

fun parseDimacs(source: BufferedSource): Sequence<Clause> = sequence {
    while (true) {
        val line = source.readUtf8Line()?.trim() ?: break

        if (line.startsWith('c')) {
            // Skip comment
        } else if (line.isBlank()) {
            // Skip empty line
        } else if (line.startsWith('p')) {
            // Header
            val tokens = line.split(RE_SPACE)
            check(tokens[0] == "p") {
                "First header token must be 'p': \"$line\""
            }
            check(tokens[1] == "cnf") {
                "Second header token must be 'cnf': \"$line\""
            }
            check(tokens.size == 4) {
                "Header should have exactly 4 tokens: \"$line\""
            }
        } else {
            // Clause
            val tokens = line.split(RE_SPACE)
            check(tokens.last() == "0") {
                "Last token in clause must be '0': \"$line\""
            }
            val lits = tokens.dropLast(1).map { it.toInt() }
            val clause = lits
            yield(clause)
        }
    }
}
