package com.github.lipen.satlib.utils

import com.github.lipen.multiarray.BooleanMultiArray
import com.github.lipen.multiarray.IntMultiArray
import com.github.lipen.multiarray.MultiArray
import com.github.lipen.multiarray.map
import com.github.lipen.multiarray.mapToBoolean
import com.github.lipen.multiarray.mapToInt
import kotlin.math.absoluteValue

interface Model {
    operator fun get(v: Lit): Boolean // 1-based, as Lit
}

class Model0(private val data: BooleanArray) : Model {
    override operator fun get(v: Lit): Boolean = when (v) {
        // Solver.trueLiteral -> true
        // Solver.falseLiteral -> false
        else -> data[v.absoluteValue - 1] xor (v < 0)
    }

    override fun toString(): String {
        return data.asList().toString()
    }
}

class Model1(private val data: BooleanArray) : Model {
    override operator fun get(v: Lit): Boolean = when (v) {
        // Solver.trueLiteral -> true
        // Solver.falseLiteral -> false
        else -> data[v.absoluteValue] xor (v < 0)
    }

    override fun toString(): String {
        return data.drop(1).toString()
    }
}

fun <T> DomainVar<T>.convert(model: Model): T? =
    storage.entries.firstOrNull { model[it.value] }?.key

inline fun <reified T> DomainVarArray<T>.convert(model: Model): MultiArray<T> =
    map { it.convert(model) ?: error("So sad :c") }

fun IntVarArray.convert(model: Model): IntMultiArray =
    mapToInt { it.convert(model) ?: error("So sad :c") }

fun BoolVarArray.convert(model: Model): BooleanMultiArray =
    mapToBoolean { model[it] }

@JvmName("multiArrayDomainVarArrayConvert")
inline fun <reified T> MultiArray<DomainVarArray<T>>.convert(model: Model): MultiArray<MultiArray<T>> =
    map { it.convert(model) }

@JvmName("multiArrayIntVarArrayConvert")
fun MultiArray<IntVarArray>.convert(model: Model): MultiArray<IntMultiArray> =
    map { it.convert(model) }

@JvmName("multiArrayBoolVarArrayConvert")
fun MultiArray<BoolVarArray>.convert(model: Model): MultiArray<BooleanMultiArray> =
    map { it.convert(model) }
