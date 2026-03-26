plugins {
    alias(libs.plugins.blossom)
    alias(libs.plugins.indragit)
}

description = "Core functions for valiobungee"

sourceSets {
    main {
        blossom {
            javaSources {
                property("version", version.toString())
                property("git", indraGit.commit().get().name)
            }
        }
    }
}

java {
    withJavadocJar()
    withSourcesJar()
}
