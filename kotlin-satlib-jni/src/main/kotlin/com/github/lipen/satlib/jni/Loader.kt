package com.github.lipen.satlib.jni

import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import kotlin.io.path.createTempDirectory

private val logger = KotlinLogging.logger {}

/**
 * Native library loader.
 */
object Loader {
    @JvmStatic
    fun load(name: String) {
        try {
            logger.debug { "Loading $name..." }
            System.loadLibrary(name)
        } catch (e: UnsatisfiedLinkError) {
            val libName = System.mapLibraryName(name)
            val resource = "/lib/$OS_ARCH/$libName"
            logger.debug { "Resorting to loading from a resource: $resource" }
            val stream = this::class.java.getResourceAsStream(resource)
                ?: throw UnsatisfiedLinkError("Could not load $name neither using System.loadLibrary, nor from a resource")
            stream.use { resourceStream ->
                val libFile = NATIVE_LIB_TEMP_DIR.resolve(libName).apply { deleteOnExit() }
                libFile.outputStream().use { libFileStream ->
                    resourceStream.copyTo(libFileStream)
                }
                logger.debug { "Loading from ${libFile.absolutePath}..." }
                System.load(libFile.absolutePath)
            }
        }
        logger.debug { "Successfully loaded $name" }
    }

    private val OS_ARCH: String by lazy {
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
        "$os$arch"
    }

    private const val NATIVE_LIB_TEMP_DIR_NAME = "nativelib"
    private val NATIVE_LIB_TEMP_DIR: File by lazy {
        createTempDirectory(NATIVE_LIB_TEMP_DIR_NAME).toFile().apply { deleteOnExit() }
    }
}
