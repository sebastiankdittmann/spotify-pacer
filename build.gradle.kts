plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.spotless)
}

spotless {
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**", "**/.gradle/**")
        ktfmt("0.53").kotlinlangStyle()
    }
    kotlinGradle {
        target("**/*.gradle.kts")
        ktfmt("0.53").kotlinlangStyle()
    }
}
