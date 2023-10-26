package com.github.lipen.satlib.jna

import com.sun.jna.DefaultTypeMapper
import com.sun.jna.FromNativeContext
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.ToNativeContext
import com.sun.jna.TypeConverter
import mu.KotlinLogging

internal val logger = KotlinLogging.logger {}

internal inline fun <reified T : Library> loadLibrary(
    name: String,
    options: Map<String, *> = emptyMap<String, Any>(),
): T {
    logger.debug { "Loading '$name'..." }
    val lib = Native.load(name, T::class.java, options)
    logger.debug { "Loaded '$name': $lib" }
    return lib
}

internal inline fun <reified T : Any, reified N : Any> typeConverter(
    crossinline fromNative: (nativeValue: N, context: FromNativeContext?) -> T,
    crossinline toNative: (value: T, context: ToNativeContext?) -> N,
): TypeConverter = object : TypeConverter {
    override fun fromNative(nativeValue: Any?, context: FromNativeContext?): Any? {
        if (nativeValue == null) return null
        check(nativeValue is N)
        return fromNative(nativeValue, context)
    }

    override fun toNative(value: Any?, context: ToNativeContext?): Any? {
        if (value == null) return null
        check(value is T)
        return toNative(value, context)
    }

    override fun nativeType(): Class<*> {
        return N::class.java
    }
}

internal inline fun <reified T> DefaultTypeMapper.addTypeConverter(converter: TypeConverter) {
    addTypeConverter(T::class.java, converter)
}
