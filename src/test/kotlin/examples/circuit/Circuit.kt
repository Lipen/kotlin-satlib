@file:Suppress("LocalVariableName")

package examples.circuit

import com.github.lipen.multiarray.MultiArray
import com.github.lipen.satlib.core.convertDomainVarArray
import com.github.lipen.satlib.core.eq
import com.github.lipen.satlib.core.neq
import com.github.lipen.satlib.core.newBoolVarArray
import com.github.lipen.satlib.core.newDomainVarArray
import com.github.lipen.satlib.core.sign
import com.github.lipen.satlib.op.imply
import com.github.lipen.satlib.op.implyAnd
import com.github.lipen.satlib.op.implyIff
import com.github.lipen.satlib.op.implyIffAnd
import com.github.lipen.satlib.op.implyIffOr
import com.github.lipen.satlib.op.implyImplyAnd
import com.github.lipen.satlib.solver.Solver
import com.github.lipen.satlib.solver.addClause
import com.github.lipen.satlib.solver.jni.CadicalSolver
import com.github.lipen.satlib.utils.useWith
import examples.bf.toBinaryString
import examples.utils.pow
import examples.utils.timeNow
import examples.utils.timeSince
import examples.utils.toBooleanArray
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

data class Input(
    val values: List<Boolean>,
) {
    constructor(values: BooleanArray) :
        this(values.asList())

    constructor(values: Iterable<Boolean>) :
        this(values.toList())

    constructor(values: String) :
        this(values.toBooleanArray())

    constructor(i: Int, numberOfVariables: Int) :
        this(i.toLong(), numberOfVariables)

    constructor(i: Long, numberOfVariables: Int) :
        this(List(numberOfVariables) { j -> 2L.pow(numberOfVariables - j - 1) and i != 0L })

    override fun toString(): String {
        return values.toBinaryString()
    }
}

data class Output(
    val values: List<Boolean>,
) {
    constructor(values: BooleanArray) :
        this(values.asList())

    constructor(values: Iterable<Boolean>) :
        this(values.toList())

    constructor(values: String) :
        this(values.toBooleanArray())

    override fun toString(): String {
        return values.toBinaryString()
    }
}

data class GateType(
    val name: String,
    val inputs: Int, // Number of gate inputs
    val outputs: Int, // Number of gate outputs
)

sealed class Location {
    data class Input(val index: Int) : Location()
    data class Output(val index: Int) : Location()
    data class Gate(val index: Int, val output: Int) : Location()

    object Disconnected : Location() {
        override fun toString(): String {
            return "Disconnected"
        }
    }
}

