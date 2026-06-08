buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.2.2")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.22")
    }
}

// Keep allprojects{} completely gone. settings.gradle.kts handles app dependencies.

tasks.register("clean", Delete::class) {
    delete(layout.buildDirectory)
}