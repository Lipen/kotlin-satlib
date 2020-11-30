package com.github.lipen.satlib.utils

import okio.BufferedSource

internal fun parseDimacsOutput(source: BufferedSource): Model? {
    // TODO: if solver's output is malformed and does not contain 's ' line,
    //  then the misleading "no answer from solver" exception is thrown.
    //  We should fix the error message, or/and show the solver's output with it.
    val answer = source.lineSequence().firstOrNull { it.startsWith("s ") }
        ?: error("No answer from solver")
    return when {
        "UNSAT" in answer -> null
        "INDETERMINATE" in answer -> null
        "SAT" in answer ->
            source
                .lineSequence()
                .filter { it.startsWith("v ") }
                .flatMap { it.drop(2).trim().splitToSequence(' ') }
                .map { it.toInt() }
                .takeWhile { it != 0 }
                .map { it > 0 }
                .toList()
                .also { check(it.isNotEmpty()) { "Model is empty" } }
                .let { Model.from(it, zerobased = true) }
        else -> error("Bad answer (neither SAT nor UNSAT) from solver: '$answer'")
    }
}
