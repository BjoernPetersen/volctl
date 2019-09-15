package net.bjoernpetersen.volctl

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Allows master system audio volume access and control.
 *
 * This implementation uses JNI to perform the required native system calls. To do that it has to
 * store an included library file outside of the .jar-file. The location and name of that file can
 * be customized using the constructor parameters.
 *
 * @param dllLocation the directory to store the native access library in, defaults to temp dir
 * @param dllName the name of the library file without file extension, defaults to "volctl"
 */
@Suppress("unused")
class VolumeControl @JvmOverloads constructor(
    dllLocation: Path = getTempDir(),
    dllName: String = DEFAULT_DLL_NAME
) {
    init {
        val dllPath = dllLocation.resolve("$dllName.dll")
        if (!Files.exists(dllPath)) {
            this::class.java
                .getResourceAsStream("/volctl.dll")
                .use { input ->
                    Files.newOutputStream(dllPath).use { output -> input.copyTo(output) }
                }
        }

        System.load(dllPath.toString())
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
        const val MIN_VOLUME = 0
        const val MAX_VOLUME = 100

        private const val DEFAULT_DLL_NAME = "volctl"
        private const val TMP_DIR_PROPERTY_NAME = "java.io.tmpdir"

        /**
         * Retrieves the directory for temporary files on the current system.
         */
        @JvmStatic
        fun getTempDir(): Path {
            val path = System.getProperty(TMP_DIR_PROPERTY_NAME)
            return Paths.get(path)
        }
    }
}
