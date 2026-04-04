plugins {
    java
    alias(libs.plugins.shadow)
    alias(libs.plugins.run.velocity)
}


dependencies {
    implementation(project(":valiobungee-velocity-api"))
    implementation(project(":valiobungee-core"))
    implementation(project(":valiobungee-core-standalone"))
    implementation(project(":valiobungee-core-redisson"))
    compileOnly(libs.platform.velocity)
    annotationProcessor(libs.platform.velocity)
}

description = "ValioBungee Velocity implementation"

tasks {
    runVelocity {
        velocityVersion(libs.versions.velocity.get())
    }
}

