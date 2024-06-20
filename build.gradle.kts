import org.jetbrains.kotlin.daemon.common.*
import proguard.gradle.*
import java.security.MessageDigest

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        //classpath("com.guardsquare:proguard-gradle:7.5.0")
        classpath("com.guardsquare:proguard-gradle:7.2.2")
    }
}

plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
    //id("edu.sc.seis.launch4j") version "3.0.5"
    kotlin("jvm") version "2.0.0"
    kotlin("plugin.compose") version "2.0.0"
    application
}

//apply(plugin = "com.guardsquare.proguard")

group = "org.korge"
version = "1.0.0"

var projectVersion = System.getenv("FORCED_VERSION")
    ?.replaceFirst(Regex("^refs/tags/"), "")
    ?: "unknown"
//?: props.getProperty("version")

repositories {
    mavenLocal()
    mavenCentral()
    google()
}

dependencies {
    implementation("org.jetbrains.compose.runtime:runtime:1.6.11")
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0-RC")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.9.0-RC")
    testImplementation(kotlin("test"))
    implementation("io.airlift:aircompressor:0.27")
    implementation("org.apache.commons:commons-compress:1.26.2")
    implementation("com.soywiz:korlibs-serialization-jvm:6.0.0-alpha5")
    implementation("com.soywiz:korlibs-dyn-jvm:6.0.0-alpha5")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0-RC")
}

tasks.test {
    useJUnitPlatform()
}

val appMainClassName = "korge.MainKt"

tasks {
    jar {
        // Specify the main class for the manifest
        manifest {
            attributes["Main-Class"] = appMainClassName
        }

        // Optionally, include additional resources or files
        //from(sourceSets.main.get().output)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8" // or any other JVM version you want to use
}

application {
    // Define the main class for the application
    mainClass.set(appMainClassName)
}

tasks {
    val proguard by creating(ProGuardTask::class) {
        dependsOn(shadowJar)
        //configurations = listOf("default")
        injars(shadowJar.get().outputs.files.toList())
        outjars("$buildDir/libs/korge-forge-installer-1.0.0-all.min.jar")
        dontwarn()
        ignorewarnings()
        dontobfuscate()
        keep("class $appMainClassName { *; }")
        keep("class kotlinx.coroutines.swing.** { *; }")

        //keepnames("class com.sun.jna.** { *; }")
        //println(configurations.getByName("runtimeClasspath").files.toList())
        //libraryjars(configurations.getByName("runtimeClasspath"))
        project.afterEvaluate {
            val javaHome = System.getProperty("java.home")
            libraryjars("$javaHome/lib/rt.jar")
            // Support newer java versions that doesn't have rt.jar
            libraryjars(project.fileTree("$javaHome/jmods/") {
                include("**/java.*.jmod")
            })
        }
    }

    val createInstallerJar by creating(Copy::class) {
        dependsOn(proguard)
        from("$buildDir/libs/korge-forge-installer-1.0.0-all.min.jar")
        into("$buildDir")
        rename { "korge-forge-installer.jar" }
        doLast {
            val JAR_SHA1 = MessageDigest.getInstance("SHA1").digest(File(projectDir, "build/korge-forge-installer.jar").readBytes()).toHexString()
            fun String.replaceScriptStrings(): String = this
                .replace(Regex("INSTALLER_URL=(.*)"), "INSTALLER_URL=https://github.com/korlibs/korge-forge-installer/releases/download/$projectVersion/korge-forge-installer.jar")
                .replace(Regex("INSTALLER_SHA1=(.*)"), "INSTALLER_SHA1=$JAR_SHA1")
                .replace(Regex("KORGE_FORGE_VERSION=(.*)"), "KORGE_FORGE_VERSION=$projectVersion")

            File(projectDir, "build/install-korge-forge.cmd").writeText(File(projectDir, "install-korge-forge.cmd").readText().replaceScriptStrings())
            File(projectDir, "build/install-korge-forge.sh").writeText(File(projectDir, "install-korge-forge.sh").readText().replaceScriptStrings())
        }
    }

    val unzipTcc by creating(Copy::class) {
        from(zipTree("tcc-0.9.27-win64-bin.zip"))
        into("build/tcc")
        //rename { it.removePrefix("tcc/") }
    }

    val createExe by creating(Exec::class) {
        dependsOn(unzipTcc)
        commandLine("wine64", "build/tcc/tcc/tcc.exe", "korge-forge-installer.c", "icons.res", "-o", "build/korge-forge-installer.exe")
    }
}

//launch4j {
//    mainClassName = appMainClassName
//    icon = "${projectDir}/install.ico"
//}

// "c:\dev\graalvm-jdk-22.0.1+8.1\bin\native-image" -Djava.awt.headless=false -jar korge-forge-installer.jar --no-fallback --report-unsupported-elements-at-runtime
