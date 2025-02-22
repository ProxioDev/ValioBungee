plugins {
    `java-library`
}

dependencies {
    compileOnly(project(":RedisBungee-API"))
    implementation(libs.acf.core)
    compileOnly(libs.adventure.api)
    compileOnly(libs.adventure.miniMessage)
    compileOnly(libs.adventure.gson)
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
