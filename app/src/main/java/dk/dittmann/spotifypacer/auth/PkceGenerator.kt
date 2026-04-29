package dk.dittmann.spotifypacer.auth

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

object PkceGenerator {

    private const val VERIFIER_BYTE_LENGTH = 64

    private val urlEncoder: Base64.Encoder = Base64.getUrlEncoder().withoutPadding()

    fun generateCodeVerifier(random: SecureRandom = SecureRandom()): String {
        val bytes = ByteArray(VERIFIER_BYTE_LENGTH).also(random::nextBytes)
        return urlEncoder.encodeToString(bytes)
    }

    fun codeChallenge(codeVerifier: String): String {
        val digest =
            MessageDigest.getInstance("SHA-256").digest(codeVerifier.toByteArray(Charsets.US_ASCII))
        return urlEncoder.encodeToString(digest)
    }
}
