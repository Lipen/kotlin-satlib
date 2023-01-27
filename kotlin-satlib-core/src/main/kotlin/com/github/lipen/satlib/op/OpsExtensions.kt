@file:Suppress("FunctionName")

package com.github.lipen.satlib.op

import com.github.lipen.satlib.core.Lit
import com.github.lipen.satlib.core.LitArray
import com.github.lipen.satlib.core.SequenceScopeLit
import com.github.lipen.satlib.solver.Solver

/** `AtLeastOne`([literals]) */
fun Solver.atLeastOne_(literals: LitArray) {
    atLeastOne(literals.asList())
}

/** `AtLeastOne`([literals]) */
fun Solver.atLeastOne(vararg literals: Lit) {
    atLeastOne_(literals)
}

/** `AtLeastOne`([literals]) */
fun Solver.atLeastOne(literals: Sequence<Lit>) {
    atLeastOne(literals.asIterable())
}

/** `AtLeastOne`(literals) */
fun Solver.atLeastOne(block: SequenceScopeLit) {
    atLeastOne(sequence(block))
}

/** `AtMostOne`([literals]) */
fun Solver.atMostOne_(literals: LitArray) {
    atMostOne(literals.asList())
}

/** `AtMostOne`([literals]) */
fun Solver.atMostOne(vararg literals: Lit) {
    atMostOne_(literals)
}

/** `AtMostOne`([literals]) */
fun Solver.atMostOne(literals: Sequence<Lit>) {
    atMostOne(literals.asIterable())
}

/** `AtMostOne`(literals) */
fun Solver.atMostOne(block: SequenceScopeLit) {
    atMostOne(sequence(block))
}

/** `ExactlyOne`([literals]) */
fun Solver.exactlyOne_(literals: LitArray) {
    exactlyOne(literals.asList())
}

/** `ExactlyOne`([literals]) */
fun Solver.exactlyOne(vararg literals: Lit) {
    exactlyOne_(literals)
}

/** `ExactlyOne`([literals]) */
fun Solver.exactlyOne(literals: Sequence<Lit>) {
    exactlyOne(literals.asIterable())
}

/** `ExactlyOne`(literals) */
fun Solver.exactlyOne(block: SequenceScopeLit) {
    exactlyOne(sequence(block))
}

/** [lhs] => `AND`([rhs]) */
fun Solver.implyAnd_(lhs: Lit, rhs: LitArray) {
    implyAnd(lhs, rhs.asList())
}

/** [lhs] => `AND`([rhs]) */
fun Solver.implyAnd(lhs: Lit, vararg rhs: Lit) {
    implyAnd_(lhs, rhs)
}

/** [lhs] => `AND`([rhs]) */
fun Solver.implyAnd(lhs: Lit, rhs: Sequence<Lit>) {
    implyAnd(lhs, rhs.asIterable())
}

/** [lhs] => `AND`(rhs) */
fun Solver.implyAnd(lhs: Lit, block: SequenceScopeLit) {
    implyAnd(lhs, sequence(block))
}

/** [lhs] => `OR`([rhs]) */
fun Solver.implyOr_(lhs: Lit, rhs: LitArray) {
    implyOr(lhs, rhs.asList())
}

/** [lhs] => `OR`([rhs]) */
fun Solver.implyOr(lhs: Lit, vararg rhs: Lit) {
    implyOr_(lhs, rhs)
}

/** [lhs] => `OR`([rhs]) */
fun Solver.implyOr(lhs: Lit, rhs: Sequence<Lit>) {
    implyOr(lhs, rhs.asIterable())
}

/** [lhs] => `OR`(rhs) */
fun Solver.implyOr(lhs: Lit, block: SequenceScopeLit) {
    implyOr(lhs, sequence(block))
}

/** [x1] => (x2 => `AND`([rhs]) */
fun Solver.implyImplyAnd_(x1: Lit, x2: Lit, rhs: LitArray) {
    implyImplyAnd(x1, x2, rhs.asList())
}

/** [x1] => (x2 => `AND`([rhs]) */
fun Solver.implyImplyAnd(x1: Lit, x2: Lit, vararg rhs: Lit) {
    implyImplyAnd_(x1, x2, rhs)
}

/** [x1] => (x2 => `AND`([rhs]) */
fun Solver.implyImplyAnd(x1: Lit, x2: Lit, rhs: Sequence<Lit>) {
    implyImplyAnd(x1, x2, rhs.asIterable())
}

