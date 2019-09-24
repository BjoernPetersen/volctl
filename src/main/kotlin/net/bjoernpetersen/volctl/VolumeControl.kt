package net.bjoernpetersen.volctl

import net.bjoernpetersen.volctl.VolumeControl.Companion.newInstanceWithClassLoaderSupport
import java.io.IOException
import java.nio.channels.Channels
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

/**
 * Allows master system audio volume access and control.
 *
 * This implementation uses JNI to perform the required native system calls. To do that it has to
 * store an included library file outside of the .jar-file. The location and name of that file can
 * be customized using the constructor parameters.
 *
 * ## Usage with multiple class loaders
 *
 * If you plan to use this class from multiple class loaders, you'll have to set
 * `supportMultipleClassLoaders` to `true`. If you do that, **every instance will export its own
 * library file**. These files won't have a fully predictable filename and cannot be deleted by
 * the same JVM that has loaded them.
 * This workaround is necessary because only one [ClassLoader] is allowed to load a library file.
 *
 * **Note:** Java-callers may use the [newInstanceWithClassLoaderSupport] method if they want to
 * use the default location and name, but enable multiple class loader support.
 *
 * @param dllLocation the directory to store the native access library in, defaults to temp dir
 * @param dllName the name of the library file without file extension, defaults to "volctl"
 * @param supportMultipleClassLoaders whether to create library files with different names for each
 * new instance (see above)
 */
@Suppress("unused")
class VolumeControl @JvmOverloads constructor(
    dllLocation: Path = getTempDir(),
    dllName: String = getDefaultLibName(),
    supportMultipleClassLoaders: Boolean = false
) {
    init {
        val defaultLibFile = getDefaultLibFileName()
        val extension = defaultLibFile.substringAfterLast('.')
        val path = if (supportMultipleClassLoaders) {
            // Every instance gets its own library file
            Files.createTempFile(dllLocation, dllName, ".$extension").also {
                FileChannel.open(it, StandardOpenOption.WRITE)
                    .writeLibraryFile()
            }
        } else {
            dllLocation.resolve("$dllName.$extension").also {
                try {
                    // Try to delete outdated library file
                    if (Files.isRegularFile(it)) Files.delete(it)
                    FileChannel
                        .open(it, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)
                        .writeLibraryFile()
                } catch (e: IOException) {
                    // Errors are most likely caused by another instance using the existing file,
                    // so we can just ignore it and use the file
                }
            }
        }

        System.load(path.toAbsolutePath().toString())
    }

    /**
     * The current master audio volume level. The value is always between 0 and 100 (inclusively).
     */
    var volume: Int
        get() = getVolumeNative()
        set(value) {
            require(value >= MIN_VOLUME) { "Value must be positive, was $value" }
            require(value <= MAX_VOLUME) { "Value must be less than 100, was $value" }
            setVolumeNative(value)
        }

    private external fun getVolumeNative(): Int

    private external fun setVolumeNative(value: Int)

    companion object {
        /**
         * The minimum value the volume may have.
         */
        const val MIN_VOLUME = 0
        /**
         * The maximum value the volume may have.
         */
        const val MAX_VOLUME = 100

        private const val LIB_NAME = "volctl"
        private const val TMP_DIR_PROPERTY_NAME = "java.io.tmpdir"

        /**
         * Java-friendly way to create a VolumeControl instance with defaults except for
         * `supportMultipleClassLoaders` being true.
         *
         * Equivalent to the Kotlin call:
         *
         * ```kotlin
         * VolumeControl(supportMultipleClassLoaders = true)
         * ```
         */
        @JvmStatic
        fun newInstanceWithClassLoaderSupport(): VolumeControl =
            VolumeControl(supportMultipleClassLoaders = true)

        /**
         * Retrieves the directory for temporary files on the current system.
         */
        @JvmStatic
        fun getTempDir(): Path {
            val path = System.getProperty(TMP_DIR_PROPERTY_NAME)
            return Paths.get(path)
        }

        /**
         * @return the default library file name for the current OS, including extension
         */
        @JvmStatic
        fun getDefaultLibFileName(): String = System.mapLibraryName(LIB_NAME)

        /**
         * @return the default library file name for the current OS, excluding extension
         */
        @JvmStatic
        fun getDefaultLibName(): String = getDefaultLibFileName().substringBeforeLast('.')
    }
}

private fun FileChannel.writeLibraryFile() {
    use {
        withLock {
            this::class.java
                .getResourceAsStream("/${VolumeControl.getDefaultLibFileName()}")
                .let { Channels.newChannel(it) }
                .use { input -> transferFrom(input, 0, Long.MAX_VALUE) }
        }
    }
}

private fun FileChannel.withLock(block: (FileChannel) -> Unit) {
    val lock = lock()
    try {
        block(this)
    } finally {
        lock.release()
    }
}
