plugins {
    `java-library`
    `maven-publish`
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("xyz.jpenilla.run-velocity") version "2.0.0"
}

dependencies {
    api(project(":RedisBungee-API")) {
        // Since velocity already includes guava / configurate exlude them
        exclude("com.google.guava", "guava")
        exclude("com.google.code.gson", "gson")
        exclude("org.spongepowered", "configurate-yaml")
    }
    compileOnly("com.velocitypowered:velocity-api:3.2.0-SNAPSHOT")
    annotationProcessor("com.velocitypowered:velocity-api:3.2.0-SNAPSHOT")
}

description = "RedisBungee Velocity implementation"

java {
    withJavadocJar()
    withSourcesJar()
}

tasks {
    runVelocity {
        velocityVersion("3.2.0-SNAPSHOT")
    }
    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(11)
    }
    javadoc {
        options.encoding = Charsets.UTF_8.name()
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
    }

}


repositories {
    mavenCentral()
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
}
