/**
 * Copyright (c) 2016, Miklos Maroti, University of Szeged
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

package com.github.lipen.jnisat

@Suppress("FunctionName")
class JMiniSat : AutoCloseable {
    private var handle: Long = 0
    private var solvable: Boolean = false

    init {
        reset()
    }

    fun reset() {
        if (handle != 0L) minisat_dtor(handle)
        handle = minisat_ctor()
        if (handle == 0L) throw OutOfMemoryError("minisat_ctor returned NULL")
        solvable = true
    }

    override fun close() {
        if (handle != 0L) minisat_dtor(handle)
        handle = 0
    }

    fun newVariable(): Int {
        val lit = minisat_new_var(handle, Polarity.UNDEF.value)
        minisat_set_frozen(handle, lit, true)
        return lit
    }

    fun newVariable(
        polarity: Polarity = Polarity.UNDEF,
        eliminate: Boolean = false,
        decision: Boolean = true
    ): Int {
        val lit = minisat_new_var(handle, polarity.value)
        if (!eliminate) minisat_set_frozen(handle, lit, true)
        if (!decision) minisat_set_decision_var(handle, lit, false)
        return lit
    }

    fun addClause(lit: Int) {
        solvable = minisat_add_clause(handle, lit)
    }

    fun addClause(lit1: Int, lit2: Int) {
        solvable = minisat_add_clause(handle, lit1, lit2)
    }

    fun addClause(lit1: Int, lit2: Int, lit3: Int) {
        solvable = minisat_add_clause(handle, lit1, lit2, lit3)
    }

    fun addClause(literals: IntArray) {
        solvable = minisat_add_clause(handle, literals)
    }

    @JvmName("addClauseVararg")
    fun addClause(vararg literals: Int) {
        addClause(literals)
    }

    fun solve(do_simp: Boolean = true, turn_off_simp: Boolean = false): Boolean {
        solvable = minisat_solve(handle, do_simp, turn_off_simp)
        return solvable
    }

    fun solve(lit: Int, do_simp: Boolean = true, turn_off_simp: Boolean = false): Boolean {
        solvable = minisat_solve(handle, lit, do_simp, turn_off_simp)
        return solvable
    }

    fun solve(lit1: Int, lit2: Int, do_simp: Boolean = true, turn_off_simp: Boolean = false): Boolean {
        solvable = minisat_solve(handle, lit1, lit2, do_simp, turn_off_simp)
        return solvable
    }

    fun solve(lit1: Int, lit2: Int, lit3: Int, do_simp: Boolean = true, turn_off_simp: Boolean = false): Boolean {
        solvable = minisat_solve(handle, lit1, lit2, lit3, do_simp, turn_off_simp)
        return solvable
    }

    fun solve(assumptions: IntArray, do_simp: Boolean = true, turn_off_simp: Boolean = false): Boolean {
        solvable = minisat_solve(handle, assumptions, do_simp, turn_off_simp)
        return solvable
    }

    @JvmName("solveVararg")
    fun solve(vararg assumptions: Int, do_simp: Boolean = true, turn_off_simp: Boolean = false): Boolean {
        return solve(assumptions, do_simp, turn_off_simp)
    }

    fun getValue(lit: Int): Boolean {
        assert(solvable)
        return when (val value = minisat_model_value(handle, lit)) {
            LBOOL_TRUE -> true
            LBOOL_FALSE -> false
            else -> error("minisat_model_value returned $value")
        }
    }

    fun getModel(): BooleanArray {
        // Note: resulting array is 1-based, i.e. of size (nVars+1) with garbage(false) in index 0
        assert(solvable)
        return minisat_get_model(handle)
            ?: throw OutOfMemoryError("minisat_get_model returned NULL")
    }

    private external fun minisat_ctor(): Long
    private external fun minisat_dtor(handle: Long)
    private external fun minisat_new_var(handle: Long, polarity: Byte): Int
    private external fun minisat_set_decision_var(handle: Long, lit: Int, value: Boolean)
    private external fun minisat_set_frozen(handle: Long, lit: Int, value: Boolean)
    private external fun minisat_add_clause(handle: Long, lit: Int): Boolean
    private external fun minisat_add_clause(handle: Long, lit1: Int, lit2: Int): Boolean
    private external fun minisat_add_clause(handle: Long, lit1: Int, lit2: Int, lit3: Int): Boolean
    private external fun minisat_add_clause(handle: Long, lits: IntArray): Boolean

    private external fun minisat_solve(
        handle: Long, do_simp: Boolean, turn_off_simp: Boolean
    ): Boolean

    private external fun minisat_solve(
        handle: Long, lit: Int, do_simp: Boolean, turn_off_simp: Boolean
    ): Boolean

    private external fun minisat_solve(
        handle: Long, lit1: Int, lit2: Int, do_simp: Boolean, turn_off_simp: Boolean
    ): Boolean

    private external fun minisat_solve(
        handle: Long, lit1: Int, lit2: Int, lit3: Int, do_simp: Boolean, turn_off_simp: Boolean
    ): Boolean

    private external fun minisat_solve(
        handle: Long, assumptions: IntArray, do_simp: Boolean, turn_off_simp: Boolean
    ): Boolean

    private external fun minisat_simplify(handle: Long): Boolean
    private external fun minisat_eliminate(handle: Long, turnoff: Boolean): Boolean
    private external fun minisat_is_eliminated(handle: Long, lit: Int): Boolean
    private external fun minisat_okay(handle: Long): Boolean
    private external fun minisat_model_value(handle: Long, lit: Int): Byte
    private external fun minisat_get_model(handle: Long): BooleanArray?

    companion object {
        init {
            // Note: jminisat shared library depends on minisat shared library.
            // On Windows, during the jminisat loading, the dependent minisat.dll is found only if
            //  it is in the current directory (PATH, LD_LIBRARY_PATH, java.library.path, etc do not help).
            // So, first load the dependent minisat library via System.loadLibrary, which respects
            //  'java.library.path'. The alternative is to place minisat.dll inside 'resources' folder.
            // Only then load the jminisat library.
            Loader.load("minisat")
            Loader.load("jminisat")
        }

        enum class Polarity(val value: Byte) {
            TRUE(LBOOL_TRUE),
            FALSE(LBOOL_FALSE),
            UNDEF(LBOOL_UNDEF);
        }

        private const val LBOOL_TRUE: Byte = 0
        private const val LBOOL_FALSE: Byte = 1
        private const val LBOOL_UNDEF: Byte = 2
    }
}

fun main() {
    fun <T : AutoCloseable, R> T.useWith(block: T.() -> R): R = use(block)

    JMiniSat().useWith {
        val x = newVariable()
        val y = newVariable()
        val z = newVariable()

        addClause(-x)
        addClause(-z)
        addClause(x, y, z)

        check(solve()) { "Unexpected UNSAT" }

        // Answer must be: x = false, y = true, z = false
        println("x = ${getValue(x)}, y = ${getValue(y)}, z = ${getValue(z)}")
        println("model = ${getModel().drop(1)}")

        check(!solve(x))
        check(!solve(-y))
        check(!solve(z))
        check(solve(-x, y, -z))
        println("Solving with assumptions: OK")
    }
}
