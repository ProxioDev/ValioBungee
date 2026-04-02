plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    compileOnly(libs.platform.velocity)
    api(project(":valiobungee-api"))
}

description = "ValioBungee Velocity API"

java {
    withJavadocJar()
    withSourcesJar()
}

tasks {
    withType<Javadoc> {
        dependsOn(project(":valiobungee-api").getTasksByName("javadoc", false))
        val options = options as StandardJavadocDocletOptions
        options.use()
        options.isDocFilesSubDirs = true
        options.links(
            "https://jd.papermc.io/velocity/3.5.0/", // velocity api
        )
        val apiDocs = File(rootProject.projectDir, "api/build/docs/javadoc")
        //options.linksOffline("https://ci.limework.net/ValioBungee/api/build/docs/javadoc", apiDocs.path)
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
