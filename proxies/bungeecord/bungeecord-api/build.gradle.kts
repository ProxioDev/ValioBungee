plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":RedisBungee-API"))
    compileOnly(libs.platform.bungeecord) {
        exclude("com.google.guava", "guava")
        exclude("com.google.code.gson", "gson")
        exclude("net.kyori","adventure-api")
    }
    compileOnly(libs.adventure.platforms.bungeecord)
}

description = "RedisBungee Bungeecord API"

java {
    withJavadocJar()
    withSourcesJar()
}


tasks {
    withType<Javadoc> {
        dependsOn(project(":RedisBungee-API").getTasksByName("javadoc", false))
        val options = options as StandardJavadocDocletOptions
        options.use()
        options.isDocFilesSubDirs = true
        options.links(
            "https://ci.md-5.net/job/BungeeCord/ws/api/target/apidocs/", // bungeecord api
        )
        val apiDocs = File(rootProject.projectDir, "api/build/docs/javadoc")
        options.linksOffline("https://ci.limework.net/ValioBungee/api/build/docs/javadoc", apiDocs.path)
    }
    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(17)
    }
    javadoc {
        options.encoding = Charsets.UTF_8.name()
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}