plugins {
    `java-library`
    alias { libs.plugins.spotless } apply false
}


subprojects {
    apply { plugin("com.diffplug.spotless") }
    apply { plugin("java-library") }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
        withJavadocJar()
        withSourcesJar()
    }

    tasks {
        javadoc {
            options.encoding = Charsets.UTF_8.name()
        }
        processResources {
            filteringCharset = Charsets.UTF_8.name()
        }
    }
    extensions.configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        var redisBungeeProjects = sequenceOf("RedisBungee-API", "RedisBungee-Lang", "RedisBungee-Commands", "RedisBungee-Bungee", "RedisBungee-Proxy-Bungee", "RedisBungee-Velocity", "RedisBungee-Proxy-Velocity")
        var apiProjects = sequenceOf("valiobungee-api", "valiobungee-velocity-api")

        java {
            removeUnusedImports()
            googleJavaFormat()
            if (apiProjects.contains(project.name)) {
                licenseHeaderFile(rootProject.file("api/copyright_header.txt"))
            } else if (redisBungeeProjects.contains(project.name)) {
                licenseHeaderFile(rootProject.file("redisbungee/copyright_header.txt"))
            } else {
                licenseHeaderFile(rootProject.file("copyright_header.txt"))
            }
            if (project.name == "valiobungee-core") {
                targetExclude("**/net/limework/valiobungee/core/proto/**")
            }
        }
    }


}
