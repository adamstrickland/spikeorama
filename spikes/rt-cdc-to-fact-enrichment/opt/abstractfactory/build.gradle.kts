//import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

//val kotlin_version = "1.8.10"
//val spek_version = "2.0.19"
//val mapstruct_version = "1.5.3.Final"
//val protobuf_version = "3.22.0"
val flinkVersion = "1.18-SNAPSHOT"

val mainClassName = "abstractfactory.FactFactory"

plugins {
    java
    application
//    kotlin("jvm") version "1.8.10"
    id("org.springframework.boot") version "3.0.4"
    id("io.spring.dependency-management") version "1.1.0"
//    id("org.flywaydb.flyway") version "9.8.1"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("com.google.protobuf") version "0.9.2"
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://repository.apache.org/content/repositories/snapshots")
        mavenContent {
            snapshotsOnly()
        }
    }
    maven {
        url =
            uri(
                "https://dl.cloudsmith.io/${System.getenv("CLOUDSMITH_MAVEN_RELEASES")}/carta/maven-releases/maven/"
            )
    }
}

val flinkShadow: Configuration by configurations.creating {
    dependencies {
        exclude("org.apache.flink", "force-shading")
        exclude("com.google.code.findbugs", "jsr305")
        exclude("org.slf4j")
        exclude("org.apache.logging.log4j")
    }
}

configurations {
    compileOnly { extendsFrom(configurations.annotationProcessor.get()) }
}

sourceSets {
    main {
        proto {
            srcDir("src/main/proto")
        }
        java {
            srcDir("src/main/java")
            srcDir("src/main/resources")
            srcDir("src/gen/java")
//            srcDir("../../src/gen/java")
//            srcDir("${buildDir}/generated/sources/annotationProcessor/java/main")
        }
        compileClasspath += flinkShadow
        runtimeClasspath += flinkShadow
    }
    test {
//        kotlin { srcDir("src/test/kotlin") }
        java { srcDir("src/test/java") }
        compileClasspath += flinkShadow
        runtimeClasspath += flinkShadow
    }
}

java {
    //  group = "com.carta.rra"
    //  version = "0.0.1-SNAPSHOT"
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
//    toolchain {
//        languageVersion.set(JavaLanguageVersion.of(11))
//    }
}

protobuf {
    protoc {
        path = "/opt/homebrew/bin/protoc"
    }
}

application {
    mainClass.set(mainClassName)
}

//run.classpath = sourceSets.main.

//run {
////    classpath = sourceSets.main.runtimeClasspath
//}

