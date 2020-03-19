package com.github.lipen.satlib.core

import com.github.lipen.multiarray.IntMultiArray
import com.github.lipen.multiarray.MultiArray

typealias Lit = Int

infix fun Lit.sign(b: Boolean): Lit = if (b) this else -this

class DomainVar<T>(
    val storage: Map<T, Lit>
) {
    val domain: Set<T> = storage.keys
    val literals: Collection<Lit> = storage.values

    constructor(domain: Iterable<T>, init: (T) -> Lit) :
        this(domain.associateWith { init(it) })

    // TODO: return falseLiteral is value is not in storage
    infix fun eq(value: T): Lit = storage.getValue(value)
    infix fun neq(value: T): Lit = -eq(value)
}

typealias IntVar = DomainVar<Int>

typealias DomainVarArray<T> = MultiArray<DomainVar<T>>

typealias IntVarArray = MultiArray<IntVar>

class BoolVarArray private constructor(
    private val backend: IntMultiArray
) : MultiArray<Lit> by backend {
    companion object Factory {
        @JvmStatic
        fun create(
            shape: IntArray,
            init: (IntArray) -> Lit
        ): BoolVarArray =
            BoolVarArray(IntMultiArray.create(shape, init))

        @JvmStatic
        @JvmName("createVararg")
        fun create(
            vararg shape: Int,
            init: (IntArray) -> Lit
        ): BoolVarArray =
            create(shape, init)
    }
}
