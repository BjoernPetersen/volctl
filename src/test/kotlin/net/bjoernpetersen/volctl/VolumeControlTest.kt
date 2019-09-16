package net.bjoernpetersen.volctl

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

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
}
