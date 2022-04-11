plugins {
    java
    antlr
    kotlin("jvm") version "1.6.10"
    `maven-publish`
}

group = "io.github.persiancalendar"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    antlr("org.antlr:antlr4:4.10")
    implementation("org.antlr:antlr4-runtime:4.9.3")
    testImplementation(kotlin("test"))
    val junit5Version = "5.8.2"
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junit5Version")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junit5Version")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junit5Version")
}

// https://github.com/gradle/gradle/issues/820#issuecomment-808315335
configurations[JavaPlugin.API_CONFIGURATION_NAME].let { apiConfiguration ->
    apiConfiguration.setExtendsFrom(apiConfiguration.extendsFrom.filter { it.name != "antlr" })
}

tasks.generateGrammarSource {
    maxHeapSize = "64m"
    arguments = arguments + listOf("-no-listener", "-visitor", "-package", "io.github.persiancalendar.calculator.parser")
    outputDirectory = File("${project.buildDir}/generated-src/antlr/main/io/github/persiancalendar/calculator/parser")
}
tasks.named("compileKotlin").configure { dependsOn(tasks.generateGrammarSource) }

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
