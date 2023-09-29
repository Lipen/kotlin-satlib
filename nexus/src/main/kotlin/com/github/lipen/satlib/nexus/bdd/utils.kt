package com.github.lipen.satlib.nexus.bdd

/**
 * [Cantor pairing function](https://en.wikipedia.org/wiki/Pairing_function#Cantor_pairing_function).
 */
internal fun cantor2(a: Int, b: Int): Int {
    require(a >= 0)
    require(b >= 0)
    return (a + b) * (a + b + 1) / 2 + b
}

internal fun cantor3(a: Int, b: Int, c: Int): Int {
    return cantor2(cantor2(a, b), c)
}

/**
 * [Pairing function](https://en.wikipedia.org/wiki/Pairing_function) for [ULong]s.
 */
internal fun pairing2(a: ULong, b: ULong): ULong {
    // Cantor:
    // require(a >= 0)
    // require(b >= 0)
    // return (a + b) * (a + b + 1u) / 2u + b

    // Hopcroft & Ullman:
    require(a > 0u)
    require(b > 0u)
    return (a + b - 2u) * (a + b - 1u) / 2u + a
}

internal fun pairing(xs: List<ULong>): ULong {
    return when (xs.size) {
        0 -> error("Empty")
        1 -> xs[0]
        2 -> pairing2(xs[0], xs[1])
        else -> pairing2(pairing(xs.subList(0, xs.lastIndex)), xs.last())
    }
}

internal fun pairing(vararg xs: ULong): ULong {
    @OptIn(ExperimentalUnsignedTypes::class)
    return pairing(xs.asList())
}
