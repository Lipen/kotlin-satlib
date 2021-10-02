/**
 * Copyright © 2016, Miklos Maroti, University of Szeged
 * Copyright © 2020, Konstantin Chukharev, ITMO University
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

package com.github.lipen.satlib.jni.solver

import com.github.lipen.satlib.jni.Loader
import com.github.lipen.satlib.utils.useWith
import java.io.File

@Suppress("PropertyName", "FunctionName", "MemberVisibilityCanBePrivate", "unused")
class JMiniSat(
    val initialSeed: Double? = null, // default is 91648253
    val initialRandomVarFreq: Double? = null,
    val initialRandomPolarities: Boolean = false,
    val initialRandomInitialActivities: Boolean = false,
) : AutoCloseable {
    private var handle: Long = 0
    private var solvable: Boolean = false

    val numberOfVariables: Int get() = minisat_nvars(handle)
    val numberOfClauses: Int get() = minisat_nclauses(handle)
    val numberOfLearnts: Int get() = minisat_nlearnts(handle)
    val numberOfDecisions: Long get() = minisat_decisions(handle)
    val numberOfPropagations: Long get() = minisat_propagations(handle)
    val numberOfConflicts: Long get() = minisat_conflicts(handle)

    init {
        reset()
    }

    fun reset() {
        if (handle != 0L) minisat_dtor(handle)
        handle = minisat_ctor()
        if (handle == 0L) throw OutOfMemoryError("minisat_ctor returned NULL")
        if (initialSeed != null) setRandomSeed(initialSeed)
        if (initialRandomVarFreq != null) setRandomVarFreq(initialRandomVarFreq)
        if (initialRandomPolarities) setRandomPolarities(true)
        if (initialRandomInitialActivities) setRandomInitialActivities(true)
        solvable = true
    }

    override fun close() {
        if (handle != 0L) minisat_dtor(handle)
        handle = 0
    }

    fun okay(): Boolean {
        return minisat_okay(handle)
    }

    @JvmOverloads
    fun newVariable(
        polarity: Polarity = Polarity.UNDEF,
        decision: Boolean = true,
        frozen: Boolean = true,
    ): Int {
        val lit = minisat_new_var(handle, polarity.value, decision)
        if (frozen) freeze(lit)
        return lit
    }

    fun setPolarity(lit: Int, polarity: Polarity) {
        minisat_set_polarity(handle, lit, polarity.value)
    }

    fun setDecision(lit: Int, decision: Boolean) {
        minisat_set_decision(handle, lit, decision)
    }

    fun setFrozen(lit: Int, frozen: Boolean) {
        minisat_set_frozen(handle, lit, frozen)
    }

    fun freeze(lit: Int) {
        minisat_freeze(handle, lit)
    }

    fun thaw() {
        minisat_thaw(handle)
    }

    fun simplify(): Boolean {
        return minisat_simplify(handle)
    }

    @JvmOverloads
    fun eliminate(turn_off_elim: Boolean = false): Boolean {
        return minisat_eliminate(handle, turn_off_elim)
    }

    fun isEliminated(lit: Int): Boolean {
        return minisat_is_eliminated(handle, lit)
    }

    fun setRandomSeed(random_seed: Double) {
        minisat_set_random_seed(handle, random_seed)
    }

    fun setRandomVarFreq(random_var_freq: Double) {
        minisat_set_random_var_freq(handle, random_var_freq)
    }

    fun setRandomPolarities(rnd_pol: Boolean) {
        minisat_set_rnd_pol(handle, rnd_pol)
    }

    fun setRandomInitialActivities(rnd_init_act: Boolean) {
        minisat_set_rnd_init_act(handle, rnd_init_act)
    }

    fun interrupt() {
        minisat_interrupt(handle)
    }

    fun clearInterrupt() {
        minisat_clear_interrupt(handle)
    }

    fun writeDimacs(path: String) {
        minisat_to_dimacs(handle, path)
    }

    fun writeDimacs(file: File) {
        writeDimacs(file.path)
    }

    @Deprecated(
        "Clause must contain at least one literal!",
        ReplaceWith("addClause(...)")
    )
    fun addClause(): Boolean {
        solvable = minisat_add_clause(handle)
        return solvable
    }

    fun addClause(lit: Int): Boolean {
        solvable = minisat_add_clause(handle, lit)
        return solvable
    }

    fun addClause(lit1: Int, lit2: Int): Boolean {
        solvable = minisat_add_clause(handle, lit1, lit2)
        return solvable
    }

    fun addClause(lit1: Int, lit2: Int, lit3: Int): Boolean {
        solvable = minisat_add_clause(handle, lit1, lit2, lit3)
        return solvable
    }

    fun addClause(vararg literals: Int): Boolean {
        return addClause_(literals)
    }

    fun addClause_(literals: IntArray): Boolean {
        solvable = minisat_add_clause(handle, literals)
        return solvable
    }

    @JvmOverloads
    fun solve(do_simp: Boolean = true, turn_off_simp: Boolean = false): Boolean {
        solvable = minisat_solve(handle, do_simp, turn_off_simp)
        return solvable
    }

    @JvmOverloads
    fun solve(assumptions: IntArray, do_simp: Boolean = true, turn_off_simp: Boolean = false): Boolean {
        solvable = minisat_solve(handle, assumptions, do_simp, turn_off_simp)
        return solvable
    }

    @JvmOverloads
    @JvmName("solveVararg")
    fun solve(vararg assumptions: Int, do_simp: Boolean = true, turn_off_simp: Boolean = false): Boolean {
        return solve(assumptions, do_simp, turn_off_simp)
    }

    fun getValue(lit: Int): Boolean {
        assert(solvable)
        return when (val value = minisat_get_value(handle, lit)) {
            LBOOL_TRUE -> true
            LBOOL_FALSE -> false
            LBOOL_UNDEF -> error("minisat_get_value returned l_Undef")
            else -> error("minisat_get_value returned $value")
        }
    }

    /** Note: resulting array is 1-based. */
    fun getModel(): BooleanArray {
        assert(solvable)
        return minisat_get_model(handle)
            ?: throw OutOfMemoryError("minisat_get_model returned NULL")
    }

    private external fun minisat_ctor(): Long
    private external fun minisat_dtor(handle: Long)
    private external fun minisat_okay(handle: Long): Boolean
    private external fun minisat_nvars(handle: Long): Int
    private external fun minisat_nclauses(handle: Long): Int
    private external fun minisat_nlearnts(handle: Long): Int
    private external fun minisat_decisions(handle: Long): Long
    private external fun minisat_propagations(handle: Long): Long
    private external fun minisat_conflicts(handle: Long): Long
    private external fun minisat_new_var(handle: Long, polarity: Byte, decision: Boolean): Int
    private external fun minisat_set_polarity(handle: Long, lit: Int, polarity: Byte)
    private external fun minisat_set_decision(handle: Long, lit: Int, decision: Boolean)
    private external fun minisat_set_frozen(handle: Long, lit: Int, frozen: Boolean)
    private external fun minisat_freeze(handle: Long, lit: Int)
    private external fun minisat_thaw(handle: Long)
    private external fun minisat_simplify(handle: Long): Boolean
    private external fun minisat_eliminate(handle: Long, turn_off_elim: Boolean): Boolean
    private external fun minisat_is_eliminated(handle: Long, lit: Int): Boolean
    private external fun minisat_set_random_seed(handle: Long, seed: Double)
    private external fun minisat_set_random_var_freq(handle: Long, random_var_freq: Double)
    private external fun minisat_set_rnd_pol(handle: Long, rnd_pol: Boolean)
    private external fun minisat_set_rnd_init_act(handle: Long, rnd_init_act: Boolean)
    private external fun minisat_interrupt(handle: Long)
    private external fun minisat_clear_interrupt(handle: Long)
    private external fun minisat_to_dimacs(handle: Long, path: String)
    private external fun minisat_add_clause(handle: Long): Boolean
    private external fun minisat_add_clause(handle: Long, lit: Int): Boolean
    private external fun minisat_add_clause(handle: Long, lit1: Int, lit2: Int): Boolean
    private external fun minisat_add_clause(handle: Long, lit1: Int, lit2: Int, lit3: Int): Boolean
    private external fun minisat_add_clause(handle: Long, literals: IntArray): Boolean

    private external fun minisat_solve(
        handle: Long,
        do_simp: Boolean,
        turn_off_simp: Boolean,
    ): Boolean

    private external fun minisat_solve(
        handle: Long,
        lit: Int,
        do_simp: Boolean,
        turn_off_simp: Boolean,
    ): Boolean

    private external fun minisat_solve(
        handle: Long,
        lit1: Int,
        lit2: Int,
        do_simp: Boolean,
        turn_off_simp: Boolean,
    ): Boolean

    private external fun minisat_solve(
        handle: Long,
        lit1: Int,
        lit2: Int,
        lit3: Int,
        do_simp: Boolean,
        turn_off_simp: Boolean,
    ): Boolean

    private external fun minisat_solve(
        handle: Long,
        assumptions: IntArray,
        do_simp: Boolean,
        turn_off_simp: Boolean,
    ): Boolean

    private external fun minisat_get_value(handle: Long, lit: Int): Byte
    private external fun minisat_get_model(handle: Long): BooleanArray?

    companion object {
        init {
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

private fun main() {
    @Suppress("DuplicatedCode")
    JMiniSat().useWith {
        val x = newVariable()
        val y = newVariable()
        val z = newVariable()

        println("Encoding exactlyOne({x, y, z})")
        addClause(-x, -y)
        addClause(-x, -z)
        addClause(-y, -z)
        addClause(x, y, z)

        println("nvars = $numberOfVariables, nclauses = $numberOfClauses, nlearnts = $numberOfLearnts")
        println("Solving...")
        check(solve()) { "Unexpected UNSAT" }
        println("x = ${getValue(x)}, y = ${getValue(y)}, z = ${getValue(z)}")
        println("model = ${getModel().drop(1)}")

        println("Solving with assumptions...")
        check(this.solve(x)); println("model = ${getModel().drop(1)}"); check(getValue(x))
        check(this.solve(y)); println("model = ${getModel().drop(1)}"); check(getValue(y))
        check(this.solve(z)); println("model = ${getModel().drop(1)}"); check(getValue(z))
        println("Solving with assumptions: OK")
    }
}
