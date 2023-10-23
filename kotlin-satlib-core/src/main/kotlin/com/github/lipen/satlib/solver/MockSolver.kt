@file:Suppress("PrivatePropertyName")

package com.github.lipen.satlib.solver

import com.github.lipen.satlib.core.Context
import com.github.lipen.satlib.core.Lit
import com.github.lipen.satlib.core.Model
import com.github.lipen.satlib.core.newContext
import java.io.File

class MockSolver(
    private val __comment: (String) -> Unit = {},
    private val __newLiteral: (outer: Lit) -> Lit = { it },
    private val __addClause: (List<Lit>) -> Unit = {},
    private val __solve: () -> Boolean = { TODO() },
    private val __reset: () -> Unit = {},
    private val __close: () -> Unit = {},
    private val __dumpDimacs: MockSolver.(File) -> Unit = {},
    private val __interrupt: () -> Unit = {},
    private val __getModel: () -> Model = { TODO() },
) : Solver {
    override var context: Context = newContext()
    override var numberOfVariables: Int = 0
        private set
    override var numberOfClauses: Int = 0
        private set
    override val assumptions: MutableList<Lit> = mutableListOf()

    override fun reset() {
        context = newContext()
        numberOfVariables = 0
        numberOfClauses = 0
        assumptions.clear()
        __reset()
    }

    override fun close() {
        __close()
    }

    override fun interrupt() {
        __interrupt()
    }

    override fun dumpDimacs(file: File) {
        __dumpDimacs(file)
    }

    override fun comment(comment: String) {
        __comment(comment)
    }

    override fun newLiteral(): Lit {
        val outer = ++numberOfVariables
        return __newLiteral(outer)
    }

    override fun addClause(literals: List<Lit>) {
        ++numberOfClauses
        __addClause(literals)
    }

    override fun solve(): Boolean {
        return __solve()
    }

    override fun getValue(lit: Lit): Boolean {
        return getModel()[lit]
    }

    override fun getModel(): Model {
        return __getModel()
    }
}
