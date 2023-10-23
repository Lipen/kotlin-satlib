package com.github.lipen.satlib.utils

interface Domain<out V> : Iterable<V> {
    operator fun contains(value: @UnsafeVariance V): Boolean

    override operator fun iterator(): Iterator<V>
}

@JvmInline
value class ListDomain<out V>(
    val values: List<V>,
) : Domain<V> {
    constructor(vararg values: V) : this(values.asList())

    override operator fun contains(value: @UnsafeVariance V): Boolean {
        return value in values
    }

    override operator fun iterator(): Iterator<V> {
        return values.iterator()
    }

    companion object {
        // constructor for enums
        inline operator fun <reified E : Enum<E>> invoke(): ListDomain<E> {
            return ListDomain(enumValues<E>().asList())
        }
    }
}

@JvmInline
value class IntRangeDomain(
    val range: IntRange,
) : Domain<Int> {
    override operator fun contains(value: Int): Boolean {
        return value in range
    }

    override operator fun iterator(): IntIterator {
        return range.iterator()
    }
}

data class IntValueRangeDomain<out V : Value<Int>>(
    val range: IntRange,
    val convert: (Int) -> V,
) : Domain<V> {
    override operator fun contains(value: @UnsafeVariance V): Boolean {
        return value.value in range
    }

    override operator fun iterator(): Iterator<V> {
        return range.asSequence().map(convert).iterator()
    }
}

object AnyDomain : Domain<Any> {
    override operator fun contains(value: Any): Boolean = true

    override operator fun iterator(): Iterator<Any> {
        error("AnyDomain cannot be iterated")
    }
}

class TDomain<out T> : Domain<T> {
    override operator fun contains(value: @UnsafeVariance T): Boolean = true

    override operator fun iterator(): Iterator<T> {
        error("This domain cannot be iterated")
    }
}

// ==================================================

@JvmInline
private value class Vertex(override val value: Int) : Value<Int>

@JvmInline
private value class Name(override val value: String) : Value<String>

fun main() {
    val intDomain: Domain<Int> = IntRangeDomain(1..10)
    println("intDomain = $intDomain")

    val anyDomain = AnyDomain
    println("anyDomain = $anyDomain")
    check(42 in anyDomain)

    val vertexDomain = IntValueRangeDomain(1..10, ::Vertex)
    println("vertexDomain = $vertexDomain")

    val v1 = Vertex(1)
    println("v1 = $v1")
    val v2 = Vertex(42)
    println("v2 = $v2")

    println("v1 in domain: ${v1 in vertexDomain}")
    check(v1 in vertexDomain)
    println("v2 in domain: ${v2 in vertexDomain}")
    check(v2 !in vertexDomain)

    val cat = Name("cat")
    val dog = Name("dog")

    val nameDomain: Domain<Name> = ListDomain(cat, dog)
    check(cat in nameDomain)
    check(dog in nameDomain)
    check(Name("mouse") !in nameDomain)

    run {
        val domains = Domains(nameDomain, vertexDomain)
        println("domains = $domains")
        val m: MutableDomainMap<Tuple2<Name, Vertex>, Long> = MutableDomainMap(domains)
        val ten = Vertex(10)
        val twenty = Vertex(20)
        m[Tuple2(cat, ten)] = 42
        m[dog, twenty] = 5
        println("m = $m")
        println("m[\"dog\", 20] = ${m[dog, twenty]}")
        check(m[dog, twenty] == 5L)
        println("m[\"cat\", 10] = ${m[cat, ten]}")
        check(m[cat, ten] == 42L)
        println("${Tuple(cat, twenty)} in m: ${Tuple(cat, twenty) in m}")
        check(Tuple(cat, twenty) !in m)
    }

    println()
    println("=".repeat(42))
    println()

    run {
        val domains = Domains(nameDomain, vertexDomain, anyDomain)
        println("domains = $domains")
        // val m: MutableDomainMap<Tuple3<Name, Vertex, Any>, Long> = MutableDomainMap(domains)
        // val m = MutableDomainMapBuilder(domains).build<Long>()
        val m = MutableDomainMap.builder(domains).build<Long>()
        val ten = Vertex(10)
        val twenty = Vertex(20)
        m[Tuple3(cat, ten, "abc")] = 42
        m[dog, twenty, "qwerty"] = 5
        println("m = $m")
        println("m[\"dog\", 20, \"qwerty\"] = ${m[dog, twenty, "qwerty"]}")
        check(m[dog, twenty, "qwerty"] == 5L)
        println("m[\"cat\", 10, \"abc\"] = ${m[cat, ten, "abc"]}")
        check(m[cat, ten, "abc"] == 42L)
        println("${Tuple(cat, twenty, "abc")} in m: ${Tuple(cat, twenty, "abc") in m}")
        check(Tuple(cat, twenty, "abc") !in m)
    }
}
