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
import java.net.URLClassLoader
import java.nio.file.Path
import java.nio.file.Paths

class VolumeControlTest {
    private val control = VolumeControl()

    private fun Int.assertValid() {
        assertFalse(this < VolumeControl.MIN_VOLUME)
        assertFalse(this > VolumeControl.MAX_VOLUME)
    }

    @Test
    fun `get valid`() {
        control.volume.assertValid()
    }

    @TestFactory
    fun `set works`(): List<DynamicTest> = listOf(
        0, 10, 58, 90, 100
    ).map {
        dynamicTest("for value $it") {
            control.volume = it
            assertEquals(it, control.volume)
        }
    }

    @Test
    fun `multiple instances`() {
        repeat(10) {
            assertDoesNotThrow { VolumeControl() }
        }
    }

    @Test
    fun `multiple instances from multiple class loaders`() {
        // NOTE: IntelliJ will not update the .class files in that dir, use Gradle for that
        val url = Paths.get("build/classes/kotlin/main").toUri().toURL()
        val pickyClassLoader = PickyClassLoader()
        val dllLocation = VolumeControl.getTempDir()
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
        private var beforeVolume: Int = 0

        @BeforeAll
        @JvmStatic
        fun saveVolume() {
            try {
                beforeVolume = VolumeControl().volume
            } catch (e: Throwable) {
                // ignore
            }
        }

        @AfterAll
        @JvmStatic
        fun restoreVolume() {
            try {
                VolumeControl().volume = beforeVolume
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
