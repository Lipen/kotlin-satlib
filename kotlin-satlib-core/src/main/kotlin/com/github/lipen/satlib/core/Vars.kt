package com.github.lipen.satlib.core

import com.github.lipen.multiarray.IntMultiArray
import com.github.lipen.multiarray.MultiArray
import com.github.lipen.satlib.op.encodeOneHot
import com.github.lipen.satlib.solver.Solver

typealias Lit = Int
typealias LitArray = IntArray
typealias SequenceScopeLit = suspend SequenceScope<Lit>.() -> Unit

infix fun Lit.sign(b: Boolean): Lit = if (b) this else -this

interface DomainVar<T> {
    val storage: Map<T, Lit>
    val domain: Set<T>
    val literals: Collection<Lit> // Note: proper order is *not* guaranteed

    infix fun eq(value: T): Lit = storage.getValue(value)
    infix fun neq(value: T): Lit = -eq(value)

    // infix fun safeEq(value: T): Lit
    // infix fun safeNeq(value: T): Lit = -safeEq(value)

    companion object {
        @JvmStatic
        inline fun <T> new(
            domain: Iterable<T>,
            init: (T) -> Lit,
        ): DomainVar<T> = DefaultDomainVar(domain, init)

        fun <T> empty(): DomainVar<T> = DefaultDomainVar(emptyMap())
    }
}

class DefaultDomainVar<T> @PublishedApi internal constructor(
    override val storage: Map<T, Lit>,
) : DomainVar<T> {
    override val domain: Set<T> = storage.keys
    override val literals: Collection<Lit> = storage.values

    override infix fun eq(value: T): Lit = storage.getValue(value)
    override infix fun neq(value: T): Lit = -eq(value)

    // override fun safeEq(value: T): Lit = storage[value] ?: Solver.falseLiteral
    // override fun safeNeq(value: T): Lit = -safeEq(value)

    override fun toString(): String {
        return "OneHotDomainVar(domain = $domain)"
    }

    companion object {
        inline operator fun <T> invoke(
            domain: Iterable<T>,
            init: (T) -> Lit,
        ): DefaultDomainVar<T> = DefaultDomainVar(domain.associateWith { init(it) })
    }
}

typealias IntVar = DomainVar<Int>
typealias IntVarArray = MultiArray<IntVar>
typealias DomainVarArray<T> = MultiArray<DomainVar<T>>

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

inline fun <T> Solver.newDomainVar(
    domain: Iterable<T>,
    encodeOneHot: Boolean = true,
    init: (T) -> Lit = { newLiteral() },
): DomainVar<T> {
    val v = DomainVar.new(domain, init)
    if (encodeOneHot) encodeOneHot(v)
    return v
}

inline fun Solver.newIntVar(
    domain: Iterable<Int>,
    encodeOneHot: Boolean = true,
    init: (Int) -> Lit = { newLiteral() },
): IntVar = newDomainVar(domain, encodeOneHot, init)

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
