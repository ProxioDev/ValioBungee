import java.time.Instant
import java.io.ByteArrayOutputStream

plugins {
    `java-library`
    `maven-publish`
    id("net.kyori.blossom") version "1.2.0"

}

dependencies {
    api(libs.guava)
    api(libs.jedis)
    api(libs.okhttp)
    api(libs.configurate)
    api(libs.caffeine)
    api(libs.adventure.api)
    api(libs.adventure.gson)
    api(libs.adventure.legacy)
    api(libs.adventure.plain)
    api(libs.adventure.miniMessage)
}

description = "RedisBungee interfaces"

blossom {
    replaceToken("@version@", "$version")
    // GIT
    val commit: String;
    val commitStdout = ByteArrayOutputStream()
    rootProject.exec {
        standardOutput = commitStdout
        commandLine("git", "rev-parse", "HEAD")
    }
    commit = "$commitStdout".replace("\n", "") // for some reason it adds new line so remove it.
    commitStdout.close()
    replaceToken("@git_commit@", commit)
    replaceToken("@build_date@", "${Instant.now().epochSecond}")
}


java {
    withJavadocJar()
    withSourcesJar()
}

tasks {
    // thanks again for paper too
    withType<Javadoc> {
        val options = options as StandardJavadocDocletOptions
        options.use()
        options.isDocFilesSubDirs = true
        val jedisVersion = libs.jedis.get().version
        val configurateVersion = libs.configurate.get().version
        val guavaVersion = libs.guava.get().version
        val adventureVersion = libs.guava.get().version
        options.links(
            "https://configurate.aoeu.xyz/$configurateVersion/apidocs/", // configurate
            "https://javadoc.io/doc/redis.clients/jedis/$jedisVersion/", // jedis
            "https://guava.dev/releases/$guavaVersion/api/docs/", // guava
            "https://javadoc.io/doc/com.github.ben-manes.caffeine/caffeine",
            "https://jd.advntr.dev/api/$adventureVersion"

        )

    }

    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(17)
    }
    javadoc {
        options.encoding = Charsets.UTF_8.name()
    }
    processResources {
        filteringCharset = Charsets.UTF_8.name()
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
