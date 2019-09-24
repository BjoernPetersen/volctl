package net.bjoernpetersen.volctl;

import org.junit.jupiter.api.*;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

final class VolumeControlTest {
    private static VolumeControl runnerControl;

    static {
        try {
            runnerControl = new VolumeControl("testRunnerVolCtl");
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static int beforeVolume = 0;
    private static final Path DIR = Paths.get("build/tmp/libFiles");

    private final VolumeControl control;

    VolumeControlTest() throws IOException {
        control = new VolumeControl(DIR);
    }

    @Test
    void getValid() {
        assertValidVolume(control.getVolume());
    }

    @TestFactory
    Stream<DynamicTest> setWorks() {
        return IntStream.rangeClosed(0, 100).mapToObj(value ->
            dynamicTest("for value " + value, () -> {
                    control.setVolume(value);
                    assertEquals(value, control.getVolume());
                }
            ));
    }

    @Test
    void multipleInstances() {
        for (int i = 0; i < 10; ++i) {
            assertDoesNotThrow(() -> new VolumeControl(DIR));
        }
    }

    @Test
    void multipleInstancesFromMultipleClassLoaders()
        throws IOException, ReflectiveOperationException {
        // NOTE: IntelliJ will not update the .class files in that dir, use Gradle for that
        URL[] urls = new URL[1];
        urls[0] = Paths.get("build/classes/java/main").toUri().toURL();

        ClassLoader pickyClassLoader = new PickyClassLoader();
        String dllName = VolumeControl.getDefaultLibName();

        for (int i = 0; i < 10; ++i) {
            ClassLoader loader = new URLClassLoader(urls, pickyClassLoader);
            Class<?> volumeControlClass = loader.loadClass(VolumeControl.class.getName());
            Constructor<?> constructor = volumeControlClass
                .getConstructor(Path.class, String.class, boolean.class);
            Object instance = assertDoesNotThrow(() ->
                constructor.newInstance(DIR, dllName, true)
            );
            Method getVolume = volumeControlClass.getMethod("getVolume");
            assertDoesNotThrow(() -> getVolume.invoke(instance));
        }
    }

    @BeforeAll
    static void cleanDir() throws IOException {
        Files.walkFileTree(DIR, new FileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                try {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                } catch (IOException e) {
                    return FileVisitResult.TERMINATE;
                }
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                return FileVisitResult.TERMINATE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                if (exc != null) return FileVisitResult.TERMINATE;
                try {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                } catch (IOException e) {
                    return FileVisitResult.TERMINATE;
                }
            }
        });
        Files.createDirectories(DIR);
    }

    @BeforeAll
    static void saveVolume() {
        try {
            beforeVolume = runnerControl.getVolume();
        } catch (Throwable e) {
            // ignore
        }
    }

    @AfterAll
    static void restoreVolume() {
        try {
            runnerControl.setVolume(beforeVolume);
        } catch (Throwable e) {
            // ignore
        }
    }

    private static void assertValidVolume(int volume) {
        assertFalse(volume < VolumeControl.MIN_VOLUME);
        assertFalse(volume > VolumeControl.MAX_VOLUME);
    }

    /**
     * Loads everything using the SystemClassLoader, except for the VolumeControl class.
     */
    private static class PickyClassLoader extends ClassLoader {

        private static final String PACKAGE_NAME = "net.bjoernpetersen.volctl";

        PickyClassLoader() {
            super(getSystemClassLoader());
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            String packageName = StringUtils.substringBeforeLast(name, '.');
            if (PACKAGE_NAME.equals(packageName)) throw new ClassNotFoundException();
            else return super.loadClass(name, resolve);
        }
    }

}
