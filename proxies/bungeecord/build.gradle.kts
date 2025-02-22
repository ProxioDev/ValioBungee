plugins {
    java
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(project(":RedisBungee-Bungee"))
    compileOnly(libs.platform.bungeecord)
    implementation(libs.adventure.platforms.bungeecord)
    implementation(libs.adventure.miniMessage)
    implementation(libs.acf.bungeecord)
    implementation(project(":RedisBungee-Commands"))
    implementation(project(":RedisBungee-Lang"))
}

description = "RedisBungee Bungeecord implementation"

java {
    withSourcesJar()
}

tasks {

    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(17)
    }
    processResources {
        filteringCharset = Charsets.UTF_8.name()
        filesMatching("plugin.yml") {
            filter {
                it.replace("*{redisbungee.version}*", "$version", false)
            }
        }

    }
    shadowJar {
        relocate("redis.clients.jedis", "com.imaginarycode.minecraft.redisbungee.internal.jedis")
        relocate("redis.clients.util", "com.imaginarycode.minecraft.redisbungee.internal.jedisutil")
        relocate("org.apache.commons.pool", "com.imaginarycode.minecraft.redisbungee.internal.commonspool")
        relocate("okhttp3", "com.imaginarycode.minecraft.redisbungee.internal.okhttp3")
        relocate("kotlin", "com.imaginarycode.minecraft.redisbungee.internal.kotlin")
        relocate("okio", "com.imaginarycode.minecraft.redisbungee.internal.okio")
        relocate("org.json", "com.imaginarycode.minecraft.redisbungee.internal.json")
        // configurate shade
        relocate("ninja.leaping.configurate", "com.imaginarycode.minecraft.redisbungee.internal.configurate")
        relocate("org.yaml", "com.imaginarycode.minecraft.redisbungee.internal.yml")
        relocate("com.google.common", "com.imaginarycode.minecraft.redisbungee.internal.com.google.common")
        relocate("com.google.errorprone", "com.imaginarycode.minecraft.redisbungee.internal.com.google.errorprone")
        relocate("com.google.gson", "com.imaginarycode.minecraft.redisbungee.internal.com.google.gson")
        relocate("com.google.j2objc", "com.imaginarycode.minecraft.redisbungee.internal.com.google.j2objc")
        relocate("com.google.thirdparty", "com.imaginarycode.minecraft.redisbungee.internal.com.google.thirdparty")
        relocate("com.github.benmanes.caffeine", "com.imaginarycode.minecraft.redisbungee.internal.caffeine")
        // acf shade
        relocate("co.aikar.commands", "com.imaginarycode.minecraft.redisbungee.internal.acf.commands")
        // adventure :/
        relocate("net.kyori", "com.imaginarycode.minecraft.redisbungee.internal.net.kyori")

    }

}
