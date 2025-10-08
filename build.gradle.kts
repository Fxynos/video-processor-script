plugins {
    kotlin("jvm") version "2.1.20"
}

group = "com.fxynos.multiprocessing.lab1"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.openpnp:opencv:4.9.0-0")
}

kotlin {
    jvmToolchain(17)
}