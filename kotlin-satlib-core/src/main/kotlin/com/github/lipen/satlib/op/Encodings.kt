package com.github.lipen.satlib.op

import com.github.lipen.satlib.core.DomainVar
import com.github.lipen.satlib.core.Lit
import com.github.lipen.satlib.core.eq
import com.github.lipen.satlib.solver.Solver
import kotlin.math.ceil
import kotlin.math.log2

fun <T> Solver.encodeOneHot(v: DomainVar<T>) {
    if (v.literals.isNotEmpty()) {
        exactlyOne(v.literals)
    } else {
        // TODO: emit warning
    }
}

fun <T> Solver.encodeOneHotBinary(v: DomainVar<T>): List<Lit> {
    /* OneHot+Binary encoding:
        o0 \/ o1 \/ o2 \/ o3
        o0 -> ~b0 /\ ~b1
        o1 ->  b0 /\ ~b1
        o2 -> ~b0 /\  b1
        o3 ->  b0 /\  b1
        ============================
        u1 \/ u2 \/ u3 \/...\/ uU
        u1 -> ~x1 /\ ~x2 /\.../\ ~xX
        u2 ->  x1 /\ ~x2 /\.../\ ~xX
        u3 -> ~x1 /\  x2 /\.../\ ~xX
        ...
        uU ->  x1 /\  x2 /\.../\  xX
    */

    require(v.literals.isNotEmpty())

    atLeastOne(v.literals)

    val n = v.domain.size
    if (n < 2) return emptyList()
    val m = ceil(log2(n.toDouble())).toInt()
    val bits = List(m) { newLiteral() }

    for ((i, x) in v.domain.withIndex())
        implyAnd(v eq x) {
            // Note: bits[0] is LSB, so we reverse the bitstring
            val s = i.toString(2).padStart(bits.size, '0').reversed()
            for ((j, b) in s.withIndex()) {
                when (b) {
                    '1' -> yield(bits[j])
                    '0' -> yield(-bits[j])
                    else -> error("Bad bit '$b'")
                }
            }
        }

    return bits
}
