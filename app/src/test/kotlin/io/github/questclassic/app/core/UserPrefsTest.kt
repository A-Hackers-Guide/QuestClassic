package io.github.questclassic.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UserPrefsTest {

    @Test
    fun `allKeys lists every enumerated pref`() {
        val n = UserPrefs.BoolPref.entries.size +
            UserPrefs.IntPref.entries.size +
            UserPrefs.FloatPref.entries.size +
            UserPrefs.StringPref.entries.size
        assertEquals(n, UserPrefs.allKeys().size)
    }

    @Test
    fun `all keys are unique`() {
        val keys = UserPrefs.allKeys()
        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun `bool prefs expose non-blank keys`() {
        for (p in UserPrefs.BoolPref.entries) {
            assertTrue(p.name, p.key.isNotBlank())
        }
    }

    @Test
    fun `int defaults are inside documented ranges`() {
        // These prefs are clamped on set; their defaults should already
        // be inside the clamp range so the UI shows the right value.
        val buzz = UserPrefs.IntPref.BUZZ_INTENSITY_PERCENT.default
        assertTrue(buzz in 0..100)
        val temp = UserPrefs.IntPref.AI_TEMPERATURE_INT.default
        assertTrue(temp in 0..100)
    }

    @Test
    fun `float default scale is inside clamp`() {
        val s = UserPrefs.FloatPref.UI_SCALE.default
        assertTrue(s in 0.5f..2.0f)
    }

    @Test
    fun `preferred tone default matches an AssistantToneProfile`() {
        val tone = UserPrefs.StringPref.PREFERRED_TONE.default
        // Can't import AssistantToneProfile from core, but the string
        // must be a well-formed enum name.
        assertTrue(tone.matches(Regex("""[A-Z_]{3,20}""")))
    }

    @Test
    fun `preferred locale default is blank meaning system`() {
        assertEquals("", UserPrefs.StringPref.PREFERRED_LOCALE.default)
    }
}
