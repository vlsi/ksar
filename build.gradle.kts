plugins {
    id("java")
    id("application")
    id("com.gradleup.shadow") version "9.2.2"
    id("checkstyle")
    id("com.github.vlsi.gradle-extensions") version "2.0.0"
}

group = "com.github.vlsi.ksar"

if (!project.hasProperty("release")) {
    version = "$version-SNAPSHOT"
}

println("Building kSar $version")

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

application {
    mainClass.set("net.atomique.ksar.Main")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation("ch.qos.logback:logback-core:1.5.20")
    implementation("ch.qos.logback:logback-classic:1.5.20")

    implementation("com.itextpdf:itextpdf:5.5.13.4")
    implementation("com.jcraft:jsch:0.1.55")
    implementation("org.jfree:jfreechart:1.5.6")

    testImplementation("org.junit.jupiter:junit-jupiter:5.14.0")
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
