package com.github.lipen.satlib.nexus.cli

import com.github.ajalt.clikt.core.ParameterHolder
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.switch
import com.github.lipen.satlib.solver.CadicalSolver
import com.github.lipen.satlib.solver.CryptoMiniSatSolver
import com.github.lipen.satlib.solver.GlucoseSolver
import com.github.lipen.satlib.solver.MiniSatSolver
import com.github.lipen.satlib.solver.Solver

internal enum class SolverType {
    MINISAT, GLUCOSE, CMS, CADICAL;

    fun solverProvider(/* TODO: args for solvers */): () -> Solver =
        when (this) {
            MINISAT -> {
                { MiniSatSolver() }
            }
            GLUCOSE -> {
                { GlucoseSolver() }
            }
            CMS -> {
                { CryptoMiniSatSolver() }
            }
            CADICAL -> {
                { CadicalSolver() }
            }
        }
}

internal fun ParameterHolder.solverTypeOption() =
    option(
        help = "SAT-solver"
    ).switch(
        "--minisat" to SolverType.MINISAT,
        "--glucose" to SolverType.GLUCOSE,
        "--cms" to SolverType.CMS,
        "--cadical" to SolverType.CADICAL
    ).default(
        SolverType.MINISAT
    )
