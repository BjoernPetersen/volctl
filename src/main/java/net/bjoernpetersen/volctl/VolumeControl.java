package net.bjoernpetersen.volctl;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import static net.bjoernpetersen.volctl.StringUtils.substringAfterLast;
import static net.bjoernpetersen.volctl.StringUtils.substringBeforeLast;

/**
 * Allows master system audio volume access and control.
 * <p>
 * This implementation uses JNI to perform the required native system calls. To do that it has to
 * store an included library file outside of the .jar-file. The location and name of that file can
 * be customized using the constructor parameters.
 *
 * <h2>Usage with multiple class loaders</h2>
 * <p>
 * If you plan to use this class from multiple class loaders, you'll have to set
 * {@code supportMultipleClassLoaders} to {@code true}. If you do that,
 * <b>every instance will export its own library file</b>. These files won't have a fully
 * predictable filename and cannot be deleted by the same JVM that has loaded them.
 * This workaround is necessary because only one {@link ClassLoader} is allowed to load a
 * library file.
 * <p>
 * <b>Note:</b> You may use the {@link #newInstanceWithClassLoaderSupport} method if you
 * want to use the default location and name, but enable multiple class loader support.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class VolumeControl {
    /**
     * The minimum value the volume may have.
     */
    public static final int MIN_VOLUME = 0;
    /**
     * The maximum value the volume may have.
     */
    public static final int MAX_VOLUME = 100;

    private static final String LIB_NAME = "volctl";
    private static final String TMP_DIR_PROPERTY_NAME = "java.io.tmpdir";

    /**
     * Constructor.
     *
     * @param dllLocation                 the directory to store the native access library in,
     *                                    defaults to temp dir
     * @param dllName                     the name of the library file without file extension,
     *                                    defaults to "volctl"
     * @param supportMultipleClassLoaders whether to create library files with different names for
     *                                    each new instance (see {@link VolumeControl class docs})
     * @throws IOException if the library file can't be exported or loaded
     */
    public VolumeControl(
        @NotNull Path dllLocation,
        @NotNull String dllName,
        boolean supportMultipleClassLoaders
    ) throws IOException {
        String defaultLibFile = getDefaultLibFileName();
        String extension = substringAfterLast(defaultLibFile, '.');
        final Path path;
        if (supportMultipleClassLoaders) {
            // Every instance gets its own library file
            path = Files.createTempFile(dllLocation, dllName, '.' + extension);
            FileChannel channel = FileChannel.open(path, StandardOpenOption.WRITE);
            writeLibraryFile(channel);
        } else {
            path = dllLocation.resolve(dllName + '.' + extension);
            boolean writeFile = true;
            if (Files.isRegularFile(path)) {
                try {
                    // Try to delete outdated library file
                    Files.delete(path);
                } catch (IOException e) {
                    // Errors are most likely caused by another instance using the existing file,
                    // so we can just ignore it and use the file
                    writeFile = false;
                }
            }

            if (writeFile) {
                FileChannel channel = FileChannel
                    .open(path, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
                writeLibraryFile(channel);
            }
        }

        System.load(path.toAbsolutePath().toString());
    }

    /**
     * Same as calling
     * {@link #VolumeControl(Path, String, boolean) VolumeControl(dllLocation, dllName, false)}.
     *
     * @param dllLocation the directory to store the native access library in,
     *                    defaults to temp dir
     * @param dllName     the name of the library file without file extension,
     *                    defaults to "volctl"
     * @throws IOException if the library file can't be exported or loaded
     */
    public VolumeControl(@NotNull Path dllLocation, @NotNull String dllName) throws IOException {
        this(dllLocation, dllName, false);
    }

    /**
     * Same as calling
     * {@link #VolumeControl(Path, String) VolumeControl(dllLocation, getDefaultLibName())}.
     *
     * @param dllLocation the directory to store the native access library in,
     *                    defaults to temp dir
     * @throws IOException if the library file can't be exported or loaded
     */
    public VolumeControl(@NotNull Path dllLocation) throws IOException {
        this(dllLocation, getDefaultLibName());
    }

    /**
     * Same as calling
     * {@link #VolumeControl(Path, String) VolumeControl(getTempDir(), dllName)}.
     *
     * @param dllName the name of the library file without file extension,
     *                defaults to "volctl"
     * @throws IOException if the library file can't be exported or loaded
     */
    public VolumeControl(@NotNull String dllName) throws IOException {
        this(getTempDir(), dllName);
    }

    /**
     * Same as calling
     * {@link #VolumeControl(Path) VolumeControl(getTempDir())}.
     *
     * @throws IOException if the library file can't be exported or loaded
     */
    public VolumeControl() throws IOException {
        this(getTempDir());
    }

    /**
     * Gets the current master audio volume level.
     *
     * @return a value between 0 and 100 (inclusively)
     */
    public int getVolume() {
        return getVolumeNative();
    }

    /**
     * Sets the current master audio volume level.
     *
     * @param value a value between 0 and 100 (inclusively)
     */
    public void setVolume(int value) {
        if (value < MIN_VOLUME)
            throw new IllegalStateException("Value must be positive, was " + value);
        if (value > MAX_VOLUME)
            throw new IllegalStateException("Value must be less than 100, was " + value);
        setVolumeNative(value);
    }

    private native int getVolumeNative();

    private native void setVolumeNative(int value);

    /**
     * Creates a VolumeControl instance with defaults except for the
     * {@code supportMultipleClassLoaders} parameter being true.
     *
     * @return a new VolumeControl instance
     * @throws IOException if the library file couldn't be exported or loaded
     */
    @NotNull
    public static VolumeControl newInstanceWithClassLoaderSupport() throws IOException {
        return new VolumeControl(getTempDir(), getDefaultLibName(), true);
    }

    /**
     * Retrieves the directory for temporary files on the current system.
     *
     * @return the path to the tmp directory
     */
    @NotNull
    public static Path getTempDir() {
        String path = System.getProperty(TMP_DIR_PROPERTY_NAME);
        return Paths.get(path);
    }

    /**
     * @return the default library file name for the current OS, including extension
     */
    @NotNull
    public static String getDefaultLibFileName() {
        return System.mapLibraryName(LIB_NAME);
    }

    /**
     * @return the default library file name for the current OS, excluding extension
     */
    @NotNull
    public static String getDefaultLibName() {
        return substringBeforeLast(getDefaultLibFileName(), '.');
    }

    private static void writeLibraryFile(@NotNull FileChannel channel) throws IOException {
        try {
            FileLock lock = channel.lock();
            try (ReadableByteChannel input = Channels.newChannel(
                VolumeControl.class
                    .getResourceAsStream('/' + getDefaultLibFileName()))) {
                channel.transferFrom(input, 0, Long.MAX_VALUE);
            } finally {
                lock.release();
            }
        } finally {
            channel.close();
        }
    }
}
