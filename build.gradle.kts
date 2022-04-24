import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    base
    kotlin("jvm") version "1.6.20"
}

allprojects {
    group = "io.pleo"
    version = "1.0"

    repositories {
        mavenCentral()
        jcenter()
    }

    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions.jvmTarget = "11"
        kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}