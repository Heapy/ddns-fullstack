plugins {
    application
    kotlin("jvm").version("1.8.0")
    kotlin("plugin.serialization").version("1.8.0")
}

application {
    mainClass.set("io.heapy.ddns.Application")
    applicationName = "ddns"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.2.2")

    implementation("io.ktor:ktor-client-cio:2.2.2")
    implementation("io.ktor:ktor-client-content-negotiation:2.2.2")

    implementation("io.ktor:ktor-server-cio:2.2.2")
    implementation("io.ktor:ktor-server-content-negotiation:2.2.2")
    implementation("io.ktor:ktor-server-call-logging:2.2.2")

    implementation("ch.qos.logback:logback-classic:1.4.5")

    testImplementation("io.ktor:ktor-server-tests:2.2.2")

    testImplementation(kotlin("test-junit"))
}
