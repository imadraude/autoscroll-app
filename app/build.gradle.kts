plugins {
    id("com.android.application")
}

val appVersionCode = providers.environmentVariable("APP_VERSION_CODE")
    .orNull
    ?.toIntOrNull()
    ?: 1
val releaseKeystorePath = providers.environmentVariable("ANDROID_KEYSTORE_PATH").orNull
val releaseStorePassword = providers.environmentVariable("ANDROID_KEYSTORE_PASSWORD").orNull
val releaseKeyAlias = providers.environmentVariable("ANDROID_KEY_ALIAS").orNull
val releaseKeyPassword = providers.environmentVariable("ANDROID_KEY_PASSWORD").orNull
val stableSigningConfigured = listOf(
    releaseKeystorePath,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword
).all { !it.isNullOrBlank() }

android {
    namespace = "com.imadraude.autoscroller"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.imadraude.autoscroller"
        minSdk = 26
        targetSdk = 36
        versionCode = appVersionCode
        versionName = "1.0"
    }

    signingConfigs {
        if (stableSigningConfigured) {
            create("stableRelease") {
                storeFile = file(requireNotNull(releaseKeystorePath))
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        getByName("release") {
            if (stableSigningConfigured) {
                signingConfig = signingConfigs.getByName("stableRelease")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
        warningsAsErrors = true
        htmlReport = true
        xmlReport = true
        sarifReport = true
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}

tasks.withType<org.gradle.api.tasks.compile.JavaCompile>().configureEach {
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
}
