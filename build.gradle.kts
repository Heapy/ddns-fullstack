plugins {
    application
    kotlin("jvm").version("1.9.22")
    kotlin("plugin.serialization").version("1.9.22")
}

application {
    mainClass.set("io.heapy.ddns.Application")
    applicationName = "ddns"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")

    implementation("io.ktor:ktor-client-cio:2.3.7")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.7")

    implementation("io.ktor:ktor-server-cio:2.3.7")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.7")
    implementation("io.ktor:ktor-server-call-logging:2.3.7")

    implementation("ch.qos.logback:logback-classic:1.4.14")

    testImplementation("io.ktor:ktor-server-tests:2.3.7")

    testImplementation(kotlin("test-junit"))
}
