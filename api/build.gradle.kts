description = "Api functions for valiobungee"

plugins {
    alias(libs.plugins.blossom)
    alias(libs.plugins.indragit)
}


sourceSets {
    main {
        blossom {
            javaSources {
                property("apiversion", project.property("api-version").toString())
            }
        }
    }
}

java {
    withJavadocJar()
    withSourcesJar()
}

dependencies {
    testImplementation(libs.testing.juipter)
}

tasks.test {
    useJUnitPlatform()
}

