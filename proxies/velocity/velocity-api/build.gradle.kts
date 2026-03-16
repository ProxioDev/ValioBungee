plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":RedisBungee-API")) {
        // Since velocity already includes guava / configurate / guava exlude them
        exclude("com.google.guava", "guava")
        exclude("com.google.code.gson", "gson")
        exclude("org.spongepowered", "configurate-yaml")
        exclude("com.github.ben-manes.caffeine", "caffeine")
    }
    implementation(project(":RedisBungee-Lang"))
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
        val options = options as StandardJavadocDocletOptions
        options.use()
        options.isDocFilesSubDirs = true
        options.links(
            "https://jd.papermc.io/velocity/3.0.0/", // velocity api
        )
        val apiDocs = File(rootProject.projectDir, "api/build/docs/javadoc")
        options.linksOffline("https://ci.limework.net/ValioBungee/api/build/docs/javadoc", apiDocs.path)
    }
    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(21) // required by velocity
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