fun Solver.encodeCircuitSynthesis(
    tt: Map<Input, Output>, // Truth table
    G: Int, // Number of gates
    X: Int, // Number of inputs
    Z: Int, // Number of outputs
    gateTypes: List<GateType>, // Available types of gates
) {
    val XG = gateTypes.maxOf { it.inputs }
    val ZG = gateTypes.maxOf { it.outputs }
    context["XG"] = XG
    context["ZG"] = ZG

    val gateType = newDomainVarArray(G) { gateTypes }
    val gateParent = newDomainVarArray(G, XG) { (g, _) ->
        (1..X).map { x -> Location.Input(x) } +
            (1 until g).flatMap { gp -> (1..ZG).map { zg -> Location.Gate(gp, zg) } } +
            Location.Disconnected
    }
    val outputParent = newDomainVarArray(Z) {
        (1..X).map { x -> Location.Input(x) } +
            (1..G).flatMap { g -> (1..ZG).map { zg -> Location.Gate(g, zg) } } +
            Location.Disconnected
    }
    context["gateType"] = gateType
    context["gateParent"] = gateParent
    context["outputParent"] = outputParent

    val uniqueInputs: List<Input> = tt.keys.sortedBy { it.values.toBinaryString() }
    val U = uniqueInputs.size
    context["uniqueInputs"] = uniqueInputs
    context["U"] = U

    val inputValue = newBoolVarArray(X, U)
    val outputValue = newBoolVarArray(Z, U)
    val gateInputValue = newBoolVarArray(G, XG, U)
    val gateOutputValue = newBoolVarArray(G, ZG, U)
    context["inputValue"] = inputValue
    context["outputValue"] = outputValue
    context["gateInputValue"] = gateInputValue
    context["gateOutputValue"] = gateOutputValue

    comment("Input-Output semantics")
    for (u in 1..U) {
        val input = uniqueInputs[u - 1]
        for (x in 1..X) {
            addClause(inputValue[x, u] sign input.values[x - 1])
        }

        val output = tt.getValue(input)
        for (z in 1..Z) {
            addClause(outputValue[z, u] sign output.values[z - 1])
        }
    }

    comment("Output connections")
    for (u in 1..U) {
        for (z in 1..Z) {
            comment("Connected to input")
            for (x in 1..X) {
                implyIff(
                    outputParent[z] eq Location.Input(x),
                    outputValue[z, u],
                    inputValue[x, u]
                )
            }

            comment("Connected to gate")
            for (g in 1..G) {
                for (zg in 1..ZG) {
                    implyIff(
                        outputParent[z] eq Location.Gate(g, zg),
                        outputValue[z, u],
                        gateOutputValue[g, zg, u]
                    )
                }
            }

            comment("Disconnected outputs are False")
            imply(
                outputParent[z] eq Location.Disconnected,
                -outputValue[z, u]
            )
        }
    }

    comment("Gate connections")
    for (u in 1..U) {
        for (g in 1..G) {
            for (xg in 1..XG) {
                comment("Connected to input")
                for (x in 1..X) {
                    implyIff(
                        gateParent[g, xg] eq Location.Input(x),
                        gateInputValue[g, xg, u],
                        inputValue[x, u]
                    )
                }

                comment("Connected to another gate")
                for (gp in 1 until g) {
                    for (zg in 1..ZG) {
                        implyIff(
                            gateParent[g, xg] eq Location.Gate(gp, zg),
                            gateInputValue[g, xg, u],
                            gateOutputValue[gp, zg, u]
                        )
                    }
                }

                comment("Disconnected gate inputs are False")
                imply(
                    gateParent[g, xg] eq Location.Disconnected,
                    -gateInputValue[g, xg, u]
                )
            }
        }
    }

    comment("Disconnected inputs of gates are last")
    for (g in 1..G) {
        for (xg in 1 until XG) {
            imply(
                gateParent[g, xg] eq Location.Disconnected,
                gateParent[g, xg + 1] eq Location.Disconnected
            )
        }
    }

    comment("Unused inputs of gates are disconnected")
    for (g in 1..G) {
        for (t in gateTypes) {
            implyAnd(gateType[g] eq t) {
                for (xg in t.inputs + 1..XG) {
                    yield(gateParent[g, xg] eq Location.Disconnected)
                }
            }
        }
    }

    comment("Unused outputs of gates are False")
    for (u in 1..U) {
        for (g in 1..G) {
            for (t in gateTypes) {
                implyAnd(gateType[g] eq t) {
                    for (zg in t.outputs + 1..ZG) {
                        yield(-gateOutputValue[g, zg, u])
                    }
                }
            }
        }
    }

    comment("Unused outputs of gates are disconnected")
    for (gp in 1..G) {
        for (t in gateTypes) {
            implyAnd(gateType[gp] eq t) {
                for (zg in t.outputs + 1..ZG) {
                    // Another gate
                    for (gc in gp + 1..G) {
                        for (xg in 1..XG) {
                            yield(gateParent[gc, xg] neq Location.Gate(gp, zg))
                        }
                    }
                    // Output
                    for (z in 1..Z) {
                        yield(outputParent[z] neq Location.Gate(gp, zg))
                    }
                }
            }
        }
    }

    comment("Gate encoding")
    for (u in 1..U) {
        for (g in 1..G) {
            for (t in gateTypes) {
                when (t) {
                    GateType("AND", 2, 1) -> {
                        implyIffAnd(
                            gateType[g] eq t,
                            gateOutputValue[g, 1, u],
                            gateInputValue[g, 1, u],
                            gateInputValue[g, 2, u]
                        )
                    }

                    GateType("OR", 2, 1) -> {
                        implyIffOr(
                            gateType[g] eq t,
                            gateOutputValue[g, 1, u],
                            gateInputValue[g, 1, u],
                            gateInputValue[g, 2, u]
                        )
                    }

                    GateType("NAND", 2, 1) -> {
                        implyIffAnd(
                            gateType[g] eq t,
                            -gateOutputValue[g, 1, u],
                            gateInputValue[g, 1, u],
                            gateInputValue[g, 2, u]
                        )
                    }

                    GateType("NOR", 2, 1) -> {
                        implyIffOr(
                            gateType[g] eq GateType("NOR", 2, 1),
                            -gateOutputValue[g, 1, u],
                            gateInputValue[g, 1, u],
                            gateInputValue[g, 2, u]
                        )
                    }

                    GateType("XOR", 2, 1) -> {
                        // implyIffXor(
                        //     gateType[g] eq t,
                        //     gateOutputValue[g, 1, u],
                        //     gateInputValue[g, 1, u],
                        //     gateInputValue[g, 2, u]
                        // )
                        val x1 = gateType[g] eq t
                        val x2 = gateOutputValue[g, 1, u]
                        val x3 = gateInputValue[g, 1, u]
                        val x4 = gateInputValue[g, 2, u]
                        // x1 => (x2 <=> (x3 xor x4))
                        addClause(-x1, -x2, x3, x4)
                        addClause(-x1, -x2, -x3, -x4)
                        addClause(-x1, x2, -x3, x4)
                        addClause(-x1, x2, x3, -x4)
                    }

                    GateType("XNOR", 2, 1) -> {
                        // implyIffXor(
                        //     gateType[g] eq t,
                        //     -gateOutputValue[g, 1, u],
                        //     gateInputValue[g, 1, u],
                        //     gateInputValue[g, 2, u]
                        // )
                        val x1 = gateType[g] eq t
                        val x2 = -gateOutputValue[g, 1, u]
                        val x3 = gateInputValue[g, 1, u]
                        val x4 = gateInputValue[g, 2, u]
                        // x1 => (x2 <=> (x3 xor x4))
                        addClause(-x1, -x2, x3, x4)
                        addClause(-x1, -x2, -x3, -x4)
                        addClause(-x1, x2, -x3, x4)
                        addClause(-x1, x2, x3, -x4)
                    }

                    GateType("NOT", 1, 1) -> {
                        implyIff(
                            gateType[g] eq t,
                            gateOutputValue[g, 1, u],
                            gateInputValue[g, 1, u]
                        )
                    }

                    else -> error("$t is not supported")
                }
            }
        }

        // ADHOC
        comment("Order of gate inputs")
        for (g in 1..G) {
            for (t in gateTypes) {
                for (xg in 1 until t.inputs) {
                    // Another gate
                    for (g2 in 1 until g) {
                        for (zg in 1..ZG) {
                            implyImplyAnd(
                                gateType[g] eq t,
                                gateParent[g, xg] eq Location.Gate(g2, zg),
                            ) {
                                for (g3 in 1 until g2) {
                                    yield(gateParent[g, xg + 1] neq Location.Gate(g3, zg))
                                }
                                for (x in 1..X) {
                                    yield(gateParent[g, xg + 1] neq Location.Input(x))
                                }
                            }
                        }
                    }
                    // Input
                    for (x in 1..X) {
                        implyImplyAnd(
                            gateType[g] eq t,
                            gateParent[g, xg] eq Location.Input(x),
                        ) {
                            for (x2 in 1 until x) {
                                yield(gateParent[g, xg + 1] neq Location.Input(x2))
                            }
                        }
                    }
                }
            }
        }
    }
}

