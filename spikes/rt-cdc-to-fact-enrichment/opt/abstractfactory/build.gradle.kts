val flinkVersion = "1.18-SNAPSHOT"

val mainClassName = "abstractfactory.FactFactory"

plugins {
    java
    application
    id("org.springframework.boot") version "3.0.4"
    id("io.spring.dependency-management") version "1.1.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("com.google.protobuf") version "0.9.2"
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://repository.apache.org/content/repositories/snapshots")
        mavenContent { snapshotsOnly() }
    }
    maven {
        url =
                uri(
                        "https://dl.cloudsmith.io/${System.getenv("CLOUDSMITH_MAVEN_RELEASES")}/carta/maven-releases/maven/"
                )
    }
}

val flinkShadow: Configuration by
        configurations.creating {
            dependencies {
                exclude("org.apache.flink", "force-shading")
                exclude("com.google.code.findbugs", "jsr305")
                exclude("org.slf4j")
                exclude("org.apache.logging.log4j")
            }
        }

configurations { compileOnly { extendsFrom(configurations.annotationProcessor.get()) } }

sourceSets {
    main {
        proto { srcDir("src/main/proto") }
        java {
            srcDir("src/main/java")
            srcDir("src/main/resources")
            srcDir("src/gen/java")
        }
        compileClasspath += flinkShadow
        runtimeClasspath += flinkShadow
    }
    test {
        java { srcDir("src/test/java") }
        compileClasspath += flinkShadow
        runtimeClasspath += flinkShadow
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

protobuf { protoc { path = "/opt/homebrew/bin/protoc" } }

application { mainClass.set(mainClassName) }

dependencies {
    annotationProcessor("org.projectlombok:lombok")
    compileOnly("org.projectlombok:lombok")

    implementation("org.apache.flink:flink-streaming-java:${flinkVersion}")
    implementation("org.apache.flink:flink-clients:${flinkVersion}")

    flinkShadow("org.apache.flink:flink-connector-kafka:${flinkVersion}")
    flinkShadow("com.jayway.jsonpath:json-path:2.8.0")
    flinkShadow("com.google.protobuf:protobuf-java:3.22.0")
    flinkShadow("com.google.protobuf:protobuf-java-util:3.22.0")
    flinkShadow("org.apache.logging.log4j:log4j-slf4j-impl")
    flinkShadow("org.apache.logging.log4j:log4j-api")
    flinkShadow("org.apache.logging.log4j:log4j-core")
    flinkShadow("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")
    flinkShadow("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    flinkShadow("com.fasterxml.jackson.datatype:jackson-datatype-jdk8")
}

fun deriveFromDockerCompose(fmt: String, svc: String, port: Int): String {
    val shellCmd = { cmd: String ->
        org.codehaus.groovy.runtime.ProcessGroovyMethods.getText(
                org.codehaus.groovy.runtime.ProcessGroovyMethods.execute(cmd)
        )
    }
    val dockerComposeHostPort = { s: String, p: Int -> shellCmd("docker compose port $s $p") }
    val dockerComposePort = { s: String, p: Int -> dockerComposeHostPort(s, p).split(":").last() }
    return String.format(fmt, dockerComposePort(svc, port))
}

fun getOrDeriveEvar(evar: String, fmt: String, svc: String, port: Int): Pair<String, String> {
    val env = System.getenv()
    return evar to env.getOrDefault(evar, deriveFromDockerCompose(fmt, svc, port))
}

tasks {
    withType<JavaCompile> { options.encoding = "UTF-8" }

    withType<JavaExec> {
        val evars =
                mapOf(
                        getOrDeriveEvar(
                                "DATABASE_URL",
                                "postgresql://localhost:%s/vesting?user=postgres&password=postgres",
                                "postgres",
                                5432
                        ),
                        getOrDeriveEvar("KAFKA_URL", "localhost:%s", "kafka", 9092),
                )
        environment(evars)
    }

    withType<Jar> { manifest { attributes["Main-Class"] = mainClassName } }

    withType<Copy> {
        filesMatching("**/*.proto") {
            duplicatesStrategy = org.gradle.api.file.DuplicatesStrategy.INCLUDE
        }
    }

    shadowJar {
        manifest { attributes["Main-Class"] = mainClassName }
        configurations = listOf(flinkShadow)
    }

    test {
        useJUnitPlatform()
        jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
    }
}
