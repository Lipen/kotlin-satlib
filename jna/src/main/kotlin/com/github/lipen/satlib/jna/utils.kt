package com.github.lipen.satlib.jna

import com.sun.jna.FromNativeContext
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.ToNativeContext
import com.sun.jna.TypeConverter

inline fun <reified T : Library> loadLibrary(
    name: String,
    options: Map<String, *> = emptyMap<String, Any>(),
): T {
    return Native.load(name, T::class.java, options)
}

inline fun <reified T : Any, reified N : Any> typeConverter(
    crossinline fromNative: (nativeValue: N, context: FromNativeContext?) -> T,
    crossinline toNative: (value: T, context: ToNativeContext?) -> N,
): TypeConverter = object : TypeConverter {
    override fun fromNative(nativeValue: Any?, context: FromNativeContext?): Any {
        checkNotNull(nativeValue)
        check(nativeValue is N)
        return fromNative(nativeValue, context)
    }

    override fun toNative(value: Any?, context: ToNativeContext?): Any {
        checkNotNull(value)
        check(value is T)
        return toNative(value, context)
    }

    override fun nativeType(): Class<*> {
        return N::class.java
    }
}