/** [x1] => (x2 => `AND`(rhs) */
fun Solver.implyImplyAnd(x1: Lit, x2: Lit, block: SequenceScopeLit) {
    implyImplyAnd(x1, x2, sequence(block))
}

/** [x1] => (x2 => `OR`([rhs]) */
fun Solver.implyImplyOr_(x1: Lit, x2: Lit, rhs: LitArray) {
    implyImplyOr(x1, x2, rhs.asList())
}

/** [x1] => (x2 => `OR`([rhs]) */
fun Solver.implyImplyOr(x1: Lit, x2: Lit, vararg rhs: Lit) {
    implyImplyOr_(x1, x2, rhs)
}

/** [x1] => (x2 => `OR`([rhs]) */
fun Solver.implyImplyOr(x1: Lit, x2: Lit, rhs: Sequence<Lit>) {
    implyImplyOr(x1, x2, rhs.asIterable())
}

/** [x1] => (x2 => `OR`(rhs) */
fun Solver.implyImplyOr(x1: Lit, x2: Lit, block: SequenceScopeLit) {
    implyImplyOr(x1, x2, sequence(block))
}

/** [x1] => ([x2] => ([x3] => `AND`([xs])) */
fun Solver.implyImplyImplyAnd_(x1: Lit, x2: Lit, x3: Lit, xs: LitArray) {
    implyImplyImplyAnd(x1, x2, x3, xs.asList())
}

/** [x1] => ([x2] => ([x3] => `AND`([xs])) */
fun Solver.implyImplyImplyAnd(x1: Lit, x2: Lit, x3: Lit, vararg xs: Lit) {
    implyImplyImplyAnd_(x1, x2, x3, xs)
}

/**  [x1] => ([x2] => ([x3] => `AND`([xs])) */
fun Solver.implyImplyImplyAnd(x1: Lit, x2: Lit, x3: Lit, xs: Sequence<Lit>) {
    implyImplyImplyAnd(x1, x2, x3, xs.asIterable())
}

/**  [x1] => ([x2] => ([x3] => `AND`(xs)) */
fun Solver.implyImplyImplyAnd(x1: Lit, x2: Lit, x3: Lit, block: SequenceScopeLit) {
    implyImplyImplyAnd(x1, x2, x3, sequence(block))
}

/** [x1] => ([x2] => ([x3] => `OR`([xs])) */
fun Solver.implyImplyImplyOr_(x1: Lit, x2: Lit, x3: Lit, xs: LitArray) {
    implyImplyImplyOr(x1, x2, x3, xs.asList())
}

/** [x1] => ([x2] => ([x3] => `OR`([xs])) */
fun Solver.implyImplyImplyOr(x1: Lit, x2: Lit, x3: Lit, vararg xs: Lit) {
    implyImplyImplyOr_(x1, x2, x3, xs)
}

/** [x1] => ([x2] => ([x3] => `OR`([xs])) */
fun Solver.implyImplyImplyOr(x1: Lit, x2: Lit, x3: Lit, xs: Sequence<Lit>) {
    implyImplyImplyOr(x1, x2, x3, xs.asIterable())
}

/** [x1] => ([x2] => ([x3] => `OR`(xs)) */
fun Solver.implyImplyImplyOr(x1: Lit, x2: Lit, x3: Lit, block: SequenceScopeLit) {
    implyImplyImplyOr(x1, x2, x3, sequence(block))
}

/** [x1] => ([x2] => ([x3] <=> `AND`([xs])) */
fun Solver.implyImplyIffAnd_(x1: Lit, x2: Lit, x3: Lit, xs: LitArray) {
    implyImplyIffAnd(x1, x2, x3, xs.asList())
}

/** [x1] => ([x2] => ([x3] <=> `AND`([xs])) */
fun Solver.implyImplyIffAnd(x1: Lit, x2: Lit, x3: Lit, vararg xs: Lit) {
    implyImplyIffAnd_(x1, x2, x3, xs)
}

/**  [x1] => ([x2] => ([x3] <=> `AND`([xs])) */
fun Solver.implyImplyIffAnd(x1: Lit, x2: Lit, x3: Lit, xs: Sequence<Lit>) {
    implyImplyIffAnd(x1, x2, x3, xs.asIterable())
}

/**  [x1] => ([x2] => ([x3] <=> `AND`(xs)) */
fun Solver.implyImplyIffAnd(x1: Lit, x2: Lit, x3: Lit, block: SequenceScopeLit) {
    implyImplyIffAnd(x1, x2, x3, sequence(block))
}

