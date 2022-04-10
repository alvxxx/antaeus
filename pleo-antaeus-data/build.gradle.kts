plugins {
    kotlin("jvm")
}

kotlinProject()

dataLibs()

dependencies {
    api(project(":pleo-antaeus-models"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.1")
}
