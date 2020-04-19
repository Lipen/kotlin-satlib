package com.github.lipen.satlib.op

import com.github.lipen.satlib.solver.MiniSatSolver
import com.github.lipen.satlib.solver.Solver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

fun <T> Solver.runWithTimeout(timeMillis: Long, block: Solver.() -> T): T {
    return runBlocking {
        val job = async(Dispatchers.Default) { block() }
        try {
            withTimeout(timeMillis) {
                job.await()
            }
        } catch (e: TimeoutCancellationException) {
            interrupt()
            job.await()
        } finally {
            if (this@runWithTimeout is MiniSatSolver) {
                backend.clearInterrupt()
            }
        }
    }
}
