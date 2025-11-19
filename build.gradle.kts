import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform") version "2.2.21"
    kotlin("native.cocoapods") version "2.2.21"
    id("org.jetbrains.dokka") version "2.1.0"
    id("com.vanniktech.maven.publish") version "0.35.0"
    id("com.diffplug.spotless") version "8.1.0"

    signing
}

spotless {
    kotlin {
        target("**/*.kt", "**/*.kts")
        ktfmt().kotlinlangStyle().configure { ktfmt ->
            ktfmt.setMaxWidth(120)
            ktfmt.setRemoveUnusedImports(true)
        }
    }
}

val v = "0.1.0"

group = "xyz.calcugames"

version =
    "${if (project.hasProperty("snapshot")) "$v-SNAPSHOT" else v}${project.findProperty("suffix")?.toString()?.run { "-${this}" } ?: ""}"

val desc = "Kotlin/Native Bindings for SDL3"

description = desc

repositories {
    google()
    mavenCentral()
    mavenLocal()
}

java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }

kotlin {
    configureSourceSets()
    applyDefaultHierarchyTemplate()
    withSourcesJar()

    val hostOs = System.getProperty("os.name").lowercase()
    val hostArch = System.getProperty("os.arch")

    println("Host OS: $hostOs")
    println("Host Arch: $hostArch")

    val isX64 = hostArch == "x86_64" || hostArch == "amd64"
    val isArm64 = hostArch == "aarch64" || hostArch == "arm64"
    val isWindows = hostOs.startsWith("windows")
    val isMacos = hostOs.startsWith("mac os x") || hostOs.startsWith("darwin")
    val isLinux = hostOs.startsWith("linux")

    if (isX64) {
        androidNativeX86()
        androidNativeX64()
    }

    if (isArm64) {
        androidNativeArm32()
        androidNativeArm64()
    }

    if (isWindows && isX64) {
        mingwX64()
    }

    if (isMacos) {
        if (isX64) {
            iosX64()
            macosX64()
            tvosX64()
        }

        if (isArm64) {
            macosArm64()
            iosArm64()
            iosSimulatorArm64()
            tvosArm64()
            tvosSimulatorArm64()
        }
    }

    if (isLinux) {
        if (isX64) {
            linuxX64()
        }

        if (isArm64) {
            linuxArm64()
        }
    }

    cocoapods {
        version = project.version.toString()
        summary = desc
        homepage = "https://github.com/CalculusGames/SDLkt"
        name = "sdlkt"

        framework {
            baseName = "sdlkt"
            isStatic = false
        }
    }

    targets.filterIsInstance<KotlinNativeTarget>().forEach { target ->
        target.compilations.all {
            cinterops {
                val sdl3 by creating { definitionFile.set(file("sdl3.def")) }
            }
        }
    }
}

fun KotlinMultiplatformExtension.configureSourceSets() {
    sourceSets
        .matching { it.name !in listOf("main", "test") }
        .all {
            val srcDir = if ("Test" in name) "test" else "main"
            val resourcesPrefix = if (name.endsWith("Test")) "test-" else ""
            val platform =
                when {
                    (name.endsWith("Main") || name.endsWith("Test")) && "android" !in name ->
                        name.dropLast(4)
                    else -> name.substringBefore(name.first { it.isUpperCase() })
                }

            kotlin.srcDir("src/$platform/$srcDir")
            resources.srcDir("src/$platform/${resourcesPrefix}resources")

            languageSettings.apply { progressiveMode = true }
        }
}

signing {
    val signingKey: String? by project
    val signingPassword: String? by project

    if (signingKey != null && signingPassword != null)
        useInMemoryPgpKeys(signingKey, signingPassword)

    sign(publishing.publications)
}

publishing {
    publications {
        filterIsInstance<MavenPublication>().forEach {
            it.apply {
                pom {
                    name = "TabroomAPI"

                    licenses {
                        license {
                            name = "MIT License"
                            url = "https://opensource.org/licenses/MIT"
                        }
                    }

                    developers {
                        developer {
                            id = "gmitch215"
                            name = "Gregory Mitchell"
                            email = "me@gmitch215.xyz"
                        }
                    }

                    scm {
                        connection = "scm:git:git://github.com/CalculusGames/SDLkt.git"
                        developerConnection = "scm:git:ssh://github.com/CalculusGames/SDLkt.git"
                        url = "https://github.com/CalculusGames/SDLkt"
                    }
                }
            }
        }
    }

    repositories {
        maven {
            name = "CalculusGames"
            credentials {
                username = System.getenv("NEXUS_USERNAME")
                password = System.getenv("NEXUS_PASSWORD")
            }

            val releases = "https://repo.calcugames.xyz/repository/maven-releases/"
            val snapshots = "https://repo.calcugames.xyz/repository/maven-snapshots/"
            url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshots else releases)
        }

        if (!version.toString().endsWith("SNAPSHOT")) {
            maven {
                name = "GithubPackages"
                credentials {
                    username = System.getenv("GITHUB_ACTOR")
                    password = System.getenv("GITHUB_TOKEN")
                }

                url = uri("https://maven.pkg.github.com/CalculusGames/SDLkt")
            }
        }
    }
}

mavenPublishing {
    coordinates(project.group.toString(), project.name, project.version.toString())

    pom {
        name.set("sdlkt")
        description.set(desc)
        url.set("https://github.com/CalculusGames/SDLkt")
        inceptionYear.set("2025")

        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
            }
        }

        developers {
            developer {
                id = "gmitch215"
                name = "Gregory Mitchell"
                email = "me@gmitch215.xyz"
            }
        }

        scm {
            connection = "scm:git:git://github.com/CalculusGames/SDLkt.git"
            developerConnection = "scm:git:ssh://github.com/CalculusGames/SDLkt.git"
            url = "https://github.com/CalculusGames/SDLkt"
        }
    }

    publishToMavenCentral(true)
    signAllPublications()
}
