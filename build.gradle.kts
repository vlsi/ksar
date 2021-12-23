plugins {
    id("java")
    kotlin("jvm") version "1.5.31"
    id("application")
    id("com.github.johnrengelman.shadow") version "7.0.0"
    id("com.github.vlsi.gradle-extensions") version "1.74"
    id("com.github.autostyle") version "3.1"
}

group = "com.github.vlsi.ksar"

setProperty("archivesBaseName", "ksar")

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

application {
    mainClass.set("net.atomique.ksar.Main")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.16.0")
    implementation("org.apache.logging.log4j:log4j-core:2.16.0")

    implementation("com.itextpdf:itextpdf:5.5.13.2")
    implementation("com.jcraft:jsch:0.1.55")
    implementation("org.jfree:jfreechart:1.5.3")

    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Xlint:unchecked")
    options.compilerArgs.add("-Xlint:deprecation")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

autostyle {
    kotlinGradle {
        ktlint("0.40.0") {
            userData(mapOf("disabled_rules" to "no-wildcard-imports,import-ordering"))
        }
        trimTrailingWhitespace()
        endWithNewline()
    }
}
plugins.withId("org.jetbrains.kotlin.jvm") {
    autostyle {
        kotlin {
            trimTrailingWhitespace()
            // Generated build/generated-sources/licenses/com/github/vlsi/gradle/license/api/License.kt
            // has wrong indentation, and it is not clear how to exclude it
            ktlint("0.40.0") {
                userData(mapOf("disabled_rules" to "no-wildcard-imports,import-ordering"))
            }
            // It prints errors regarding build/generated-sources/licenses/com/github/vlsi/gradle/license/api/License.kt
            // so comment it for now :(
            endWithNewline()
        }
    }
}

val writeVersion by tasks.registering {
    val outDir = project.layout.buildDirectory.dir("generated/version")
    val versionText = version.toString()
    inputs.property("ksar.version", versionText)
    outputs.dir(outDir)
    doLast {
        outDir.get().apply {
            asFile.mkdirs()
            file("kSar.version").asFile.writeText(versionText)
        }
    }
}

sourceSets.main.get().resources.srcDir(writeVersion)

tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to "kSAR",
            "Implementation-Version" to archiveVersion,
            "SplashScreen-Image" to "logo_ksar.jpg",
        )
    }
}
