package com.github.lipen.satlib.op

import com.github.lipen.satlib.core.DomainVar
import com.github.lipen.satlib.core.Lit
import com.github.lipen.satlib.core.eq
import com.github.lipen.satlib.core.neq
import com.github.lipen.satlib.solver.Solver
import com.github.lipen.satlib.solver.addClause
import com.github.lipen.satlib.utils.pairs
import com.github.lipen.satlib.utils.toList_

// Note: iterate over Iterable not more than once!
// Use `val pool = iterable.toList()` if necessary.

/** `AtLeastOne`([literals]) */
fun Solver.atLeastOne(literals: Iterable<Lit>) {
    addClause(literals)
}

/** `AtMostOne`([literals]) */
fun Solver.atMostOne(literals: Iterable<Lit>) {
    for ((a, b) in literals.pairs())
        imply(a, -b)
}

/** `ExactlyOne`([literals]) */
fun Solver.exactlyOne(literals: Iterable<Lit>) {
    val pool = literals.toList_()
    atLeastOne(pool)
    atMostOne(pool)
}

/** [lhs] => [rhs] */
fun Solver.imply(lhs: Lit, rhs: Lit) {
    addClause(-lhs, rhs)
}

/** [lhs] <=> [rhs] */
fun Solver.iff(lhs: Lit, rhs: Lit) {
    imply(lhs, rhs)
    imply(rhs, lhs)
}

/** `ITE`([cond], [a], [b]) */
fun Solver.ite(cond: Lit, a: Lit, b: Lit) {
    imply(cond, a)
    imply(-cond, b)
}

/** [lhs] => `AND`([rhs]) */
fun Solver.implyAnd(lhs: Lit, rhs: Iterable<Lit>) {
    for (x in rhs)
        imply(lhs, x)
}

/** [lhs] => `OR`([rhs]) */
fun Solver.implyOr(lhs: Lit, rhs: Iterable<Lit>) {
    addClause {
        yield(-lhs)
        for (x in rhs)
            yield(x)
    }
}

/** [x1] => ([x2] => [x3]) */
fun Solver.implyImply(x1: Lit, x2: Lit, x3: Lit) {
    addClause(-x1, -x2, x3)
}

/** [x1] => ([x2] <=> [x3]) */
fun Solver.implyIff(x1: Lit, x2: Lit, x3: Lit) {
    implyImply(x1, x2, x3)
    implyImply(x1, x3, x2)
}

/** [lhs] => `ITE`([cond], [a], [b]) */
fun Solver.implyIte(lhs: Lit, cond: Lit, a: Lit, b: Lit) {
    implyImply(lhs, cond, a)
    implyImply(lhs, -cond, b)
}

/** [x1] => ([x2] => `AND`([xs]) */
fun Solver.implyImplyAnd(x1: Lit, x2: Lit, xs: Iterable<Lit>) {
    for (x in xs)
        implyImply(x1, x2, x)
}

/** [x1] => ([x2] => `OR`([xs]) */
fun Solver.implyImplyOr(x1: Lit, x2: Lit, xs: Iterable<Lit>) {
    addClause {
        yield(-x1)
        yield(-x2)
        for (x in xs)
            yield(x)
    }
}

/** [x1] => ([x2] => ([x3] => [x4])) */
fun Solver.implyImplyImply(x1: Lit, x2: Lit, x3: Lit, x4: Lit) {
    addClause(-x1, -x2, -x3, x4)
}

/** [x1] => ([x2] => ([x3] <=> [x4])) */
fun Solver.implyImplyIff(x1: Lit, x2: Lit, x3: Lit, x4: Lit) {
    implyImplyImply(x1, x2, x3, x4)
    implyImplyImply(x1, x2, x4, x3)
}

/** [x1] => ([x2] => `ITE`([cond], [a], [b])) */
fun Solver.implyImplyIte(x1: Lit, x2: Lit, cond: Lit, a: Lit, b: Lit) {
    implyImplyImply(x1, x2, cond, a)
    implyImplyImply(x1, x2, -cond, b)
}

/** [x1] => ([x2] => ([x3] => `AND`([xs]))) */
fun Solver.implyImplyImplyAnd(x1: Lit, x2: Lit, x3: Lit, xs: Iterable<Lit>) {
    for (x in xs)
        implyImplyImply(x1, x2, x3, x)
}

