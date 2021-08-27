package com.github.lipen.satlib.core

import com.github.lipen.multiarray.MultiArray
import com.github.lipen.satlib.core.Lit
import com.github.lipen.satlib.core.OneHotBinaryDomainVar
import com.github.lipen.satlib.core.OneHotBinaryIntVar
import com.github.lipen.satlib.core.newOneHotBinaryDomainVar
import com.github.lipen.satlib.core.newOneHotBinaryIntVar
import com.github.lipen.satlib.solver.Solver

typealias OneHotBinaryIntVarArray = MultiArray<OneHotBinaryIntVar>
typealias OneHotBinaryDomainVarArray<T> = MultiArray<OneHotBinaryDomainVar<T>>

inline fun <T> Solver.newOneHotBinaryDomainVarArray(
    vararg shape: Int,
    zerobased: Boolean = false,
    init: (T) -> Lit = { newLiteral() },
    domain: (IntArray) -> Iterable<T>,
): OneHotBinaryDomainVarArray<T> = OneHotBinaryDomainVarArray.new(shape, zerobased) { index ->
    newOneHotBinaryDomainVar(domain(index), init)
}

inline fun Solver.newOneHotBinaryIntVarArray(
    vararg shape: Int,
    init: (Int) -> Lit = { newLiteral() },
    domain: (IntArray) -> Iterable<Int>,
): OneHotBinaryIntVarArray = OneHotBinaryIntVarArray.new(shape) { index ->
    newOneHotBinaryIntVar(domain(index), init)
}
