package com.github.lipen.satlib.core

import com.github.lipen.multiarray.Index
import com.github.lipen.multiarray.MultiArray
import com.github.lipen.multiarray.Shape
import com.github.lipen.satlib.solver.Solver

typealias OneHotBinaryIntVarArray = MultiArray<OneHotBinaryIntVar>
typealias OneHotBinaryDomainVarArray<T> = MultiArray<OneHotBinaryDomainVar<T>>

inline fun <T> Solver.newOneHotBinaryDomainVarArray(
    vararg shape: Int,
    zerobased: Boolean = false,
    init: (T) -> Lit = { newLiteral() },
    domain: (Index) -> Iterable<T>,
): OneHotBinaryDomainVarArray<T> = OneHotBinaryDomainVarArray.new(Shape(shape), zerobased) { index ->
    newOneHotBinaryDomainVar(domain(index), init)
}

inline fun Solver.newOneHotBinaryIntVarArray(
    vararg shape: Int,
    init: (Int) -> Lit = { newLiteral() },
    domain: (Index) -> Iterable<Int>,
): OneHotBinaryIntVarArray = OneHotBinaryIntVarArray.new(Shape(shape)) { index ->
    newOneHotBinaryIntVar(domain(index), init)
}