/** [x1] => ([x2] => ([x3] => `OR`([xs]))) */
fun Solver.implyImplyImplyOr(x1: Lit, x2: Lit, x3: Lit, xs: Iterable<Lit>) {
    addClause {
        yield(-x1)
        yield(-x2)
        yield(-x3)
        for (x in xs)
            yield(x)
    }
}

/** [x1] => ([x2] => ([x3] => ([x4] => [x5]))) */
fun Solver.implyImplyImplyImply(x1: Lit, x2: Lit, x3: Lit, x4: Lit, x5: Lit) {
    addClause(-x1, -x2, -x3, -x4, x5)
}

// TODO: implyImplyImplyIff
// TODO: implyImplyImplyIte

/** [x1] => ([x2] <=> `AND`([xs])) */
fun Solver.implyIffAnd(x1: Lit, x2: Lit, xs: Iterable<Lit>) {
    addClause {
        yield(-x1)
        yield(x2)
        for (x in xs) {
            implyImply(x1, x2, x)
            yield(-x)
        }
    }
}

/** [x1] => ([x2] <=> `OR`([xs])) */
fun Solver.implyIffOr(x1: Lit, x2: Lit, xs: Iterable<Lit>) {
    addClause {
        yield(-x1)
        yield(-x2)
        for (x in xs) {
            implyImply(x1, x, x2)
            yield(x)
        }
    }
}

// TODO: implyIffImply
// TODO: implyIffIff

/** [x1] => ([x2] <=> ITE([cond], [a], [b]) */
fun Solver.implyIffIte(x1: Lit, x2: Lit, cond: Lit, a: Lit, b: Lit) {
    implyImplyImply(x1, x2, cond, a)
    implyImplyImply(x1, x2, -cond, b)
    implyImplyImply(x1, cond, a, x2)
    implyImplyImply(x1, -cond, b, x2)
}

/** [x1] => ([x2] => ([x3] <=> `AND`([xs])) */
fun Solver.implyImplyIffAnd(x1: Lit, x2: Lit, x3: Lit, xs: Iterable<Lit>) {
    addClause {
        yield(-x1)
        yield(-x2)
        yield(x3)
        for (x in xs) {
            implyImplyImply(x1, x2, x3, x)
            yield(-x)
        }
    }
}

/** [x1] => ([x2] => ([x3] <=> `OR`([xs]))) */
fun Solver.implyImplyIffOr(x1: Lit, x2: Lit, x3: Lit, xs: Iterable<Lit>) {
    addClause {
        yield(-x1)
        yield(-x2)
        yield(-x3)
        for (x in xs) {
            implyImplyImply(x1, x2, x, x3)
            yield(x)
        }
    }
}

// TODO: implyImplyIffImply
// TODO: implyImplyIffIff
// TODO: implyImplyIffIte

/** [lhs] <=> `AND`([rhs]) */
fun Solver.iffAnd(lhs: Lit, rhs: Iterable<Lit>) {
    addClause {
        yield(lhs)
        for (x in rhs) {
            imply(lhs, x)
            yield(-x)
        }
    }
}

/** [lhs] <=> `OR`([rhs]) */
fun Solver.iffOr(lhs: Lit, rhs: Iterable<Lit>) {
    addClause {
        yield(-lhs)
        for (x in rhs) {
            imply(x, lhs)
            yield(x)
        }
    }
}

/** [lhs] <=> ([x1] => [x2]) */
fun Solver.iffImply(lhs: Lit, x1: Lit, x2: Lit) {
    implyImply(lhs, x1, x2)
    addClause(lhs, x1)
    addClause(lhs, -x2)
}

// TODO: iffIff

/** [lhs] <=> `ITE`([cond], [a], [b]) */
fun Solver.iffIte(lhs: Lit, cond: Lit, a: Lit, b: Lit) {
    implyIte(lhs, cond, a, b)
    implyImply(cond, a, lhs)
    implyImply(-cond, b, lhs)
}

fun <T> Solver.neqv(a: DomainVar<T>, b: DomainVar<T>) {
    require(a.domain == b.domain) {
        "Variables have different domains (a = $a, b = $b)"
    }
    for (x in a.domain)
        imply(a eq x, b neq x)
}
