@file:Suppress("FunctionName", "LocalVariableName")

package com.github.lipen.satlib.jna

import com.sun.jna.Library
import com.sun.jna.PointerType

interface LibKissat : Library {
    fun kissat_signature(): String
    fun kissat_init(): Kissat
    fun kissat_release(ptr: Kissat)

    fun kissat_add(ptr: Kissat, lit: Int)
    fun kissat_solve(ptr: Kissat): Int
    fun kissat_terminate(ptr: Kissat)
    fun kissat_value(ptr: Kissat, lit: Int): Int

    // TODO: DllExport void kissat_set_terminate (kissat * solver, void *state, int (*terminate) (void *state));

    fun kissat_reserve(ptr: Kissat, max_var: Int)

    fun kissat_id(): String
    fun kissat_version(): String
    fun kissat_compiler(): String

    fun kissat_get_option(ptr: Kissat, name: String): Int
    fun kissat_set_option(ptr: Kissat, name: String, new_value: Int): Int

    fun kissat_has_configuration(name: String): Int
    fun kissat_set_configuration(ptr: Kissat, name: String): Int

    fun kissat_set_conflict_limit(ptr: Kissat, limit: Int)
    fun kissat_set_decision_limit(ptr: Kissat, limit: Int)

    fun kissat_print_statistics(ptr: Kissat)

    class Kissat : PointerType()

    companion object {
        val INSTANCE: LibKissat by lazy(::load)

        fun load(name: String = "kissat"): LibKissat = loadLibrary(name)
    }
}

fun LibKissat.kissat_add_clause(ptr: LibKissat.Kissat, clause: Iterable<Int>) {
    for (lit in clause) {
        kissat_add(ptr, lit)
    }
    kissat_add(ptr, 0)
}

fun main() {
    val lib = LibKissat.load("kissat")
    println("library = $lib")
    println("signature = ${lib.kissat_signature()}")
    println("id = ${lib.kissat_id()}")
    println("version = ${lib.kissat_version()}")
    println("compiler = ${lib.kissat_compiler()}")

    run {
        println("-".repeat(50))
        val ptr = lib.kissat_init()
        println("ptr = $ptr")

        // SAT
        lib.kissat_add_clause(ptr, listOf(-1))
        println("Solving...")
        val res = lib.kissat_solve(ptr)
        println("res = $res")

        lib.kissat_release(ptr)
    }

    run {
        println("-".repeat(50))
        val ptr = lib.kissat_init()
        println("ptr = $ptr")

        // SAT
        lib.kissat_add_clause(ptr, listOf(-1))
        lib.kissat_add_clause(ptr, listOf(1))
        println("Solving...")
        val res = lib.kissat_solve(ptr)
        println("res = $res")

        lib.kissat_release(ptr)
    }

    println()
    println("All done!")
}
