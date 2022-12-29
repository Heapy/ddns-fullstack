plugins {
    application
    kotlin("jvm").version("1.7.22")
    kotlin("plugin.serialization").version("1.7.22")
}

application {
    mainClass.set("io.heapy.ddns.Application")
    applicationName = "ddns"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.2.1")

    implementation("io.ktor:ktor-client-apache:2.2.1")
    implementation("io.ktor:ktor-client-content-negotiation:2.2.1")

    implementation("io.ktor:ktor-server-cio:2.2.1")
    implementation("io.ktor:ktor-server-content-negotiation:2.2.1")
    implementation("io.ktor:ktor-server-call-logging:2.2.1")

    implementation("ch.qos.logback:logback-classic:1.4.5")

    testImplementation("io.ktor:ktor-server-tests:2.2.1")

    testImplementation(kotlin("test-junit"))
}
