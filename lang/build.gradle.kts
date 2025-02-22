plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    compileOnly(project(":RedisBungee-API"))
    compileOnly(libs.adventure.api)
    compileOnly(libs.adventure.miniMessage)
}

description = "RedisBungee languages"

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
        val adventureVersion = libs.adventure.api.get().version
        options.links(
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
