plugins {
    `java-library`
}

dependencies {
    implementation(project(":RedisBungee-API"))
    implementation(libs.acf.core)
}

description = "RedisBungee common commands"


tasks {
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
