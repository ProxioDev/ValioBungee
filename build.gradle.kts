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
        java {
            removeUnusedImports()
            googleJavaFormat()
            if (project.name == "valiobungee-api") {
                licenseHeaderFile(file("copyright_header.txt"))
            } else {
                licenseHeaderFile(rootProject.file("copyright_header.txt"))
            }
            if (project.name == "valiobungee-core") {
                targetExclude("**/net/limework/valiobungee/core/proto/**")
            }
        }
    }


}
