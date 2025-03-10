plugins {
    application
    kotlin("jvm").version("2.1.10")
    kotlin("plugin.serialization").version("2.1.10")
}

application {
    mainClass.set("io.heapy.ddns.Application")
    applicationName = "ddns"
}

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.1.1")

    implementation("io.ktor:ktor-client-cio:3.1.1")
    implementation("io.ktor:ktor-client-content-negotiation:3.1.1")

    implementation("io.ktor:ktor-server-cio:3.1.1")
    implementation("io.ktor:ktor-server-content-negotiation:3.1.1")
    implementation("io.ktor:ktor-server-call-logging:3.1.1")

    implementation("ch.qos.logback:logback-classic:1.5.17")

    implementation("io.heapy.komok:komok-tech-config-dotenv:1.0.12")

    testImplementation("io.ktor:ktor-server-test-host:3.1.1")

    testImplementation(kotlin("test-junit"))
}
