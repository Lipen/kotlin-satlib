package com.github.lipen.satlib.core

import com.github.lipen.satlib.op.exactlyOne
import com.github.lipen.satlib.solver.Solver

fun <T> Solver.encodeOneHot(v: DomainVar<T>) {
    exactlyOne(v.literals)
}
