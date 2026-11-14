package io.github.questclassic.app.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FuzzyCommandMatcherTest {

    @Test
    fun `single character typo matches`() {
        assertEquals("/help", FuzzyCommandMatcher.suggest("hlep"))
    }

    @Test
    fun `transposition matches`() {
        assertEquals("/help", FuzzyCommandMatcher.suggest("hlpe"))
    }

    @Test
    fun `exact matches too`() {
        assertEquals("/help", FuzzyCommandMatcher.suggest("help"))
    }

    @Test
    fun `leading slash tolerated`() {
        assertEquals("/clear", FuzzyCommandMatcher.suggest("/claer"))
    }

    @Test
    fun `distance 3 returns null`() {
        assertNull(FuzzyCommandMatcher.suggest("hzqrst"))
    }

    @Test
    fun `empty returns null`() {
        assertNull(FuzzyCommandMatcher.suggest(""))
        assertNull(FuzzyCommandMatcher.suggest("/"))
    }

    @Test
    fun `switch typo`() {
        assertEquals("/switch", FuzzyCommandMatcher.suggest("switsh"))
    }

    @Test
    fun `whoami exact`() {
        assertEquals("/whoami", FuzzyCommandMatcher.suggest("whoami"))
    }
}
