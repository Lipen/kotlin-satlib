package com.github.lipen.satlib.nexus.utils

internal fun Int.pow(n: Int): Int =
    if (this == 2) {
        1 shl n
    } else {
        // Exponentiation by squaring
        var base = this
        var exp = n
        var result = 1
        while (true) {
            if (isEven(exp)) result *= base
            exp = exp shr 1
            if (exp == 0) break
            base *= base
        }
        result
    }

internal fun Long.pow(n: Int): Long =
    if (this == 2L) {
        1L shl n
    } else {
        // Exponentiation by squaring
        var base = this
        var exp = n
        var result = 1L
        while (true) {
            if (isEven(exp)) result *= base
            exp = exp shr 1
            if (exp == 0) break
            base *= base
        }
        result
    }
