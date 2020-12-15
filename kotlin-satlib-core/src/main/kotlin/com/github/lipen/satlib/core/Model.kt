package com.github.lipen.satlib.core

import com.github.lipen.multiarray.BooleanMultiArray
import com.github.lipen.multiarray.IntMultiArray
import com.github.lipen.multiarray.MultiArray
import com.github.lipen.multiarray.map
import kotlin.math.absoluteValue

class Model private constructor(
    /** 0-based storage of values inside model. */
    val data: List<Boolean>,
) {
    /** Retrieve the value of 1-based literal [v]. */
    operator fun get(v: Lit): Boolean {
        // Note: `v` is 1-based, but `data` is 0-based.
        return data[v.absoluteValue - 1] xor (v < 0)
    }

    override fun toString(): String {
        return data.toString()
    }

    companion object {
        fun from(data: List<Boolean>, zerobased: Boolean): Model =
            if (zerobased) Model(data)
            else Model(data.subList(1, data.size))

        fun from(data: BooleanArray, zerobased: Boolean): Model = from(data.asList(), zerobased)
    }
}

fun <T> DomainVar<T>.convert(model: Model): T? =
    storage.entries.firstOrNull { model[it.value] }?.key

@JvmName("domainVarArrayConvert")
inline fun <reified T> DomainVarArray<T>.convert(model: Model): MultiArray<T> =
    map { it.convert(model) ?: error("So sad :c") }

@JvmName("intVarArrayConvert")
fun IntVarArray.convert(model: Model): IntMultiArray =
    map { it.convert(model) ?: error("So sad :c") }

@JvmName("boolVarArrayConvert")
fun BoolVarArray.convert(model: Model): BooleanMultiArray =
    map { model[it] }

@JvmName("multiArrayDomainVarArrayConvert")
inline fun <reified T> MultiArray<DomainVarArray<T>>.convert(model: Model): MultiArray<MultiArray<T>> =
    map { it.convert(model) }

@JvmName("multiArrayIntVarArrayConvert")
fun MultiArray<IntVarArray>.convert(model: Model): MultiArray<IntMultiArray> =
    map { it.convert(model) }

@JvmName("multiArrayBoolVarArrayConvert")
fun MultiArray<BoolVarArray>.convert(model: Model): MultiArray<BooleanMultiArray> =
    map { it.convert(model) }
