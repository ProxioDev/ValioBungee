plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("xyz.jpenilla.run-velocity") version "2.0.0"
}

dependencies {
    implementation(project(":RedisBungee-Velocity"))
    compileOnly(libs.platform.velocity)
    annotationProcessor(libs.platform.velocity)
    implementation(project(":RedisBungee-Commands"))
    implementation(libs.acf.velocity)

}

description = "RedisBungee Velocity implementation"


tasks {
    runVelocity {
        velocityVersion("3.3.0-SNAPSHOT")
        environment["REDISBUNGEE_PROXY_ID"] = "velocity-1"
        environment["REDISBUNGEE_NETWORK_ID"] = "dev"
    }
    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(17)
    }
    processResources {
        filteringCharset = Charsets.UTF_8.name()
    }
    shadowJar {
        relocate("redis.clients.jedis", "com.imaginarycode.minecraft.redisbungee.internal.jedis")
        relocate("redis.clients.util", "com.imaginarycode.minecraft.redisbungee.internal.jedisutil")
        relocate("org.apache.commons.pool", "com.imaginarycode.minecraft.redisbungee.internal.commonspool")
        relocate("com.squareup.okhttp", "com.imaginarycode.minecraft.redisbungee.internal.okhttp")
        relocate("okio", "com.imaginarycode.minecraft.redisbungee.internal.okio")
        relocate("org.json", "com.imaginarycode.minecraft.redisbungee.internal.json")
        relocate("com.github.benmanes.caffeine", "com.imaginarycode.minecraft.redisbungee.internal.caffeine")
        // acf shade
        relocate("co.aikar.commands", "com.imaginarycode.minecraft.redisbungee.internal.acf.commands")
    }

}