dependencies {
    // must come before mapstruct processor
    annotationProcessor("org.projectlombok:lombok")
//    annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")
    compileOnly("org.projectlombok:lombok")

//    annotationProcessor("org.mapstruct:mapstruct-processor:$mapstruct_version")
//    testAnnotationProcessor("org.mapstruct:mapstruct-processor:$mapstruct_version")

//    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

//    compileOnly("org.flywaydb:flyway-core")
//    developmentOnly("org.springframework.boot:spring-boot-devtools")

    implementation("org.apache.flink:flink-streaming-java:${flinkVersion}")
    implementation("org.apache.flink:flink-clients:${flinkVersion}")
//    implementation("org.apache.flink:flink-connector-kafka:${flinkVersion}")

    flinkShadow("org.apache.flink:flink-connector-kafka:${flinkVersion}")

    flinkShadow("com.jayway.jsonpath:json-path:2.8.0")


//    implementation("com.carta.events:carta-events-common-java:0.0.4+")
//    implementation("com.carta.events:carta-kafka-java:0.0.13+")
//    implementation("com.carta.proto:carta-proto-events:0.1.199+")

    flinkShadow("com.google.protobuf:protobuf-java:3.22.0")
    flinkShadow("com.google.protobuf:protobuf-java-util:3.22.0")
    //  implementation("com.hubspot.jackson:jackson-datatype-protobuf:0.9.11-jackson2.8")
//    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version")
//    implementation("org.mapstruct:mapstruct:$mapstruct_version")
//    implementation("org.postgresql:postgresql")
//    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
//    implementation("org.springframework.boot:spring-boot-starter-quartz")
//    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
//    implementation("org.springframework.boot:spring-boot-starter-web")
//    implementation("org.springframework.kafka:spring-kafka")

    flinkShadow("org.apache.logging.log4j:log4j-slf4j-impl")
    flinkShadow("org.apache.logging.log4j:log4j-api")
    flinkShadow("org.apache.logging.log4j:log4j-core")

    flinkShadow("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")
    flinkShadow("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    flinkShadow("com.fasterxml.jackson.datatype:jackson-datatype-jdk8")

//    testImplementation("com.github.javafaker:javafaker:1.0.2")
//    testImplementation("io.beanmother:beanmother:0.9.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.0")
//    testImplementation("org.mockito:mockito-all:1.10.19")
//    testImplementation("org.spekframework.spek2:spek-dsl-jvm:$spek_version")
//    testImplementation("org.springframework.boot:spring-boot-starter-test")
//    testImplementation("org.springframework.kafka:spring-kafka-test")

//    testRuntimeOnly("org.jetbrains.kotlin:kotlin-reflect:$kotlin_version")
//    testRuntimeOnly("org.spekframework.spek2:spek-runner-junit5:$spek_version")
}

fun deriveFromDockerCompose(fmt: String, svc: String, port: Int): String {
    val shellCmd = { cmd: String -> org.codehaus.groovy.runtime.ProcessGroovyMethods.getText(
        org.codehaus.groovy.runtime.ProcessGroovyMethods.execute(cmd)) }
    val dockerComposeHostPort = { s: String, p: Int -> shellCmd("docker compose port $s $p") }
    val dockerComposePort = { s: String, p: Int -> dockerComposeHostPort(s, p).split(":").last() }
    return String.format(fmt, dockerComposePort(svc, port))
}

fun getOrDeriveEvar(evar: String, fmt: String, svc: String, port: Int): Pair<String, String> {
    val env = System.getenv()
    return evar to env.getOrDefault(evar, deriveFromDockerCompose(fmt, svc, port))
}

tasks {
//    withType<KotlinCompile> {
//        kotlinOptions.jvmTarget = JavaVersion.VERSION_17.toString()
//    }
    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    withType<JavaExec> {
        val evars = mapOf(
            getOrDeriveEvar("DATABASE_URL",
                "postgresql://localhost:%s/vesting?user=postgres&password=postgres",
                "postgres",
                5432),
            getOrDeriveEvar("KAFKA_URL",
                "localhost:%s",
                "kafka",
                9092),
        )
        environment(evars)
    }

    withType<Jar> {
        manifest {
            attributes["Main-Class"] = mainClassName
        }
    }

    withType<Copy> {
        filesMatching("**/*.proto") {
            duplicatesStrategy = org.gradle.api.file.DuplicatesStrategy.INCLUDE
        }
    }

//    flywayMigrate {
//        dependsOn("classes")
//    }
//    sun.tools.jar.resources.jar {
//        manifest {
////            attributes("Build-By": System.getProperty("user.name"),
////                "Build-Jdk": System.getProperty("java.version"))
//        }
//    }

    shadowJar {
        manifest {
            attributes["Main-Class"] = mainClassName
        }
        configurations = listOf(flinkShadow)
//        configurations {
//            flinkShadow
//        }
    }

    test {
        // specs (i.e. kotlin tests)
//        useJUnitPlatform { includeEngines("spek2") }

        // java tests
        useJUnitPlatform()

        jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
    }
}

//flyway { url = "jdbc:${System.getenv("DATABASE_URL")}" }

//flyway.cleanDisabled = false

//sun.tools.jar.resources.jar {
//
//}
