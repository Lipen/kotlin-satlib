package com.github.lipen.satlib.op

import com.github.lipen.satlib.core.Lit
import com.github.lipen.satlib.core.eq
import com.github.lipen.satlib.core.neq
import com.github.lipen.satlib.core.newDomainVar
import com.github.lipen.satlib.solver.MockSolver
import com.github.lipen.satlib.solver.Solver
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.math.absoluteValue

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class OpsTest {
    private val clauses: MutableList<List<Lit>> = mutableListOf()

    private val solver: Solver =
        MockSolver(
            __addClause = { literals: List<Lit> ->
                clauses.add(literals)
            }
        )

    @Test
    fun clause() {
        solver.addClause(1, 2)
        solver.addClause(3, 4)
        clauses shouldBeEqualTo listOf(
            listOf(1, 2),
            listOf(3, 4)
        )
    }

    @Test
    fun imply() {
        solver.imply(1, 2)
        solver.imply(3, 4)
        clauses shouldBeEqualTo listOf(
            listOf(-1, 2),
            listOf(-3, 4)
        )
    }

    @Test
    fun implyAnd() {
        solver.implyAnd(10, 1, 2, 3)
        clauses shouldBeEqualTo listOf(
            listOf(-10, 1),
            listOf(-10, 2),
            listOf(-10, 3)
        )
    }

    @Test
    fun implyOr() {
        solver.implyOr(10, 1, 2, 3)
        clauses shouldBeEqualTo listOf(listOf(-10, 1, 2, 3))
    }

    @Test
    fun implyImplyIffAnd() {
        solver.implyImplyIffAnd(1, 2, 5, 10, 20, 30)
        clauses.map { clause -> clause.sortedBy { it.absoluteValue } } shouldBeEqualTo listOf(
            listOf(-1, -2, -5, 10),
            listOf(-1, -2, -5, 20),
            listOf(-1, -2, -5, 30),
            listOf(-1, -2, 5, -10, -20, -30)
        )
    }

    @Test
    fun implyImplyIffOr() {
        solver.implyImplyIffOr(1, 2, 5, 10, 20, 30)
        clauses.map { clause -> clause.sortedBy { it.absoluteValue } } shouldBeEqualTo listOf(
            listOf(-1, -2, 5, -10),
            listOf(-1, -2, 5, -20),
            listOf(-1, -2, 5, -30),
            listOf(-1, -2, -5, 10, 20, 30)
        )
    }

    @Test
    fun implyIffAnd() {
        solver.implyIffAnd(1, 5, 10, 20, 30)
        clauses.map { clause -> clause.sortedBy { it.absoluteValue } } shouldBeEqualTo listOf(
            listOf(-1, -5, 10),
            listOf(-1, -5, 20),
            listOf(-1, -5, 30),
            listOf(-1, 5, -10, -20, -30)
        )
    }

    @Test
    fun implyIffOr() {
        solver.implyIffOr(1, 5, 10, 20, 30)
        clauses.map { clause -> clause.sortedBy { it.absoluteValue } } shouldBeEqualTo listOf(
            listOf(-1, 5, -10),
            listOf(-1, 5, -20),
            listOf(-1, 5, -30),
            listOf(-1, -5, 10, 20, 30)
        )
    }

    @Test
    fun neqv() {
        val domain = 1..5
        val a = solver.newDomainVar(domain, encodeOneHot = false)
        val b = solver.newDomainVar(domain, encodeOneHot = false)
        solver.neqv(a, b)
        clauses.map { clause -> clause.sortedBy { it.absoluteValue } } shouldBeEqualTo
            domain.map { x ->
                listOf(-(a eq x), b neq x)
            }
    }
}
