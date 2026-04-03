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

tasks {
    runVelocity {
        velocityVersion(libs.versions.velocity.get())
    }
}

