package com.github.lipen.jnisat

/**
 * Native library loader.
 */
object Loader {
    @JvmStatic
    fun load(name: String) {
        try {
            System.loadLibrary(name)
        } catch (e: UnsatisfiedLinkError) {
            val libName = System.mapLibraryName(name)
            val resource = "/lib/$LIBDIR/$libName"
            val stream = this::class.java.getResourceAsStream(resource)
            if (stream != null) {
                val libFile = NATIVE_LIB_TEMP_DIR.resolve(libName).apply { deleteOnExit() }
                stream.use {
                    it.copyTo(libFile.outputStream())
                }
                System.load(libFile.absolutePath)
            } else {
                throw UnsatisfiedLinkError(name)
            }
        }
    }

    private val LIBDIR: String by lazy {
        val osName = System.getProperty("os.name")
        val os = when {
            osName.startsWith("Linux") -> "linux"
            osName.startsWith("Windows") -> "win"
            osName.startsWith("Mac OS X") || osName.startsWith("Darwin") -> "osx"
            else -> return@lazy "unknown"
        }
        val arch = when (System.getProperty("os.arch")) {
            "x86", "i386" -> "32"
            "x86_64", "amd64" -> "64"
            else -> return@lazy "unknown"
        }
        "$os$arch"
    }

    private val NATIVE_LIB_TEMP_DIR by lazy {
        createTempDir("nativelib").apply { deleteOnExit() }
    }
}
