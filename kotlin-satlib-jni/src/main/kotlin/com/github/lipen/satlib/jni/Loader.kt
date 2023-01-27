package com.github.lipen.satlib.jni

private val log = mu.KotlinLogging.logger {}

/**
 * Native library loader.
 */
object Loader {
    @JvmStatic
    fun load(name: String) {
        try {
            log.debug { "Loading $name..." }
            System.loadLibrary(name)
        } catch (e: UnsatisfiedLinkError) {
            log.debug { "Resorting to loading from a resource" }
            val libName = System.mapLibraryName(name)
            val resource = "/$LIBDIR/$libName"
            val stream = this::class.java.getResourceAsStream(resource)
            if (stream != null) stream.use { resourceStream ->
                val libFile = NATIVE_LIB_TEMP_DIR.resolve(libName).apply { deleteOnExit() }
                libFile.outputStream().use { libFileStream ->
                    resourceStream.copyTo(libFileStream)
                }
                log.debug { "Loading ${libFile.absolutePath}..." }
                System.load(libFile.absolutePath)
            } else {
                log.error { "Could not load $name neither using System.loadLibrary, nor from a resource" }
                throw e
            }
        }
        log.debug { "Successfully loaded $name" }
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
        "lib/$os$arch"
    }

    private val NATIVE_LIB_TEMP_DIR by lazy {
        createTempDir("nativelib").apply { deleteOnExit() }
    }
}
