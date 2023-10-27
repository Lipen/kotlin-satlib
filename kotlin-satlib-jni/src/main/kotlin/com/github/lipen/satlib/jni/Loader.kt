package com.github.lipen.satlib.jni

import kotlin.io.path.createTempDirectory

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
            val libName = System.mapLibraryName(name)
            val resource = "/$LIBDIR/$libName"
            log.debug { "Resorting to loading from a resource: $resource" }
            val stream = this::class.java.getResourceAsStream(resource)
                ?: throw UnsatisfiedLinkError("Could not load $name neither using System.loadLibrary, nor from a resource")
            stream.use { resourceStream ->
                val libFile = NATIVE_LIB_TEMP_DIR.resolve(libName).apply { deleteOnExit() }
                libFile.outputStream().use { libFileStream ->
                    resourceStream.copyTo(libFileStream)
                }
                log.debug { "Loading from ${libFile.absolutePath}..." }
                System.load(libFile.absolutePath)
            }
        }
        log.debug { "Successfully loaded $name" }
    }

    private val LIBDIR: String by lazy {
        val osName = System.getProperty("os.name")
        val os = when {
            osName.startsWith("Linux") -> "linux"
            osName.startsWith("Windows") -> "win"
            osName.startsWith("Mac") || osName.startsWith("Darwin") -> "osx"
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
        createTempDirectory("nativelib").toFile().apply { deleteOnExit() }
    }
}
