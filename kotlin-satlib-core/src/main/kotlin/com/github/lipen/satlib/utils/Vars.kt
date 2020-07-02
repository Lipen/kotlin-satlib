package com.github.lipen.satlib.utils

import com.github.lipen.multiarray.IntMultiArray
import com.github.lipen.multiarray.MultiArray

typealias Lit = Int
typealias LitArray = IntArray

infix fun Lit.sign(b: Boolean): Lit = if (b) this else -this

class DomainVar<T>(
    val storage: Map<T, Lit>
) {
    val domain: Set<T> = storage.keys
    val literals: Collection<Lit> = storage.values

    // TODO: return falseLiteral if value is not in storage
    infix fun eq(value: T): Lit = storage.getValue(value)
    infix fun neq(value: T): Lit = -eq(value)

    companion object {
        inline fun <T> new(domain: Iterable<T>, init: (T) -> Lit): DomainVar<T> =
            DomainVar(domain.associateWith { init(it) })
    }
}

typealias IntVar = DomainVar<Int>

typealias DomainVarArray<T> = MultiArray<DomainVar<T>>

typealias IntVarArray = MultiArray<IntVar>

class BoolVarArray @PublishedApi internal constructor(
    private val backend: IntMultiArray
) : MultiArray<Lit> by backend {
    companion object Factory {
        @JvmStatic
        inline fun create(
            vararg shape: Int,
            init: (IntArray) -> Lit
        ): BoolVarArray =
            create_(shape, init)

        @JvmStatic
        @Suppress("FunctionName")
        inline fun create_(
            shape: IntArray,
            init: (IntArray) -> Lit
        ): BoolVarArray =
            BoolVarArray(IntMultiArray.create(shape, init))
    }
}
