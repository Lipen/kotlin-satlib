@file:Suppress("LocalVariableName")

package com.github.lipen.satlib.nexus.encoding

import com.github.lipen.satlib.core.BoolVarArray
import com.github.lipen.satlib.core.newBoolVarArray
import com.github.lipen.satlib.nexus.utils.iffXor2
import com.github.lipen.satlib.solver.Solver
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

internal fun Solver.encodeMiter() {
    /* Constants */

    val Y: Int = context["Y"]

    /* Variables */

    val outputValueLeft: BoolVarArray = context["left.outputValue"]
    val outputValueRight: BoolVarArray = context["right.outputValue"]
    val xorValue = context("xorValue") {
        newBoolVarArray(Y)
    }

    /* Constraints */

    comment("Miter XORs")
    for (y in 1..Y) {
        iffXor2(
            xorValue[y],
            outputValueLeft[y],
            outputValueRight[y],
        )
    }

    comment("Miter OR")
    addClause((1..Y).map { y -> xorValue[y] })
    // addClause(xorValue.values)
}
