@file:Suppress("LocalVariableName")

package examples.bf

import com.github.lipen.multiarray.BooleanMultiArray
import com.github.lipen.multiarray.IntMultiArray
import com.github.lipen.multiarray.MultiArray
import com.github.lipen.satlib.core.BoolVarArray
import com.github.lipen.satlib.core.DomainVarArray
import com.github.lipen.satlib.core.IntVarArray
import com.github.lipen.satlib.core.Model
import com.github.lipen.satlib.core.convert
import com.github.lipen.satlib.utils.writeln
import com.soywiz.klock.measureTimeWithResult
import examples.utils.pow
import okio.buffer
import okio.sink
import java.io.File

enum class NodeType(val value: Int) {
    TERMINAL(1),
    NOT(2),
    AND(3),
    OR(4);
}

data class Row(val values: List<Boolean>) {
    val size: Int = values.size
    val index: Int = values.reversed().withIndex().sumOf { (i, v) -> if (v) 2.pow(i) else 0 }

    constructor(i: Int, numberOfVariables: Int) :
        this(i.toLong(), numberOfVariables)

    constructor(i: Long, numberOfVariables: Int) :
        this(List(numberOfVariables) { j -> 2L.pow(numberOfVariables - j - 1) and i != 0L })
}

class BFVariables(
    val P: Int, // number of nodes
    val X: Int, // number of variables
    val U: Int, // number of inputs
    val inputs: List<List<Boolean>>,
    val values: List<Boolean>,
    val nodeType: DomainVarArray<NodeType>,
    val nodeInputVariable: IntVarArray,
    val nodeParent: IntVarArray,
    val nodeChild: IntVarArray,
    val nodeValue: BoolVarArray,
) {
    init {
        // for (p in 1..P) {
        //     println("nodeType[p = $p]: ${nodeType[p].literals}")
        // }
        // for (p in 1..P) {
        //     println("nodeInputVariable[p = $p]: ${nodeInputVariable[p].literals}")
        // }
        // for (p in 1..P) {
        //     println("nodeParent[p = $p]: ${nodeParent[p].literals}")
        // }
        // for (p in 1..P) {
        //     println("nodeChild[p = $p]: ${nodeChild[p].literals}")
        // }
        // for (p in 1..P) {
        //     println("nodeValue[p = $p]: ${(1..U).map { u -> nodeValue[p, u] }}")
        // }
        // println("rootValue: ${(1..U).map { u -> nodeValue[1, u] }}")
    }
}

fun <T> Iterable<T>.joinPadded(length: Int, separator: String = " "): String =
    joinToString(separator) { it.toString().padStart(length) }

class BFAssignment(
    val P: Int, // number of nodes
    val X: Int, // number of variables
    val U: Int, // number of inputs
    val nodeType: MultiArray<NodeType>,
    val nodeInputVariable: IntMultiArray,
    val nodeParent: IntMultiArray,
    val nodeChild: IntMultiArray,
    val nodeValue: BooleanMultiArray,
) {
    init {
        val padding: Int = when {
            P < 10 -> 2
            P < 100 -> 3
            else -> 4
        }
        println("                    ${(1..P).joinPadded(padding, "")}")
        println(
            "nodeType =          ${
                nodeType.values.map {
                    when (it) {
                        NodeType.TERMINAL -> "x"
                        NodeType.NOT -> "~"
                        NodeType.AND -> "&"
                        NodeType.OR -> "|"
                    }
                }.joinPadded(padding, "")
            }"
        )
        println("nodeInputVariable = ${nodeInputVariable.values.joinPadded(padding, "")}")
        println("nodeParent        = ${nodeParent.values.joinPadded(padding, "")}")
        println("nodeChild         = ${nodeChild.values.joinPadded(padding, "")}")
        // for (u in 1..U) {
        //     println("nodeValue[u = $u] = ${(1..P).map { p -> nodeValue[p, u] }.toBinaryString()}")
        // }

        // val bf = this.toLogic()
        // println("f = ${bf.toPrettyString()}")

        for (p in 1..P) {
            val c = nodeChild[p]
            if (c != 0) {
                check(nodeParent[c] == p) { "p = $p, c = $c" }
            }
        }
        for (c in 1..P) {
            val p = nodeParent[c]
            if (p != 0) {
                check(nodeChild[p] == c || nodeChild[p] == c - 1) { "c = $c, p = $p, nodeChild[p=$p] = ${nodeChild[p]}" }
            }
        }
    }

    fun toLogic(): Logic {
        val nodeMapping: MutableMap<Int, Logic> = mutableMapOf()
        for (p in P downTo 1) {
            nodeMapping[p] = when (nodeType[p]) {
                NodeType.TERMINAL -> Proposition(
                    name = "x${nodeInputVariable[p]}"
                )

                NodeType.NOT -> Not(
                    expr = nodeMapping[nodeChild[p]]!!
                )

                NodeType.OR -> Or(
                    lhs = nodeMapping[nodeChild[p]]!!,
                    rhs = nodeMapping[nodeChild[p] + 1]!!
                )

                NodeType.AND -> And(
                    lhs = nodeMapping[nodeChild[p]]!!,
                    rhs = nodeMapping[nodeChild[p] + 1]!!
                )
            }
        }
        return nodeMapping[1]!!
    }

    companion object {
        fun fromModel(model: Model, vars: BFVariables): BFAssignment = with(vars) {
            BFAssignment(
                P = P, X = X, U = U,
                nodeType = nodeType.convert(model),
                nodeInputVariable = nodeInputVariable.convert(model),
                nodeParent = nodeParent.convert(model),
                nodeChild = nodeChild.convert(model),
                nodeValue = nodeValue.convert(model)
            )
        }
    }
}

