@file:Suppress("unused")

package com.github.lipen.satlib.op

import com.github.lipen.satlib.core.Lit
import com.github.lipen.satlib.solver.Solver

/** AtLeastOne([literals]) */
fun Solver.atLeastOne(vararg literals: Lit) {
    atLeastOne(literals.asIterable())
}

/** AtLeastOne([literals]) */
fun Solver.atLeastOne(literals: Sequence<Lit>) {
    atLeastOne(literals.asIterable())
}

/** AtLeastOne(literals) */
fun Solver.atLeastOne(block: suspend SequenceScope<Lit>.() -> Unit) {
    atLeastOne(sequence(block))
}

/** AtMostOne([literals]) */
fun Solver.atMostOne(vararg literals: Lit) {
    atMostOne(literals.asIterable())
}

/** AtMostOne([literals]) */
fun Solver.atMostOne(literals: Sequence<Lit>) {
    atMostOne(literals.asIterable())
}

/** AtMostOne(literals) */
fun Solver.atMostOne(block: suspend SequenceScope<Lit>.() -> Unit) {
    atMostOne(sequence(block))
}

/** ExactlyOne([literals]) */
fun Solver.exactlyOne(vararg literals: Lit) {
    exactlyOne(literals.asIterable())
}

/** ExactlyOne([literals]) */
fun Solver.exactlyOne(literals: Sequence<Lit>) {
    exactlyOne(literals.asIterable())
}

/** ExactlyOne(literals) */
fun Solver.exactlyOne(block: suspend SequenceScope<Lit>.() -> Unit) {
    exactlyOne(sequence(block))
}

/** [lhs] => AND([rhs]) */
fun Solver.implyAnd(lhs: Lit, vararg rhs: Lit) {
    implyAnd(lhs, rhs.asIterable())
}

/** [lhs] => AND([rhs]) */
fun Solver.implyAnd(lhs: Lit, rhs: Sequence<Lit>) {
    implyAnd(lhs, rhs.asIterable())
}

/** [lhs] => AND(rhs) */
fun Solver.implyAnd(lhs: Lit, block: suspend SequenceScope<Lit>.() -> Unit) {
    implyAnd(lhs, sequence(block))
}

/** [lhs] => OR([rhs]) */
fun Solver.implyOr(lhs: Lit, vararg rhs: Lit) {
    implyOr(lhs, rhs.asIterable())
}

/** [lhs] => OR([rhs]) */
fun Solver.implyOr(lhs: Lit, rhs: Sequence<Lit>) {
    implyOr(lhs, rhs.asIterable())
}

/** [lhs] => OR(rhs) */
fun Solver.implyOr(lhs: Lit, block: suspend SequenceScope<Lit>.() -> Unit) {
    implyOr(lhs, sequence(block))
}

/** [x1] => (x2 => AND([rhs]) */
fun Solver.implyImplyAnd(x1: Lit, x2: Lit, vararg rhs: Lit) {
    implyImplyAnd(x1, x2, rhs.asIterable())
}

/** [x1] => (x2 => AND([rhs]) */
fun Solver.implyImplyAnd(x1: Lit, x2: Lit, rhs: Sequence<Lit>) {
    implyImplyAnd(x1, x2, rhs.asIterable())
}

/** [x1] => (x2 => AND(rhs) */
fun Solver.implyImplyAnd(x1: Lit, x2: Lit, block: suspend SequenceScope<Lit>.() -> Unit) {
    implyImplyAnd(x1, x2, sequence(block))
}

/** [x1] => (x2 => OR([rhs]) */
fun Solver.implyImplyOr(x1: Lit, x2: Lit, vararg rhs: Lit) {
    implyImplyOr(x1, x2, rhs.asIterable())
}

/** [x1] => (x2 => OR([rhs]) */
fun Solver.implyImplyOr(x1: Lit, x2: Lit, rhs: Sequence<Lit>) {
    implyImplyOr(x1, x2, rhs.asIterable())
}

/** [x1] => (x2 => OR(rhs) */
fun Solver.implyImplyOr(x1: Lit, x2: Lit, block: suspend SequenceScope<Lit>.() -> Unit) {
    implyImplyOr(x1, x2, sequence(block))
}

/** [x1] => ([x2] => ([x3] => AND([xs])) */
fun Solver.implyImplyImplyAnd(x1: Lit, x2: Lit, x3: Lit, vararg xs: Lit) {
    implyImplyImplyAnd(x1, x2, x3, xs.asIterable())
}

/**  [x1] => ([x2] => ([x3] => AND([xs])) */
fun Solver.implyImplyImplyAnd(x1: Lit, x2: Lit, x3: Lit, xs: Sequence<Lit>) {
    implyImplyImplyAnd(x1, x2, x3, xs.asIterable())
}

/**  [x1] => ([x2] => ([x3] => AND(xs)) */
fun Solver.implyImplyImplyAnd(x1: Lit, x2: Lit, x3: Lit, block: suspend SequenceScope<Lit>.() -> Unit) {
    implyImplyImplyAnd(x1, x2, x3, sequence(block))
}

