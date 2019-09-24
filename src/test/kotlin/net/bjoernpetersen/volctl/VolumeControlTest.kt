package net.bjoernpetersen.volctl

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertDoesNotThrow
import java.io.IOException
import java.net.URLClassLoader
import java.nio.file.FileVisitResult
import java.nio.file.FileVisitor
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes

class VolumeControlTest {
    private val control = VolumeControl(dllLocation = DIR)

    private fun Int.assertValid() {
        assertFalse(this < VolumeControl.MIN_VOLUME)
        assertFalse(this > VolumeControl.MAX_VOLUME)
    }

    @Test
    fun `get valid`() {
        control.volume.assertValid()
    }

    @TestFactory
    fun `set works`(): List<DynamicTest> = (0..100).map {
        dynamicTest("for value $it") {
            control.volume = it
            assertEquals(it, control.volume)
        }
    }

    @Test
    fun `multiple instances`() {
        repeat(10) {
            assertDoesNotThrow { VolumeControl(dllLocation = DIR) }
        }
    }

    @Test
    fun `multiple instances from multiple class loaders`() {
        // NOTE: IntelliJ will not update the .class files in that dir, use Gradle for that
        val url = Paths.get("build/classes/kotlin/main").toUri().toURL()
        val pickyClassLoader = PickyClassLoader()
        val dllLocation = DIR
        val dllName = VolumeControl.getDefaultLibName()
        repeat(10) {
            val loader = URLClassLoader(arrayOf(url), pickyClassLoader)
            val volumeControlClass = loader.loadClass(VolumeControl::class.java.name)
            val constructor = volumeControlClass
                .getConstructor(Path::class.java, String::class.java, Boolean::class.java)
            val instance = assertDoesNotThrow {
                constructor.newInstance(dllLocation, dllName, true)
            }
            val getVolume = volumeControlClass.getMethod("getVolume")
            assertDoesNotThrow { getVolume.invoke(instance) }
        }
    }

    companion object {
        private val control = VolumeControl(dllName = "testRunnerVolCtl")
        private var beforeVolume: Int = 0
        val DIR: Path = Paths.get("build/tmp/libFiles")

        @BeforeAll
        @JvmStatic
        fun cleanDir() {
            Files.walkFileTree(DIR, object : FileVisitor<Path> {
                override fun visitFile(file: Path, attrs: BasicFileAttributes?): FileVisitResult {
                    Files.delete(file)
                    return FileVisitResult.CONTINUE
                }

                override fun visitFileFailed(file: Path?, exc: IOException?): FileVisitResult {
                    return FileVisitResult.TERMINATE
                }

                override fun preVisitDirectory(
                    dir: Path?,
                    attrs: BasicFileAttributes?
                ): FileVisitResult = FileVisitResult.CONTINUE

                override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
                    if (exc != null) return FileVisitResult.TERMINATE
                    return try {
                        Files.delete(dir)
                        FileVisitResult.CONTINUE
                    } catch (e: IOException) {
                        FileVisitResult.TERMINATE
                    }
                }
            })
            Files.createDirectories(DIR)
        }

        @BeforeAll
        @JvmStatic
        fun saveVolume() {
            try {
                beforeVolume = control.volume
            } catch (e: Throwable) {
                // ignore
            }
        }

        @AfterAll
        @JvmStatic
        fun restoreVolume() {
            try {
                control.volume = beforeVolume
            } catch (e: Throwable) {
                // ignore
            }
        }
    }
}

/**
 * Loads everything using the SystemClassLoader, except for the VolumeControl class.
 */
private class PickyClassLoader : ClassLoader(getSystemClassLoader()) {
    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        return if (name == CLASS_NAME) throw ClassNotFoundException()
        else super.loadClass(name, resolve)
    }

    private companion object {
        const val CLASS_NAME = "net.bjoernpetersen.volctl.VolumeControl"
    }
}
