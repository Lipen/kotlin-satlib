package com.github.lipen.satlib.solver

internal fun Solver.freezeAll() {
    // Freeze all variables
    when (this) {
        is MiniSatSolver -> {
            for (v in 1..numberOfVariables) {
                backend.freeze(v)
            }
        }
        is GlucoseSolver -> {
            for (v in 1..numberOfVariables) {
                backend.freeze(v)
            }
        }
        else -> error("$this does not support variable freezing")
    }
}
