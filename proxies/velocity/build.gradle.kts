plugins {
    java
    alias(libs.plugins.shadow)
    alias(libs.plugins.run.velocity)
}

dependencies {
    implementation(project(":RedisBungee-Velocity"))
    compileOnly(libs.platform.velocity)
    annotationProcessor(libs.platform.velocity)
    implementation(project(":RedisBungee-Commands"))
    implementation(libs.acf.velocity)
    implementation(project(":RedisBungee-Lang"))

}

description = "RedisBungee Velocity implementation"

java {
    withSourcesJar()
}

tasks {
    runVelocity {
        velocityVersion(libs.versions.velocity.get())
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
        relocate("okhttp3", "com.imaginarycode.minecraft.redisbungee.internal.okhttp3")
        relocate("kotlin", "com.imaginarycode.minecraft.redisbungee.internal.kotlin")
        relocate("okio", "com.imaginarycode.minecraft.redisbungee.internal.okio")
        relocate("org.json", "com.imaginarycode.minecraft.redisbungee.internal.json")
        // acf shade
        relocate("co.aikar.commands", "com.imaginarycode.minecraft.redisbungee.internal.acf.commands")
    }

}

