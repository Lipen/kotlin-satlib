package com.github.lipen.satlib.op

import com.github.lipen.satlib.solver.Solver
import com.github.lipen.satlib.utils.DomainVar

fun <T> Solver.encodeOneHot(v: DomainVar<T>) {
    exactlyOne(v.literals)
}
