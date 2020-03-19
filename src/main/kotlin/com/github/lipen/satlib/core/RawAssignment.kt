package com.github.lipen.satlib.core

import com.github.lipen.multiarray.BooleanMultiArray
import com.github.lipen.multiarray.IntMultiArray
import com.github.lipen.multiarray.MultiArray
import com.github.lipen.multiarray.map
import com.github.lipen.multiarray.mapToBoolean
import com.github.lipen.multiarray.mapToInt

interface RawAssignment {
    operator fun get(v: Lit): Boolean // 1-based, as Lit
}

class RawAssignment0(private val data: BooleanArray) : RawAssignment {
    override operator fun get(v: Lit): Boolean = when (v) {
        // Solver.trueLiteral -> true
        // Solver.falseLiteral -> false
        else -> data[v - 1]
    }

    override fun toString(): String {
        return data.asList().toString()
    }
}

class RawAssignment1(private val data: BooleanArray) : RawAssignment {
    override operator fun get(v: Lit): Boolean = when (v) {
        // Solver.trueLiteral -> true
        // Solver.falseLiteral -> false
        else -> data[v]
    }

    override fun toString(): String {
        return data.drop(1).toString()
    }
}

fun <T> DomainVar<T>.convert(raw: RawAssignment): T? =
    storage.entries.firstOrNull { raw[it.value] }?.key

inline fun <reified T> DomainVarArray<T>.convert(raw: RawAssignment): MultiArray<T> =
    map { it.convert(raw) ?: error("So sad :c") }

fun IntVarArray.convert(raw: RawAssignment): IntMultiArray =
    mapToInt { it.convert(raw) ?: error("So sad :c") }

fun BoolVarArray.convert(raw: RawAssignment): BooleanMultiArray =
    mapToBoolean { raw[it] }

@JvmName("multiArrayDomainVarArrayConvert")
inline fun <reified T> MultiArray<DomainVarArray<T>>.convert(raw: RawAssignment): MultiArray<MultiArray<T>> =
    map { it.convert(raw) }

@JvmName("multiArrayIntVarArrayConvert")
fun MultiArray<IntVarArray>.convert(raw: RawAssignment): MultiArray<IntMultiArray> =
    map { it.convert(raw) }

@JvmName("multiArrayBoolVarArrayConvert")
fun MultiArray<BoolVarArray>.convert(raw: RawAssignment): MultiArray<BooleanMultiArray> =
    map { it.convert(raw) }
