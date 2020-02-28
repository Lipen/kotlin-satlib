@file:Suppress("ClassName", "MemberVisibilityCanBePrivate")

package com.github.lipen.jnisat

import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Param
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.TearDown
import org.openjdk.jmh.annotations.Warmup
import org.openjdk.jmh.infra.Blackhole
import java.util.concurrent.TimeUnit
import kotlin.random.Random

@BenchmarkMode(Mode.SingleShotTime)
@Warmup(iterations = 20)
@Measurement(iterations = 10)
@Fork(1)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
abstract class BenchBase {
    lateinit var solver: JMiniSat

    @Param("10000", "100000", "1000000", "10000000")
    var n: Int = 0

    @Setup
    fun setupBase() {
        check(n > 0) { "n must be a positive number" }
        solver = JMiniSat()
        with(solver) {
            val xs = List(n) { solver.newVariable() }
            for (x in xs) {
                if (Random.nextBoolean())
                    addClause(x)
                else
                    addClause(-x)
            }
            check(solve())
        }
    }

    @TearDown
    fun teardownBase() {
        solver.close()
    }
}

open class Bench_withK : BenchBase() {
    // @Param("1", "100", "1000", "10000", "100000")
    @Param(
        "1",
        "3",
        "10",
        "32",
        "100",
        "316",
        "1000",
        "3162",
        "10000",
        "31623",
        "100000",
        "316228",
        "1000000",
        "3162278",
        "10000000"
    )
    var k: Int = 0
    lateinit var literals: List<Int>

    @Setup
    fun setupWithK() {
        check(k > 0) { "k must be a positive number" }
        check(k <= n) { "k = $k is too much for n = $n" }
        literals = (1..n).shuffled().take(k)
    }

    @Benchmark
    fun getValue(bh: Blackhole) {
        for (x in literals) {
            bh.consume(solver.getValue(x))
        }
    }
}

open class Bench_onlyN : BenchBase() {
    @Benchmark
    fun getModel(bh: Blackhole) {
        val model = solver.getModel()
        bh.consume(model)
    }
}
