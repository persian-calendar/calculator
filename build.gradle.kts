plugins {
    java
    kotlin("jvm") version "1.6.10"
    `maven-publish`
}

group = "io.github.persiancalendar"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    val junit5Version = "5.8.2"
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junit5Version")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junit5Version")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junit5Version")
}

tasks.test {
    useJUnitPlatform()
}

configure<JavaPluginExtension> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

publishing {
    publications {
        register("mavenJava", MavenPublication::class) {
            from(components["java"])
        }
    }
}
