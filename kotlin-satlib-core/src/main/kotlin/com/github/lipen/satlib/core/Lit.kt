package com.github.lipen.satlib.core

typealias Lit = Int
typealias LitArray = IntArray
typealias SequenceScopeLit = suspend SequenceScope<Lit>.() -> Unit

infix fun Lit.sign(b: Boolean): Lit = if (b) this else -this
