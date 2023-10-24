package com.github.lipen.satlib.core

import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * [Context] is a [Map]-like container for any non-nullable data.
 *
 * **Examples:**
 * ```
 * // Create Context
 * val context: Context = newContext()
 *
 * // Put a value into the context:
 * context["name"] = 42
 * val x = context("name", 42)
 * val x = context("name") { 42 }  // eager eval
 *
 * // Retrieve the value from the context:
 * val x: T = context["name"]
 *
 * // Bind the property (both `val` and `var` are possible):
 * var x: T by context.bind()
 *
 * // Bind the caching property with an initial value:
 * val x by context.bindCaching(42)
 * ```
 */
class Context internal constructor(
    val map: MutableMap<String, Any> = mutableMapOf(),
) {
    operator fun <T : Any> get(key: String): T {
        @Suppress("UNCHECKED_CAST")
        return map.getValue(key) as T
    }

    operator fun set(key: String, value: Any) {
        map[key] = value
    }

    fun <T : Any> getOrNull(key: String): T? {
        @Suppress("UNCHECKED_CAST")
        return map[key]?.let { it as T }
    }

    operator fun <T : Any> invoke(name: String, value: T): T {
        this[name] = value
        return value
    }

    inline operator fun <T : Any> invoke(name: String, init: () -> T): T {
        return invoke(name, init())
    }

    fun <T : Any> bind(): ReadWriteProperty<Any?, T> = BindProperty()

    fun <T : Any> bind(value: T): PropertyDelegateProvider<Any?, ReadWriteProperty<Any?, T>> =
        PropertyDelegateProvider { _, property ->
            set(property.name, value)
            BindProperty()
        }

    private inner class BindProperty<T : Any> : ReadWriteProperty<Any?, T> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): T {
            return get(property.name)
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            set(property.name, value)
        }
    }

    fun <T : Any> bindCaching(value: T): PropertyDelegateProvider<Any?, ReadWriteProperty<Any?, T>> =
        PropertyDelegateProvider { _, property ->
            set(property.name, value)
            BindCachingProperty(value)
        }

    private inner class BindCachingProperty<T : Any>(private var value: T) : ReadWriteProperty<Any?, T> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): T {
            return this.value
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            set(property.name, value)
            this.value = value
        }
    }
}

fun newContext(): Context = Context()
