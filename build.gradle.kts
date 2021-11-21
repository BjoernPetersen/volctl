import com.diffplug.spotless.LineEnding

plugins {
    alias(libs.plugins.spotless)
    alias(libs.plugins.versions)

    `java-library`
    idea

    signing
    `maven-publish`
}

group = "com.github.bjoernpetersen"
version = "3.0.0"

tasks {
    create<Exec>("generateHeader") {
        dependsOn("compileJava")
        val classpath = "$buildDir/classes/java/main"
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

    @Suppress("UNUSED_VARIABLE")
    val javadocJar by creating(Jar::class) {
        dependsOn("javadoc")
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

    withType<Test> {
        useJUnitPlatform()
    }
}

sourceSets {
    main {
        resources {
            srcDir(files("$rootDir/native/build/Release") {
                include("*.dll")
            })
            srcDir(files("$rootDir/native/build") {
                include("*.so")
            })
        }
    }
}

spotless {
    java {
        indentWithSpaces(4)
        trimTrailingWhitespace()
        removeUnusedImports()
        encoding = Charsets.UTF_8
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

idea {
    module {
        isDownloadJavadoc = true
    }
}

dependencies {
    compileOnly(libs.annotations)
    testImplementation(libs.junit.api)
    testRuntimeOnly(libs.junit.engine)
}

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
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
                        email.set("git@bjoernpetersen.net")
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
