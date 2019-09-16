package net.bjoernpetersen.volctl

import net.bjoernpetersen.volctl.VolumeControl.Companion.newInstanceWithClassLoaderSupport
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Locale
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

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
 * library file**. These files cannot be deleted by the same JVM that has loaded them.
 * This workaround is necessary because only one [ClassLoader] is allowed to load a library file.
 *
 * **Note:** Java-callers may use the [newInstanceWithClassLoaderSupport] method if they want to
 * use the default location and name, but enable multiple class loader support.
 *
 * @param dllLocation the directory to store the native access library in, defaults to temp dir
 * @param dllName the name of the library file without file extension, defaults to "volctl"
 * @param supportMultipleClassLoaders whether to create library files with different names for each new
 * instance (see above)
 */
@Suppress("unused")
class VolumeControl @JvmOverloads constructor(
    dllLocation: Path = getTempDir(),
    dllName: String = getDefaultLibName(),
    supportMultipleClassLoaders: Boolean = false
) {
    init {
        initLock.withLock {
            val extension = getLibraryExtension()
            val dllPath = if (supportMultipleClassLoaders) {
                Files.createTempFile(dllLocation, dllName, ".$extension")
            } else {
                dllLocation.resolve("$dllName.$extension")
            }

            try {
                Files.delete(dllPath)
            } catch (e: IOException) {
                // Errors are most likely caused by another instance using it, so we can just ignore it
            }

            if (!Files.exists(dllPath)) {
                this::class.java
                    .getResourceAsStream("/${getDefaultLibName()}.$extension")
                    .use { input ->
                        Files.newOutputStream(dllPath).use { output -> input.copyTo(output) }
                    }
            }

            System.load(dllPath.toString())
        }
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
        private val initLock: Lock = ReentrantLock()

        const val MIN_VOLUME = 0
        const val MAX_VOLUME = 100

        private const val DEFAULT_DLL_NAME_LINUX = "libvolctl"
        private const val DEFAULT_LIB_NAME_WINDOWS = "volctl"

        private const val EXTENSION_LINUX = "so"
        private const val EXTENSION_WINDOWS = "dll"

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
         * Determines whether the current OS is Windows.
         */
        @JvmStatic
        fun isWindows(): Boolean {
            val osName = System.getProperty("os.name")
            return "win" in osName.toLowerCase(Locale.US)
        }

        /**
         * @return the default library name for the current OS
         */
        @JvmStatic
        fun getDefaultLibName(): String = if (isWindows()) DEFAULT_LIB_NAME_WINDOWS
        else DEFAULT_DLL_NAME_LINUX

        @JvmStatic
        private fun getLibraryExtension(): String = if (isWindows()) EXTENSION_WINDOWS
        else EXTENSION_LINUX
    }
}
