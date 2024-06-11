import proguard.gradle.*

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
    kotlin("jvm") version "2.0.0"
    kotlin("plugin.compose") version "2.0.0"
    application
}

//apply(plugin = "com.guardsquare.proguard")

group = "org.korge"
version = "1.0.0"

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
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0-RC")
}

tasks.test {
    useJUnitPlatform()
}

val mainClassName = "korge.MainKt"

tasks {
    jar {
        // Specify the main class for the manifest
        manifest {
            attributes["Main-Class"] = mainClassName
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
    mainClass.set(mainClassName)
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
        keep("class $mainClassName { *; }")
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

    val createExe by creating(Copy::class) {
        dependsOn(proguard)
        from("$buildDir/libs/korge-forge-installer-1.0.0-all.min.jar")
        into("$buildDir")
        rename { "korge-forge-installer.jar" }
    }
}