fun String.toBinary(): List<Boolean> =
    map {
        when (it) {
            '0' -> false
            '1' -> true
            else -> error("Bad char '$it'")
        }
    }

fun Iterable<Boolean>.toBinaryString(): String =
    joinToString("") { it.toInt().toString() }

fun valuesToTruthTable(values: List<Boolean?>, X: Int): Map<Row, Boolean> {
    val tt: MutableMap<Row, Boolean> = mutableMapOf()
    for ((i, b) in values.withIndex()) {
        if (b != null) {
            tt[Row(i, X)] = b
        }
    }
    return tt
}

@Suppress("LocalVariableName")
fun String.toTruthTable(X: Int): Map<Row, Boolean> {
    val values = map {
        when (it) {
            '0' -> false
            '1' -> true
            'x', '-' -> null
            else -> error("Bad char '$it'")
        }
    }
    return valuesToTruthTable(values, X)
}

@Suppress("LocalVariableName")
fun ttToBinaryString(tt: Map<Row, Boolean>): String {
    val X = tt.keys.first().size
    return (0 until 2.pow(X)).joinToString("") { f -> tt[Row(f, X)]?.toInt()?.toString() ?: "x" }
}

fun Boolean.toInt(): Int = if (this) 1 else 0

fun isBooleanFunctionCompliesWithTruthTable(f: Logic, tt: Map<Row, Boolean>): Boolean {
    val variables = (1..tt.keys.first().size).map { "x$it" }
    for ((row, value) in tt) {
        // println("f(${row.values.bf.toBinaryString()}) = ${f.eval(row.values, variables).bf.toInt()}")
        if (f.eval(row.values, variables) != value) return false
    }
    return true
}

fun solveAllIterative(
    X: Int,
    Pmax: Int = GlobalsBF.Pmax,
    timeout: Double = GlobalsBF.timeout,
) {
    File("results-inferAll$X-iterative.csv").sink().buffer().use { csv ->
        csv.writeln("f,bf,P,time")
        val U = 2.pow(X)
        val F = 2L.pow(U)
        for (f in 0 until F) {
            val values = Row(f, U).values
            val bin = values.toBinaryString()
            val tt = valuesToTruthTable(values, X)
            val (assignment, time) = measureTimeWithResult {
                solveIteratively(tt, Pmax = Pmax, timeout = timeout, quite = true)
            }
            if (assignment == null) {
                println("Could not infer BF for TT '$bin'")
                csv.writeln("$bin,?,,${time.seconds}").flush()
            } else {
                csv.writeln("$bin,${assignment.toLogic().toPrettyString()},${assignment.P},${time.seconds}").flush()
            }
        }
    }
}

