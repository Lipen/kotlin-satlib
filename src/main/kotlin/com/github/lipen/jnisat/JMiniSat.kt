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
class JMiniSat @JvmOverloads constructor(
    private val simplify: SimplificationMethod = SimplificationMethod.ONCE
) : AutoCloseable {
    private var handle: Long = 0
    private var solvable: Boolean = false
    private var simplified: Boolean = false

    init {
        reset()
    }

    fun reset() {
        if (handle != 0L) minisat_dtor(handle)
        handle = minisat_ctor()
        if (handle == 0L) throw OutOfMemoryError()
        solvable = true
        simplified = false
        if (simplify == SimplificationMethod.NEVER) minisat_eliminate(handle, true)
    }

    fun addVariable(): Int {
        val lit = minisat_new_var(handle, LBOOL_UNDEF)
        minisat_set_frozen(handle, lit, true)
        return lit
    }

    fun addVariable(
        polarity: Byte = LBOOL_UNDEF,
        eliminate: Boolean = false,
        decision: Boolean = true
    ): Int {
        val lit = minisat_new_var(handle, polarity)
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

    fun addClause(vararg literals: Int) {
        solvable = minisat_add_clause(handle, literals)
    }

    fun solve(): Boolean {
        when (simplify) {
            SimplificationMethod.ONCE -> {
                solvable = minisat_solve(handle, simplify = !simplified, turnoff = !simplified)
                simplified = true
            }
            SimplificationMethod.ALWAYS -> {
                solvable = minisat_solve(handle, simplify = true, turnoff = false)
            }
            SimplificationMethod.NEVER -> {
                solvable = minisat_solve(handle, simplify = false, turnoff = false)
            }
        }
        return solvable
    }

    fun getValue(literal: Int): Int {
        assert(solvable)
        val a = minisat_model_value(handle, literal)
        assert(a == LBOOL_FALSE || a == LBOOL_TRUE)
        return if (a == LBOOL_TRUE) 1 else -1
    }

    fun getModel(): BooleanArray {
        assert(solvable)
        TODO("minisat_get_model()")
    }

    override fun close() {
        if (handle != 0L) minisat_dtor(handle)
        handle = 0
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
    private external fun minisat_solve(handle: Long, simplify: Boolean, turnoff: Boolean): Boolean
    private external fun minisat_simplify(handle: Long): Boolean
    private external fun minisat_eliminate(handle: Long, turnoff: Boolean): Boolean
    private external fun minisat_is_eliminated(handle: Long, lit: Int): Boolean
    private external fun minisat_okay(handle: Long): Boolean
    private external fun minisat_model_value(handle: Long, lit: Int): Byte

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

        enum class SimplificationMethod {
            ONCE, ALWAYS, NEVER;
        }

        private const val LBOOL_TRUE: Byte = 0
        private const val LBOOL_FALSE: Byte = 1
        private const val LBOOL_UNDEF: Byte = 2
    }
}

fun main() {
    fun <T : AutoCloseable, R> T.useWith(block: T.() -> R): R = use(block)

    JMiniSat().useWith {
        val x = addVariable()
        val y = addVariable()
        val z = addVariable()

        addClause(-x)
        addClause(-z)
        addClause(x, y, z)

        check(solve()) { "Unexpected UNSAT" }

        // Answer must be: x = -1, y = 1, z = -1
        println("x = ${getValue(x)}, y = ${getValue(y)}, z = ${getValue(z)}")
    }
}
