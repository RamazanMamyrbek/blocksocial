plugins {
    kotlin("jvm") version "2.2.21"
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("org.json:json:20260719")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "failed")
    }
}
