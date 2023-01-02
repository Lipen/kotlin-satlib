@file:Suppress("FunctionName", "LocalVariableName")

package com.github.lipen.satlib.jna

import com.sun.jna.Callback
import com.sun.jna.Library
import com.sun.jna.Pointer
import com.sun.jna.PointerType

interface LibCadical : Library {
    fun ccadical_signature(): String
    fun ccadical_init(): CCadical
    fun ccadical_release(ptr: CCadical)

    fun ccadical_add(ptr: CCadical, lit: Int)
    fun ccadical_assume(ptr: CCadical, lit: Int)
    fun ccadical_solve(ptr: CCadical): Int
    fun ccadical_val(ptr: CCadical, lit: Int): Int
    fun ccadical_failed(ptr: CCadical, lit: Int): Int

    fun ccadical_set_terminate(ptr: CCadical, state: Pointer, terminate: TerminateCb)
    fun ccadical_set_learn(ptr: CCadical, state: Pointer, max_length: Int, learn: LearnCb)

    fun ccadical_constrain(ptr: CCadical, lit: Int)
    fun ccadical_constraint_failed(ptr: CCadical): Int
    fun ccadical_set_option(ptr: CCadical, name: String, value: Int)
    fun ccadical_limit(ptr: CCadical, name: String, limit: Int)
    fun ccadical_get_option(ptr: CCadical, name: String): Int
    fun ccadical_print_statistics(ptr: CCadical)
    fun ccadical_active(ptr: CCadical): Long
    fun ccadical_irredundant(ptr: CCadical): Long
    fun ccadical_conflicts(ptr: CCadical): Long
    fun ccadical_decisions(ptr: CCadical): Long
    fun ccadical_restarts(ptr: CCadical): Long
    fun ccadical_propagations(ptr: CCadical): Long
    fun ccadical_fixed(ptr: CCadical, lit: Int): Int
    fun ccadical_terminate(ptr: CCadical)
    fun ccadical_freeze(ptr: CCadical, lit: Int)
    fun ccadical_frozen(ptr: CCadical, lit: Int): Int
    fun ccadical_melt(ptr: CCadical, lit: Int)
    fun ccadical_simplify(ptr: CCadical): Int

    fun version(): String
    fun copyright(): String
    fun signature(): String
    fun identifier(): String
    fun compiler(): String
    fun date(): String
    fun flags(): String

    class CCadical : PointerType()

    fun interface TerminateCb : Callback {
        fun invoke(state: Pointer)
    }

    fun interface LearnCb : Callback {
        fun invoke(state: Pointer, clause: Pointer)
    }

    companion object {
        val INSTANCE: LibCadical by lazy(::load)

        fun load(name: String = "cadical"): LibCadical = loadLibraryDefault(name)
    }
}

fun LibCadical.ccadical_add_clause(ptr: LibCadical.CCadical, clause: Iterable<Int>) {
    for (lit in clause) {
        ccadical_add(ptr, lit)
    }
    ccadical_add(ptr, 0)
}

fun LibCadical.ccadical_solve(ptr: LibCadical.CCadical, assumptions: Iterable<Int>): Int {
    for (lit in assumptions) {
        ccadical_assume(ptr, lit)
    }
    return ccadical_solve(ptr)
}

fun main() {
    val lib = LibCadical.load("cadical")
    println("library = $lib")
    println("signature = ${lib.ccadical_signature()}")
    println("version = ${lib.version()}")
    println("copyright = ${lib.copyright()}")
    println("signature = ${lib.signature()}")
    println("identifier = ${lib.identifier()}")
    println("compiler = ${lib.compiler()}")
    println("date = ${lib.date()}")
    println("flags = ${lib.flags()}")

    val ptr = lib.ccadical_init()
    println("ptr = $ptr")

    lib.ccadical_add_clause(ptr, listOf(-1))

    // SAT
    println("Solving...")
    val res = lib.ccadical_solve(ptr)
    println("res = $res")

    // UNSAT under assumptions
    println("res = ${lib.ccadical_solve(ptr, listOf(1))}")

    // SAT again
    println("res = ${lib.ccadical_solve(ptr)}")

    // UNSAT
    lib.ccadical_add_clause(ptr, listOf(1))
    println("res = ${lib.ccadical_solve(ptr)}")

    lib.ccadical_release(ptr)

    println("All done!")
}
