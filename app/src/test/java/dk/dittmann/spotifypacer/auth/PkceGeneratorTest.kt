package dk.dittmann.spotifypacer.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PkceGeneratorTest {

    @Test
    fun codeChallenge_matches_rfc7636_appendix_b_vector() {
        val verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"
        val expected = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM"

        assertEquals(expected, PkceGenerator.codeChallenge(verifier))
    }

    @Test
    fun generated_verifier_is_url_safe_and_within_rfc7636_length() {
        val verifier = PkceGenerator.generateCodeVerifier()

        assertTrue("length ${verifier.length}", verifier.length in 43..128)
        assertTrue(
            "chars '$verifier'",
            verifier.all { it.isLetterOrDigit() || it == '-' || it == '_' },
        )
    }

    @Test
    fun generated_verifier_uses_fresh_entropy() {
        val first = PkceGenerator.generateCodeVerifier()
        val second = PkceGenerator.generateCodeVerifier()

        assertNotEquals(first, second)
    }
}
