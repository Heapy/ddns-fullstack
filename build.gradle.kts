plugins {
    application
    kotlin("jvm").version("2.0.21")
    kotlin("plugin.serialization").version("2.0.21")
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
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.0.1")

    implementation("io.ktor:ktor-client-cio:3.0.1")
    implementation("io.ktor:ktor-client-content-negotiation:3.0.1")

    implementation("io.ktor:ktor-server-cio:3.0.1")
    implementation("io.ktor:ktor-server-content-negotiation:3.0.1")
    implementation("io.ktor:ktor-server-call-logging:3.0.1")

    implementation("ch.qos.logback:logback-classic:1.5.12")

    implementation("io.heapy.komok:komok-tech-dotenv:1.0.7")

    testImplementation("io.ktor:ktor-server-test-host:3.0.1")

    testImplementation(kotlin("test-junit"))
}
