package com.github.lipen.satlib.core

import com.github.lipen.satlib.op.encodeOneHot
import com.github.lipen.satlib.solver.Solver

typealias IntVar = DomainVar<Int>

interface DomainVar<T> {
    val storage: Map<T, Lit>
    val domain: Set<T>
    val literals: Collection<Lit> // Note: proper order is *not* guaranteed

    companion object {
        fun <T> from(storage: Map<T, Lit>): DomainVar<T> = DomainVarImpl(storage)

        fun <T> empty(): DomainVar<T> = from(emptyMap())

        // constructor
        @JvmStatic
        inline fun <T> new(
            domain: Iterable<T>,
            init: (T) -> Lit,
        ): DomainVar<T> = from(domain.associateWith(init))
    }
}

infix fun <T> DomainVar<T>.eq(value: T): Lit = storage.getValue(value)
infix fun <T> DomainVar<T>.neq(value: T): Lit = -eq(value)

// infix fun <T> DomainVar<T>.safeEq(value: T): Lit = storage[value] ?: Solver.falseLiteral
// infix fun <T> DomainVar<T>.safeNeq(value: T): Lit = -safeEq(value)

private class DomainVarImpl<T>(
    override val storage: Map<T, Lit>,
) : DomainVar<T> {
    override val domain: Set<T> = storage.keys
    override val literals: Collection<Lit> = storage.values

    override fun toString(): String {
        return "OneHotDomainVar(domain = $domain)"
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