/** [x1] => ([x2] => ([x3] => OR([xs])) */
fun Solver.implyImplyImplyOr(x1: Lit, x2: Lit, x3: Lit, vararg xs: Lit) {
    implyImplyImplyOr(x1, x2, x3, xs.asIterable())
}

/** [x1] => ([x2] => ([x3] => OR([xs])) */
fun Solver.implyImplyImplyOr(x1: Lit, x2: Lit, x3: Lit, xs: Sequence<Lit>) {
    implyImplyImplyOr(x1, x2, x3, xs.asIterable())
}

/** [x1] => ([x2] => ([x3] => OR(xs)) */
fun Solver.implyImplyImplyOr(x1: Lit, x2: Lit, x3: Lit, block: suspend SequenceScope<Lit>.() -> Unit) {
    implyImplyImplyOr(x1, x2, x3, sequence(block))
}

/** [x1] => ([x2] => ([x3] <=> AND([xs])) */
fun Solver.implyImplyIffAnd(x1: Lit, x2: Lit, x3: Lit, vararg xs: Lit) {
    implyImplyIffAnd(x1, x2, x3, xs.asIterable())
}

/**  [x1] => ([x2] => ([x3] <=> AND([xs])) */
fun Solver.implyImplyIffAnd(x1: Lit, x2: Lit, x3: Lit, xs: Sequence<Lit>) {
    implyImplyIffAnd(x1, x2, x3, xs.asIterable())
}

/**  [x1] => ([x2] => ([x3] <=> AND(xs)) */
fun Solver.implyImplyIffAnd(x1: Lit, x2: Lit, x3: Lit, block: suspend SequenceScope<Lit>.() -> Unit) {
    implyImplyIffAnd(x1, x2, x3, sequence(block))
}

/** [x1] => ([x2] => ([x3] <=> OR([xs])) */
fun Solver.implyImplyIffOr(x1: Lit, x2: Lit, x3: Lit, vararg xs: Lit) {
    implyImplyIffOr(x1, x2, x3, xs.asIterable())
}

/** [x1] => ([x2] => ([x3] <=> OR([xs])) */
fun Solver.implyImplyIffOr(x1: Lit, x2: Lit, x3: Lit, xs: Sequence<Lit>) {
    implyImplyIffOr(x1, x2, x3, xs.asIterable())
}

/** [x1] => ([x2] => ([x3] <=> OR(xs)) */
fun Solver.implyImplyIffOr(x1: Lit, x2: Lit, x3: Lit, block: suspend SequenceScope<Lit>.() -> Unit) {
    implyImplyIffOr(x1, x2, x3, sequence(block))
}

/** [x1] => ([x2] <=> AND([xs])) */
fun Solver.implyIffAnd(x1: Lit, x2: Lit, vararg xs: Lit) {
    implyIffAnd(x1, x2, xs.asIterable())
}

/** [x1] => ([x2] <=> AND([xs])) */
fun Solver.implyIffAnd(x1: Lit, x2: Lit, xs: Sequence<Lit>) {
    implyIffAnd(x1, x2, xs.asIterable())
}

/** [x1] => ([x2] <=> AND(xs)) */
fun Solver.implyIffAnd(x1: Lit, x2: Lit, block: suspend SequenceScope<Lit>.() -> Unit) {
    implyIffAnd(x1, x2, sequence(block))
}

/** [x1] => ([x2] <=> OR([xs])) */
fun Solver.implyIffOr(x1: Lit, x2: Lit, vararg xs: Lit) {
    implyIffOr(x1, x2, xs.asIterable())
}

/** [x1] => ([x2] <=> OR([xs])) */
fun Solver.implyIffOr(x1: Lit, x2: Lit, xs: Sequence<Lit>) {
    implyIffOr(x1, x2, xs.asIterable())
}

/** [x1] => ([x2] <=> OR(xs)) */
fun Solver.implyIffOr(x1: Lit, x2: Lit, block: suspend SequenceScope<Lit>.() -> Unit) {
    implyIffOr(x1, x2, sequence(block))
}

/** [lhs] <=> AND([rhs]) */
fun Solver.iffAnd(lhs: Lit, vararg rhs: Lit) {
    iffAnd(lhs, rhs.asIterable())
}

/** [lhs] <=> AND([rhs]) */
fun Solver.iffAnd(lhs: Lit, rhs: Sequence<Lit>) {
    iffAnd(lhs, rhs.asIterable())
}

/** [lhs] <=> AND(rhs) */
fun Solver.iffAnd(lhs: Lit, block: suspend SequenceScope<Lit>.() -> Unit) {
    iffAnd(lhs, sequence(block))
}

/** [lhs] <=> OR([rhs]) */
fun Solver.iffOr(lhs: Lit, vararg rhs: Lit) {
    iffOr(lhs, rhs.asIterable())
}

/** [lhs] <=> OR([rhs]) */
fun Solver.iffOr(lhs: Lit, rhs: Sequence<Lit>) {
    iffOr(lhs, rhs.asIterable())
}

/** [lhs] <=> OR(rhs) */
fun Solver.iffOr(lhs: Lit, block: suspend SequenceScope<Lit>.() -> Unit) {
    iffOr(lhs, sequence(block))
}
