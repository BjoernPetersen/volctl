import com.diffplug.spotless.LineEnding
import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    id("com.diffplug.gradle.spotless") version Plugin.SPOTLESS
    id("io.gitlab.arturbosch.detekt") version Plugin.DETEKT

    id("com.github.ben-manes.versions") version Plugin.VERSIONS

    kotlin("jvm") version Plugin.KOTLIN
    `java-library`

    id("org.jetbrains.dokka") version Plugin.DOKKA
    idea

    signing
    `maven-publish`
}

group = "com.github.bjoernpetersen"
version = "1.1.0-SNAPSHOT"

tasks {
    create<Exec>("generateHeader") {
        dependsOn("compileKotlin")
        val classpath = "$buildDir/classes/kotlin/main"
        val output = "native/volctl.h"
        setCommandLine(
            "javah",
            "-jni",
            "-cp", classpath,
            "-o", output,
            "net.bjoernpetersen.volctl.VolumeControl"
        )
    }

    create<Exec>("setupNative") {
        dependsOn("generateHeader")
        setWorkingDir("native")
        setCommandLine("cmake", "-S", ".", "-B", "build")
    }

    create<Exec>("buildNative") {
        dependsOn("setupNative")
        setWorkingDir("native")
        setCommandLine("cmake", "--build", "build", "--config", "release")
    }

    "dokka"(DokkaTask::class) {
        outputFormat = "html"
        outputDirectory = "$buildDir/kdoc"
    }

    @Suppress("UNUSED_VARIABLE")
    val dokkaJavadoc by creating(DokkaTask::class) {
        outputFormat = "javadoc"
        outputDirectory = "$buildDir/javadoc"
    }

    @Suppress("UNUSED_VARIABLE")
    val javadocJar by creating(Jar::class) {
        dependsOn("dokkaJavadoc")
        archiveClassifier.set("javadoc")
        from("$buildDir/javadoc")
    }

    @Suppress("UNUSED_VARIABLE")
    val sourcesJar by creating(Jar::class) {
        archiveClassifier.set("sources")
        from(sourceSets["main"].allSource)
    }

    withType<Jar> {
        from(project.projectDir) {
            include("LICENSE")
        }
    }
}

sourceSets {
    main {
        resources {
            srcDir(files("$rootDir/native/build/Release") {
                include("*.dll")
            })
        }
    }
}

spotless {
    kotlin {
        ktlint()
        lineEndings = LineEnding.UNIX
        endWithNewline()
    }
    kotlinGradle {
        ktlint()
        lineEndings = LineEnding.UNIX
        endWithNewline()
    }
    format("markdown") {
        target("**/*.md")
        lineEndings = LineEnding.UNIX
        endWithNewline()
    }
}

detekt {
    toolVersion = Plugin.DETEKT
    config = files("$rootDir/buildConfig/detekt.yml")
    buildUponDefaultConfig = true
}

idea {
    module {
        isDownloadJavadoc = true
    }
}

dependencies {
    implementation(kotlin("stdlib"))
}

repositories {
    jcenter()
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_6
    targetCompatibility = JavaVersion.VERSION_1_6
}

publishing {
    publications {
        create("Maven", MavenPublication::class) {
            from(components["java"])
            artifact(tasks.getByName("javadocJar"))
            artifact(tasks.getByName("sourcesJar"))

            pom {
                name.set("volctl")
                description.set("Library to control system audio master volume.")
                url.set("https://github.com/BjoernPetersen/volctl")

                licenses {
                    license {
                        name.set("MIT")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                scm {
                    connection.set("scm:git:https://github.com/BjoernPetersen/volctl.git")
                    developerConnection.set("scm:git:git@github.com:BjoernPetersen/volctl.git")
                    url.set("https://github.com/BjoernPetersen/volctl")
                }

                developers {
                    developer {
                        id.set("BjoernPetersen")
                        name.set("Björn Petersen")
                        email.set("pheasn@gmail.com")
                        url.set("https://github.com/BjoernPetersen")
                    }
                }
            }
        }
        repositories {
            maven {
                val releasesRepoUrl = "https://oss.sonatype.org/service/local/staging/deploy/maven2"
                val snapshotsRepoUrl = "https://oss.sonatype.org/content/repositories/snapshots"
                // change to point to your repo, e.g. http://my.org/repo
                url = uri(
                    if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl
                    else releasesRepoUrl
                )
                credentials {
                    username = project.properties["ossrh.username"]?.toString()
                    password = project.properties["ossrh.password"]?.toString()
                }
            }
        }
    }
}

signing {
    sign(publishing.publications.getByName("Maven"))
}
