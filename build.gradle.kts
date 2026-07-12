plugins {
    id("java")
    id("application")
    id("com.gradleup.shadow") version "9.5.1"
    id("checkstyle")
    id("com.github.vlsi.gradle-extensions") version "3.0.2"
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
    implementation("org.slf4j:slf4j-api:2.0.18")
    implementation("ch.qos.logback:logback-core:1.5.34")
    implementation("ch.qos.logback:logback-classic:1.5.34")

    implementation("com.itextpdf:itextpdf:5.5.13.5")
    implementation("com.jcraft:jsch:0.1.55")
    implementation("org.jfree:jfreechart:1.5.6")

    testImplementation("org.junit.jupiter:junit-jupiter:6.1.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Xlint:unchecked")
    options.compilerArgs.add("-Xlint:deprecation")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    // The CI matrix (see .github/workflows/matrix.mjs) injects per-job JVM options such as
    // the test locale and JIT-stress flags here, so they reach the test workers without
    // affecting the Gradle daemon. Arguments are separated with " ::: " because a single
    // value may contain spaces.
    (project.findProperty("testExtraJvmArgs") as String?)
        ?.split(" ::: ")
        ?.filter { it.isNotBlank() }
        ?.let { jvmArgs(it) }
    // Forward the generator switch to the test workers so DateFormatHelperTest can be run with
    // -Pksar.generateDateFormats=true, without editing the test source.
    (project.findProperty("ksar.generateDateFormats") as String?)
        ?.let { systemProperty("ksar.generateDateFormats", it) }
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