fun solveAllIncremental(
    X: Int,
    Pmax: Int = GlobalsBF.Pmax,
    timeout: Double = GlobalsBF.timeout,
) {
    File("results-inferAll$X-incremental.csv").sink().buffer().use { csv ->
        csv.writeln("f,bf,P,time")
        val U = 2.pow(X)
        val F = 2L.pow(U)
        for (f in 0 until F) {
            val values = Row(f, U).values
            val bin = values.toBinaryString()
            val tt = valuesToTruthTable(values, X)
            val (assignment, time) = measureTimeWithResult {
                solveIncrementally(tt, Pmax = Pmax, timeout = timeout, quite = true)
            }
            if (assignment == null) {
                println("Could not infer BF for TT '$bin'")
                csv.writeln("$bin,?,,${time.seconds}").flush()
            } else {
                csv.writeln("$bin,${assignment.toLogic().toPrettyString()},${assignment.P},${time.seconds}").flush()
            }
        }
    }
}

fun solveAllInterleaved(
    X: Int,
    Pmax: Int = GlobalsBF.Pmax,
    timeout: Double = GlobalsBF.timeout,
) {
    val fileIterative = File("results-inferAll$X-iterative.csv")
    val fileIncremental = File("results-inferAll$X-incremental.csv")
    val header = "f,bf,P,time"
    fileIterative.sink().buffer().use { csvIterative ->
        csvIterative.writeln(header)
        fileIncremental.sink().buffer().use { csvIncremental ->
            csvIncremental.writeln(header)

            val U = 2.pow(X)
            val F = 2L.pow(U)

            for (f in 0 until F) {
                val values = Row(f, U).values
                val bin = values.toBinaryString()
                val tt = valuesToTruthTable(values, X)

                // Iterative
                val (assignmentIterative, timeIterative) = measureTimeWithResult {
                    solveIteratively(tt, Pmax = Pmax, timeout = timeout, quite = true)
                }
                if (assignmentIterative == null) {
                    println("Could not infer BF by Iterative for TT '$bin'")
                    csvIterative.writeln("$bin,?,,${timeIterative.seconds}").flush()
                } else {
                    csvIterative.writeln(
                        "$bin,${
                            assignmentIterative.toLogic().toPrettyString()
                        },${assignmentIterative.P},${timeIterative.seconds}"
                    ).flush()
                }

                // Incremental
                val (assignmentIncremental, timeIncremental) = measureTimeWithResult {
                    solveIncrementally(tt, Pmax = Pmax, timeout = timeout, quite = true)
                }
                if (assignmentIncremental == null) {
                    println("Could not infer BF by Incremental for TT '$bin'")
                    csvIncremental.writeln("$bin,?,,${timeIncremental.seconds}").flush()
                } else {
                    csvIncremental.writeln(
                        "$bin,${
                            assignmentIncremental.toLogic().toPrettyString()
                        },${assignmentIncremental.P},${timeIncremental.seconds}"
                    ).flush()
                }
            }
        }
    }
}

fun solveRandom(
    X: Int,
    n: Int,
    distribution: String = "01x",
    timeout: Double = GlobalsBF.timeout,
) {
    File("results-inferRandom$X.csv").sink().buffer().use { csv ->
        csv.writeln("tt,bf,P,time")
        for (i in 1..n) {
            var bin = (1..2.pow(X)).map { distribution.random() }.joinToString("")
            // Regenerate random string if it is all 'x's:
            while (bin.all { it == 'x' }) {
                bin = (1..2.pow(X)).map { distribution.random() }.joinToString("")
            }
            // ======
            val tt = bin.toTruthTable(X)
            val (assignment, time) = measureTimeWithResult {
                solveIteratively(tt, timeout = timeout, quite = true)
            }
            if (assignment == null) {
                println("Could not infer BF for TT '$bin'")
                csv.writeln("$bin,,,${time.seconds}")
                continue
            }
            val bf = assignment.toLogic()
            val P = assignment.P
            println(">>> f = $bin, P = $P, time = ${time.seconds}")
            csv.writeln("$bin,${bf.toPrettyString()},$P,${time.seconds}")
            csv.flush()
        }
    }
}
