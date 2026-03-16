import java.io.ByteArrayOutputStream

plugins {
    `java-library`
    `maven-publish`
    alias(libs.plugins.blossom)
    alias(libs.plugins.indragit)

}

dependencies {
    api(libs.guava)
    api(libs.jedis)
    api(libs.okhttp)
    api(libs.configurateV3)
    api(libs.caffeine)
}

description = "RedisBungee interfaces"

sourceSets {
    main {
        blossom {
            javaSources {
                property("version", "$version")
                property("git-commit", indraGit.commit().toString())
            }
        }
    }
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
        val configurateVersion = libs.configurateV3.get().version
        val guavaVersion = libs.guava.get().version
        val adventureVersion = libs.adventure.api.get().version
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
