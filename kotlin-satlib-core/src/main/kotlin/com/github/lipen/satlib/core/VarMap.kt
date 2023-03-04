package com.github.lipen.satlib.core

import com.github.lipen.satlib.solver.Solver
import com.github.lipen.satlib.utils.DomainMap
import com.github.lipen.satlib.utils.Domains
import com.github.lipen.satlib.utils.MutableDomainMap
import com.github.lipen.satlib.utils.Tuple

typealias DomainVarDomainMap<K, T> = DomainMap<K, DomainVar<T>>
typealias IntVarDomainMap<K> = DomainMap<K, IntVar>
typealias BoolVarDomainMap<K> = DomainMap<K, Lit>

inline fun <K : Tuple, T> Solver.newDomainVarDomainMap(
    domains: Domains<K>,
    encodeOneHot: Boolean = true,
    init: (T) -> Lit = { newLiteral() },
    domain: (K) -> Iterable<T>,
): DomainVarDomainMap<K, T> = MutableDomainMap(domains) {
    newDomainVar(domain(it), encodeOneHot, init)
}

inline fun <K : Tuple> Solver.newIntVarDomainMap(
    domains: Domains<K>,
    encodeOneHot: Boolean = true,
    init: (Int) -> Lit = { newLiteral() },
    domain: (K) -> Iterable<Int>,
): IntVarDomainMap<K> = MutableDomainMap(domains) {
    newIntVar(domain(it), encodeOneHot, init)
}
