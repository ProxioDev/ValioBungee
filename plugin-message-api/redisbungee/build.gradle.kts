plugins {
    `java-library`
    `maven-publish`

}

dependencies {
    compileOnly(project(":Limework-Plugin-Message-API-Protocol"))
    compileOnly(libs.guava)
}

tasks {
    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(8) // use java 8 for shit servers that still stuck on 1.8
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
