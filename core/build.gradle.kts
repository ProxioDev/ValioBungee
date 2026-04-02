plugins {
    alias(libs.plugins.blossom)
    alias(libs.plugins.indragit)
    alias(libs.plugins.protobuf)
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

dependencies {
    api(project(":valiobungee-api"))
    api(libs.protobuf)
    api(libs.caffeine)
    api(libs.slf4j)
    api(libs.redisson)
    testImplementation(libs.testing.juipter)
    testImplementation(libs.testing.slf4j.simple)
}

tasks.test {
    useJUnitPlatform()
}

protobuf {
    protoc {
        artifact = libs.protoc.get().toString()
    }
}

