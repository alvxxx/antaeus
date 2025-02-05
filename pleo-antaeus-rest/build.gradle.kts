plugins {
    kotlin("jvm")
}

kotlinProject()

dependencies {
    implementation(project(":pleo-antaeus-core"))
    implementation(project(":pleo-antaeus-models"))

    implementation("io.javalin:javalin:4.4.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.2.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.1")
}
