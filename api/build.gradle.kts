plugins {
    `maven-publish`
}

description = "Api functions for valiobungee"

dependencies {
    testImplementation(libs.testing.juipter)
}

tasks.test {
    useJUnitPlatform()
}


publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}

