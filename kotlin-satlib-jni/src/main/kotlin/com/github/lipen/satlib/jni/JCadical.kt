package com.github.lipen.satlib.jni

import java.io.File

@Suppress("FunctionName", "MemberVisibilityCanBePrivate")
class JCadical(
    val initialSeed: Int? = null, // internal default is 0
) : AutoCloseable {
    private var handle: Long = 0

    val numberOfVariables: Int get() = cadical_vars(handle)
    val numberOfConflicts: Long get() = cadical_conflicts(handle)
    val numberOfDecisions: Long get() = cadical_decisions(handle)
    val numberOfRestarts: Long get() = cadical_restarts(handle)
    val numberOfPropagations: Long get() = cadical_propagations(handle)

    init {
        reset()
    }

    fun reset() {
        if (handle != 0L) cadical_delete(handle)
        handle = cadical_create()
        if (handle == 0L) throw OutOfMemoryError("cadical_create returned NULL")
        if (initialSeed != null) setOption("seed", initialSeed)
    }

    override fun close() {
        if (handle != 0L) {
            cadical_delete(handle)
            handle = 0
        }
    }

    fun setOption(name: String, value: Int): Boolean {
        return cadical_set(handle, name, value)
    }

    fun setLongOption(arg: String): Boolean {
        return cadical_set_long_option(handle, arg)
    }

    fun frozen(lit: Int): Boolean {
        return cadical_frozen(handle, lit)
    }

    fun freeze(lit: Int) {
        cadical_freeze(handle, lit)
    }

    fun melt(lit: Int) {
        cadical_melt(handle, lit)
    }

    fun fixed(lit: Int): Int {
        return cadical_fixed(handle, lit)
    }

    fun failed(lit: Int): Boolean {
        return cadical_failed(handle, lit)
    }

    fun optimize(value: Int) {
        cadical_optimize(handle, value)
    }

    fun simplify() {
        cadical_simplify(handle)
    }

    fun terminate() {
        cadical_terminate(handle)
    }

    fun writeDimacs(path: String) {
        cadical_write_dimacs(handle, path)
    }

    fun writeDimacs(file: File) {
        writeDimacs(file.path)
    }

    fun add(lit: Int) {
        cadical_add(handle, lit)
    }

    fun assume(lit: Int) {
        cadical_assume(handle, lit)
    }

    fun addClause() {
        add(0)
    }

    fun addClause(lit1: Int) {
        add(lit1); add(0)
    }

    fun addClause(lit1: Int, lit2: Int) {
        add(lit1); add(lit2); add(0)
    }

    fun addClause(lit1: Int, lit2: Int, lit3: Int) {
        add(lit1); add(lit2); add(lit3); add(0)
    }

    fun addClause(literals: IntArray) {
        cadical_add_clause(handle, literals)
    }

    @JvmName("addClauseVararg")
    fun addClause(vararg literals: Int) {
        addClause(literals)
    }

    fun addAssumptions(literals: IntArray) {
        cadical_add_assumptions(handle, literals)
    }

    @JvmName("addAssumptionsVararg")
    fun addAssumptions(vararg literals: Int) {
        addAssumptions(literals)
    }

    // TODO: Return enum SolveResult
    fun solve(): Boolean {
        return when (val result = cadical_solve(handle)) {
            0 -> false // UNSOLVED
            10 -> true // SATISFIABLE
            20 -> false // UNSATISFIABLE
            else -> error("cadical_solve returned $result")
        }
    }

    fun solve(assumptions: IntArray): Boolean {
        addAssumptions(assumptions)
        return solve()
    }

    @JvmName("solveVararg")
    fun solve(vararg assumptions: Int): Boolean {
        return solve(assumptions)
    }

    fun getValue(lit: Int): Boolean {
        return cadical_get_value(handle, lit)
    }

    /** Note: resulting array is 1-based. */
    fun getModel(): BooleanArray {
        return cadical_get_model(handle)
            ?: throw OutOfMemoryError("cadical_get_model returned NULL")
    }

    private external fun cadical_create(): Long
    private external fun cadical_delete(handle: Long)
    private external fun cadical_set(handle: Long, name: String, value: Int): Boolean
    private external fun cadical_set_long_option(handle: Long, arg: String): Boolean
    private external fun cadical_vars(handle: Long): Int
    private external fun cadical_conflicts(handle: Long): Long
    private external fun cadical_decisions(handle: Long): Long
    private external fun cadical_restarts(handle: Long): Long
    private external fun cadical_propagations(handle: Long): Long
    private external fun cadical_frozen(handle: Long, lit: Int): Boolean
    private external fun cadical_freeze(handle: Long, lit: Int)
    private external fun cadical_melt(handle: Long, lit: Int)
    private external fun cadical_fixed(handle: Long, lit: Int): Int
    private external fun cadical_failed(handle: Long, lit: Int): Boolean
    private external fun cadical_optimize(handle: Long, value: Int)
    private external fun cadical_simplify(handle: Long)
    private external fun cadical_terminate(handle: Long)
    private external fun cadical_write_dimacs(handle: Long, path: String)
    private external fun cadical_add(handle: Long, lit: Int)
    private external fun cadical_assume(handle: Long, lit: Int)
    private external fun cadical_add_clause(handle: Long, literals: IntArray)
    private external fun cadical_add_assumptions(handle: Long, literals: IntArray)
    private external fun cadical_solve(handle: Long): Int
    private external fun cadical_get_value(handle: Long, lit: Int): Boolean
    private external fun cadical_get_model(handle: Long): BooleanArray?

    companion object {
        init {
            Loader.load("jcadical")
        }
    }
}

private fun main() {
    val solver = JCadical()
    println("solver = $solver")
}
