package com.github.lipen.satlib.jna

import com.sun.jna.Library
import com.sun.jna.Native

inline fun <reified T : Library> loadLibraryDefault(name: String): T {
    return Native.load(name, T::class.java)
}
