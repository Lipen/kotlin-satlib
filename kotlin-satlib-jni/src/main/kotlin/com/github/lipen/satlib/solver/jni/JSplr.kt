/**
 * © 2020 Konstantin Chukharev, ITMO University
 */

package com.github.lipen.satlib.solver.jni

import com.github.lipen.satlib.utils.useWith

@Suppress("FunctionName", "MemberVisibilityCanBePrivate")
class JSplr : AutoCloseable {
    private var handle: Long = 0
    private var solvable: Boolean = false

    var numberOfVariables: Int = 0
        private set
    var numberOfClauses: Int = 0
        private set
    var model: IntArray? = null
        private set

    @Suppress("PropertyName")
    val _numberOfVariablesInternal: Int
        get() = splr_nvars(handle)

    init {
        reset()
    }

    fun reset() {
        if (handle != 0L) splr_delete(handle)
        handle = splr_create()
        if (handle == 0L) error("splr_create returned 0-pointer")
        solvable = true
        numberOfVariables = 0
        numberOfClauses = 0
    }

    override fun close() {
        if (handle != 0L) {
            splr_delete(handle)
            handle = 0
        }
    }

    fun hello() {
        splr_hello()
    }

    fun newVariable(): Int {
        return splr_new_var(handle)
            .also { numberOfVariables = it }
            .also { println("newVariable() -> $it") }
    }

    fun addClause(lit1: Int): Boolean {
        ++numberOfClauses
        if (!splr_add_clause(handle, lit1)) solvable = false
        return solvable
    }

    fun addClause(lit1: Int, lit2: Int): Boolean {
        ++numberOfClauses
        if (!splr_add_clause(handle, lit1, lit2)) {
            solvable = false
        }
        return solvable
    }

    fun addClause(lit1: Int, lit2: Int, lit3: Int): Boolean {
        ++numberOfClauses
        if (!splr_add_clause(handle, lit1, lit2, lit3)) solvable = false
        return solvable
    }

    fun addClause(vararg literals: Int): Boolean {
        return addClause_(literals)
    }

    fun addClause_(literals: IntArray): Boolean {
        ++numberOfClauses
        if (!splr_add_clause(handle, literals)) solvable = false
        return solvable
    }

    fun solve(): Boolean {
        model = if (!solvable) {
            null
        } else {
            splr_solve(handle)
        }
        solvable = model != null
        return solvable
    }

    private external fun splr_hello()
    private external fun splr_create(): Long
    private external fun splr_delete(handle: Long)
    private external fun splr_new_var(handle: Long): Int
    private external fun splr_nvars(handle: Long): Int
    private external fun splr_add_clause(handle: Long): Boolean
    private external fun splr_add_clause(handle: Long, lit: Int): Boolean
    private external fun splr_add_clause(handle: Long, lit1: Int, lit2: Int): Boolean
    private external fun splr_add_clause(handle: Long, lit1: Int, lit2: Int, lit3: Int): Boolean
    private external fun splr_add_clause(handle: Long, literals: IntArray): Boolean
    private external fun splr_solve(handle: Long): IntArray

    companion object {
        init {
            Loader.load("jsplr")
        }
    }
}

fun main() {
    JSplr

    val timeStart = System.currentTimeMillis()

    @Suppress("DuplicatedCode")
    JSplr().useWith {
        hello()

        val x = newVariable()
        val y = newVariable()
        val z = newVariable()

        println("Encoding exactlyOne({x, y, z})")
        addClause(-x, -y)
        addClause(-x, -z)
        addClause(-y, -z)
        addClause(x, y, z)

        println("nVars = $numberOfVariables, nClauses = $numberOfClauses")
        println("Solving...")
        check(solve()) { "Unexpected UNSAT" }
        println("model = ${model!!.asList()}")
        // println("x = ${getValue(x)}, y = ${getValue(y)}, z = ${getValue(z)}")
        // println("model = ${getModel().drop(1)}")

        println("Adding unit-clause [${-x}]")
        addClause(-x)
        check(solve()) { "Unexpected UNSAT" }
        println("model = ${model!!.asList()}")

        println("Adding unit-clause [${-y}]")
        addClause(-y)
        check(solve()) { "Unexpected UNSAT" }
        println("model = ${model!!.asList()}")

        println("Adding unit-clause [${-z}]")
        addClause(-z)
        check(!solve()) { "Unexpected SAT with model = ${model!!.asList()}" }
    }

    println("\nAll done in %.2f s.".format((System.currentTimeMillis() - timeStart) / 1000f))
}
