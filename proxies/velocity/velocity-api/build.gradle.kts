plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":RedisBungee-API")) {
        // Since velocity already includes guava / configurate exlude them
        exclude("com.google.guava", "guava")
        exclude("com.google.code.gson", "gson")
        exclude("org.spongepowered", "configurate-yaml")
        // exclude also adventure api
        exclude("net.kyori", "adventure-api")
        exclude("net.kyori", "adventure-text-serializer-gson")
        exclude("net.kyori", "adventure-text-serializer-legacy")
        exclude("net.kyori", "adventure-text-serializer-plain")
        exclude("net.kyori", "adventure-text-minimessage")
    }
    compileOnly(libs.platform.velocity)

}

description = "RedisBungee Velocity API"

java {
    withJavadocJar()
    withSourcesJar()
}

tasks {
    withType<Javadoc> {
        dependsOn(project(":RedisBungee-API").getTasksByName("javadoc", false))
        val path = project(":RedisBungee-API").path;
        val options = options as StandardJavadocDocletOptions
        options.use()
        options.isDocFilesSubDirs = true
        options.links(
            "https://jd.papermc.io/velocity/3.0.0/", // velocity api
        )
        val apiDocs = File(rootProject.projectDir, "$path/build/docs/javadoc")
        options.linksOffline("https://ci.limework.net/ValioBungee/api/build/docs/javadoc", apiDocs.path)
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
