package com.github.lipen.satlib.op

import com.github.lipen.satlib.core.DomainVar
import com.github.lipen.satlib.solver.Solver

fun <T> Solver.encodeOneHot(v: DomainVar<T>) {
    exactlyOne(v.literals)
}
