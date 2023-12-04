plugins {
    id("java")
    id("application")
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("checkstyle")
    id("com.github.vlsi.gradle-extensions") version "1.90"
}

group = "com.github.vlsi.ksar"

if (!project.hasProperty("release")) {
    version = "$version-SNAPSHOT"
}

println("Building kSar $version")

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

application {
    mainClass.set("net.atomique.ksar.Main")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("ch.qos.logback:logback-core:1.4.14")
    implementation("ch.qos.logback:logback-classic:1.4.14")

    implementation("com.itextpdf:itextpdf:5.5.13.3")
    implementation("com.jcraft:jsch:0.1.55")
    implementation("org.jfree:jfreechart:1.5.4")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Xlint:unchecked")
    options.compilerArgs.add("-Xlint:deprecation")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
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

checkstyle {
    config = project.resources.text.fromFile("src/main/checkstyle/ksar-checks.xml", "UTF-8")
}

tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to "kSAR",
            "Implementation-Version" to archiveVersion,
            "SplashScreen-Image" to "logo_ksar.jpg",
        )
    }
}
