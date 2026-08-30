plugins {
    id("com.android.application")
}

android {
    namespace = "com.imadraude.autoscroller"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.imadraude.autoscroller"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
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
