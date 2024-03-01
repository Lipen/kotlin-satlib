package com.github.lipen.satlib.op

import com.github.lipen.satlib.solver.Solver
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

private val logger = KotlinLogging.logger {}

fun <T> Solver.runWithTimeout(
    timeMillis: Long,
    block: Solver.() -> T,
): T = runBlocking(Dispatchers.Default) {
    _runWithTimeout(timeMillis, block)
}

suspend fun <T> Solver._runWithTimeout(
    timeMillis: Long,
    block: suspend Solver.() -> T,
): T = coroutineScope {
    logger.debug { "runWithTimeout(timeMillis = $timeMillis)" }
    val job = async { block() }
    try {
        withTimeout(timeMillis) {
            logger.debug { "Awaiting with timeout..." }
            job.await().also {
                logger.debug { "Result after awaiting is '$it'" }
            }
        }
    } catch (e: TimeoutCancellationException) {
        logger.debug { "Timeout! Interrupting..." }
        interrupt()
        logger.debug { "Awaiting after interrupt..." }
        job.await().also {
            logger.debug { "Result after awaiting is '$it'" }
        }
    } finally {
        // Actually, this must be done outside, but we are trying to do it here just to be safe.
        // Note: add your solver here if it supports resetting the interrupt state.
        //
        // TODO: the library was split into `core` and `solvers` independent modules,
        //       making this code not possible here (since `core` does not depend on `solvers`,
        //       where MiniSatSolver is declared).
        //       Users have to be careful to call `clearInterrupt` manually,
        //       until we implement the replacement solution.
        // when (this@runWithTimeout) {
        //     is MiniSatSolver -> backend.clearInterrupt()
        //     is GlucoseSolver -> backend.clearInterrupt()
        // }
    }
}