/** [x1] => ([x2] => ([x3] <=> `OR`([xs])) */
fun Solver.implyImplyIffOr_(x1: Lit, x2: Lit, x3: Lit, xs: LitArray) {
    implyImplyIffOr(x1, x2, x3, xs.asList())
}

/** [x1] => ([x2] => ([x3] <=> `OR`([xs])) */
fun Solver.implyImplyIffOr(x1: Lit, x2: Lit, x3: Lit, vararg xs: Lit) {
    implyImplyIffOr_(x1, x2, x3, xs)
}

/** [x1] => ([x2] => ([x3] <=> `OR`([xs])) */
fun Solver.implyImplyIffOr(x1: Lit, x2: Lit, x3: Lit, xs: Sequence<Lit>) {
    implyImplyIffOr(x1, x2, x3, xs.asIterable())
}

/** [x1] => ([x2] => ([x3] <=> `OR`(xs)) */
fun Solver.implyImplyIffOr(x1: Lit, x2: Lit, x3: Lit, block: SequenceScopeLit) {
    implyImplyIffOr(x1, x2, x3, sequence(block))
}

/** [x1] => ([x2] <=> `AND`([xs])) */
fun Solver.implyIffAnd_(x1: Lit, x2: Lit, xs: LitArray) {
    implyIffAnd(x1, x2, xs.asList())
}

/** [x1] => ([x2] <=> `AND`([xs])) */
fun Solver.implyIffAnd(x1: Lit, x2: Lit, vararg xs: Lit) {
    implyIffAnd_(x1, x2, xs)
}

/** [x1] => ([x2] <=> `AND`([xs])) */
fun Solver.implyIffAnd(x1: Lit, x2: Lit, xs: Sequence<Lit>) {
    implyIffAnd(x1, x2, xs.asIterable())
}

/** [x1] => ([x2] <=> `AND`(xs)) */
fun Solver.implyIffAnd(x1: Lit, x2: Lit, block: SequenceScopeLit) {
    implyIffAnd(x1, x2, sequence(block))
}

/** [x1] => ([x2] <=> `OR`([xs])) */
fun Solver.implyIffOr_(x1: Lit, x2: Lit, xs: LitArray) {
    implyIffOr(x1, x2, xs.asList())
}

/** [x1] => ([x2] <=> `OR`([xs])) */
fun Solver.implyIffOr(x1: Lit, x2: Lit, vararg xs: Lit) {
    implyIffOr_(x1, x2, xs)
}

/** [x1] => ([x2] <=> `OR`([xs])) */
fun Solver.implyIffOr(x1: Lit, x2: Lit, xs: Sequence<Lit>) {
    implyIffOr(x1, x2, xs.asIterable())
}

/** [x1] => ([x2] <=> `OR`(xs)) */
fun Solver.implyIffOr(x1: Lit, x2: Lit, block: SequenceScopeLit) {
    implyIffOr(x1, x2, sequence(block))
}

/** [lhs] <=> `AND`([rhs]) */
fun Solver.iffAnd_(lhs: Lit, rhs: LitArray) {
    iffAnd(lhs, rhs.asList())
}

/** [lhs] <=> `AND`([rhs]) */
fun Solver.iffAnd(lhs: Lit, vararg rhs: Lit) {
    iffAnd_(lhs, rhs)
}

/** [lhs] <=> `AND`([rhs]) */
fun Solver.iffAnd(lhs: Lit, rhs: Sequence<Lit>) {
    iffAnd(lhs, rhs.asIterable())
}

/** [lhs] <=> `AND`(rhs) */
fun Solver.iffAnd(lhs: Lit, block: SequenceScopeLit) {
    iffAnd(lhs, sequence(block))
}

/** [lhs] <=> `OR`([rhs]) */
fun Solver.iffOr_(lhs: Lit, rhs: LitArray) {
    iffOr(lhs, rhs.asList())
}

/** [lhs] <=> `OR`([rhs]) */
fun Solver.iffOr(lhs: Lit, vararg rhs: Lit) {
    iffOr_(lhs, rhs)
}

/** [lhs] <=> `OR`([rhs]) */
fun Solver.iffOr(lhs: Lit, rhs: Sequence<Lit>) {
    iffOr(lhs, rhs.asIterable())
}

/** [lhs] <=> `OR`(rhs) */
fun Solver.iffOr(lhs: Lit, block: SequenceScopeLit) {
    iffOr(lhs, sequence(block))
}