fun synthesizeCircuit(
    tt: Map<Input, Output>, // Truth table
    G: Int, // Number of gates
    X: Int, // Number of inputs
    Z: Int, // Number of outputs
    gateTypes: List<GateType>, // Available types of gates
): Boolean {
    CadicalSolver().useWith {
        encodeCircuitSynthesis(tt, G, X, Z, gateTypes)

        logger.info { "Solving..." }
        if (solve()) {
            logger.info { "G = $G: Success" }

            val model = getModel()

            val XG: Int = context["XG"]
            val ZG: Int = context["ZG"]

            val gateType: MultiArray<GateType> = context.convertDomainVarArray("gateType", model)
            val gateParent: MultiArray<Location> = context.convertDomainVarArray("gateParent", model)
            val outputParent: MultiArray<Location> = context.convertDomainVarArray("outputParent", model)

            logger.info { "gateType:" }
            for (g in 1..G) {
                logger.info { "  gateType[g=$g] = ${gateType[g]}" }
            }
            logger.info { "gateParent:" }
            for (g in 1..G) {
                logger.info { "  gateParent[g=$g] = ${(1..XG).map { xg -> gateParent[g, xg] }}" }
            }
            logger.info { "outputParent:" }
            for (z in 1..Z) {
                logger.info { "  outputParent[z=$z] = ${outputParent[z]}" }
            }

            return true
        } else {
            logger.info { "G = $G: Could not infer circuit" }
            return false
        }
    }
}

fun synthesizeCircuitBottomUp(
    tt: Map<Input, Output>, // Truth table
    X: Int, // Number of inputs
    Z: Int, // Number of outputs
    gateTypes: List<GateType>, // Available types of gates
): Int? {
    for (G in 1..30) {
        logger.info("")
        logger.info { "Trying G = $G for X=$X, Z=$Z..." }

        if (synthesizeCircuit(tt, G, X, Z, gateTypes)) {
            return G
        }
    }
    return null
}

fun main() {
    val timeStart = timeNow()

    val tt = mutableMapOf<Input, Output>()
    for (b0 in listOf(true, false)) {
        for (b1 in listOf(true, false)) {
            for (b2 in listOf(true, false)) {
                for (b3 in listOf(true, false)) {
                    val g0 = b0 xor b1
                    val g1 = b1 xor b2
                    val g2 = b2 xor b3
                    val g3 = b3
                    val input = Input(listOf(b3, b2, b1, b0))
                    val output = Output(listOf(g3, g2, g1, g0))
                    tt[input] = output
                }
            }
        }
    }

    logger.info { "Synthesizing the circuit using common gates" }
    synthesizeCircuit(
        tt, G = 3, X = 4, Z = 4, gateTypes = listOf(
            GateType("AND", 2, 1),
            GateType("OR", 2, 1),
            GateType("NAND", 2, 1),
            GateType("NOR", 2, 1),
            GateType("XOR", 2, 1),
            GateType("XNOR", 2, 1),
            GateType("NOT", 1, 1),
        )
    )

    println()
    logger.info { "Synthesizing the circuit using only NAND gates" }
    synthesizeCircuit(
        tt, G = 12, X = 4, Z = 4, gateTypes = listOf(
            GateType("NAND", 2, 1),
        )
    )

    logger.info("All done in %.3f s.".format(timeSince(timeStart).seconds))
}
