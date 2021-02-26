package com.github.lipen.satlib.core

import com.github.lipen.multiarray.IntMultiArray
import com.github.lipen.multiarray.MultiArray
import com.github.lipen.satlib.solver.Solver

typealias DomainVarArray<T> = MultiArray<DomainVar<T>>
typealias IntVarArray = MultiArray<IntVar>

class BoolVarArray @PublishedApi internal constructor(
    private val backend: IntMultiArray,
) : MultiArray<Lit> by backend {
    companion object {
        @JvmStatic
        @JvmName("newVararg")
        inline fun new(
            vararg shape: Int,
            zerobased: Boolean = false,
            init: (IntArray) -> Lit,
        ): BoolVarArray = new(shape, zerobased, init)

        @JvmStatic
        @Suppress("FunctionName")
        inline fun new(
            shape: IntArray,
            zerobased: Boolean = false,
            init: (IntArray) -> Lit,
        ): BoolVarArray = BoolVarArray(IntMultiArray.new(shape, zerobased, init))
    }
}

inline fun <T> Solver.newDomainVarArray(
    vararg shape: Int,
    zerobased: Boolean = false,
    encodeOneHot: Boolean = true,
    init: (T) -> Lit = { newLiteral() },
    domain: (IntArray) -> Iterable<T>,
): DomainVarArray<T> = DomainVarArray.new(shape, zerobased) { index ->
    newDomainVar(domain(index), encodeOneHot, init)
}

inline fun Solver.newIntVarArray(
    vararg shape: Int,
    zerobased: Boolean = false,
    encodeOneHot: Boolean = true,
    init: (Int) -> Lit = { newLiteral() },
    domain: (IntArray) -> Iterable<Int>,
): IntVarArray = IntVarArray.new(shape, zerobased) { index ->
    newIntVar(domain(index), encodeOneHot, init)
}

inline fun Solver.newBoolVarArray(
    vararg shape: Int,
    zerobased: Boolean = false,
    init: (IntArray) -> Lit = { newLiteral() },
): BoolVarArray = BoolVarArray.new(shape, zerobased, init)
