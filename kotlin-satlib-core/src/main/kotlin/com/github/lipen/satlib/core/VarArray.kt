package com.github.lipen.satlib.core

import com.github.lipen.multiarray.Index
import com.github.lipen.multiarray.MultiArray
import com.github.lipen.multiarray.Shape
import com.github.lipen.satlib.solver.Solver

typealias DomainVarArray<T> = MultiArray<DomainVar<T>>
typealias IntVarArray = MultiArray<IntVar>
typealias BoolVarArray = MultiArray<Lit>

inline fun <T> Solver.newDomainVarArray(
    vararg shape: Int,
    zerobased: Boolean = false,
    encodeOneHot: Boolean = true,
    init: (T) -> Lit = { newLiteral() },
    domain: (Index) -> Iterable<T>,
): DomainVarArray<T> = DomainVarArray.new(Shape(shape), zerobased) { index ->
    newDomainVar(domain(index), encodeOneHot, init)
}

inline fun Solver.newIntVarArray(
    vararg shape: Int,
    zerobased: Boolean = false,
    encodeOneHot: Boolean = true,
    init: (Int) -> Lit = { newLiteral() },
    domain: (Index) -> Iterable<Int>,
): IntVarArray = IntVarArray.new(Shape(shape), zerobased) { index ->
    newIntVar(domain(index), encodeOneHot, init)
}

inline fun Solver.newBoolVarArray(
    vararg shape: Int,
    zerobased: Boolean = false,
    init: (Index) -> Lit = { newLiteral() },
): BoolVarArray = BoolVarArray.new(Shape(shape), zerobased, init)

val <T> DomainVarArray<T>.literals: List<Lit>
    get() = values.flatMap { it.literals }

val BoolVarArray.literals: List<Lit>
    get() = values
