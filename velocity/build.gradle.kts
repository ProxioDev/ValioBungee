plugins {
    java
    alias(libs.plugins.shadow)
    alias(libs.plugins.run.velocity)
}


dependencies {
    implementation(project(":valiobungee-velocity-api"))
    implementation(project(":valiobungee-core"))
    compileOnly(libs.platform.velocity)
    annotationProcessor(libs.platform.velocity)
}

description = "ValioBungee Velocity implementation"

java {
    withSourcesJar()
}

tasks {
    runVelocity {
        velocityVersion(libs.versions.velocity.get())
    }
    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(21) // required by velocity
    }
    processResources {
        filteringCharset = Charsets.UTF_8.name()
    }


}

