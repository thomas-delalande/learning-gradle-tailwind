import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermission
import org.jetbrains.kotlin.de.undercouch.gradle.tasks.download.Download
import org.jetbrains.kotlin.de.undercouch.gradle.tasks.download.Verify

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.20"
    kotlin("plugin.serialization") version "1.9.20"
    application
}

application {
    mainClass.set("app.MainKt")
    applicationName = "app"
}

tasks.distZip {
    enabled = false
}

tasks.distTar {
    archiveFileName.set("app-bundle.${archiveExtension.get()}")
}

repositories {
    mavenCentral()
}

tasks.test {
    useJUnitPlatform()
}
dependencies {
}

// Tailwind

val tailwindVersion = "3.4.1"
val tailwindBinary = "tailwindcss-macos-arm64"
val tailwindChecksum = "7558bb73a05b42b1b0e484458ddb88be"
val tailwindPath = ".tailwindcss-binaries/$tailwindBinary-$tailwindVersion"

val downloadTailwind by tasks.registering(Download::class) {
    src("https://github.com/tailwindlabs/tailwindcss/releases/download/v$tailwindVersion/$tailwindBinary")
    dest(tailwindPath)
    overwrite(false)
}

val verifyTailwind by tasks.registering(Verify::class) {
    dependsOn(downloadTailwind)
    src(File(tailwindPath))
    algorithm("MD5")
    checksum(tailwindChecksum)
}

val changePermissionsOnTailwindBinary by tasks.registering {
    dependsOn(downloadTailwind)

    val path = Paths.get("app/$tailwindPath")
    if (Files.exists(path)) {
        val permissions = setOf(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.OWNER_EXECUTE,
        )
        Files.setPosixFilePermissions(path, permissions)
    }
}

val buildCss by tasks.registering(Exec::class) {
    dependsOn(changePermissionsOnTailwindBinary)
    commandLine(
        tailwindPath,
        "-i",
        "src/main/resources/css/input.css",
        "-o",
        "src/main/resources/css/output.css",
        "--minify"
    )
}

tasks.getByName("assemble").dependsOn(buildCss)
