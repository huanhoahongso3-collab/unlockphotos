buildscript {
    dependencies {
        classpath("com.android.tools.build:gradle:8.2.2")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.22")
    }
}

// The entire allprojects {} block has been removed because 
// repositories are now handled by settings.gradle.kts

tasks.register("clean", Delete::class) {
    // Fixed the deprecation warning by using layout.buildDirectory
    delete(layout.buildDirectory)
}