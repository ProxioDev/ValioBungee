plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("xyz.jpenilla.run-waterfall") version "2.0.0"
}

dependencies {
    implementation(project(":RedisBungee-Bungee"))
    compileOnly(libs.platform.bungeecord) {
        exclude("com.google.guava", "guava")
        exclude("com.google.code.gson", "gson")
        exclude("net.kyori","adventure-api")
    }
    implementation(libs.adventure.platforms.bungeecord)
    implementation(libs.adventure.gson)
    implementation(libs.acf.bungeecord)
    implementation(project(":RedisBungee-Commands"))
}

description = "RedisBungee Bungeecord implementation"

java {
    withJavadocJar()
    withSourcesJar()
}


tasks {
    withType<Javadoc> {
        dependsOn(project(":RedisBungee-API").getTasksByName("javadoc", false))
        val options = options as StandardJavadocDocletOptions
        options.use()
        options.isDocFilesSubDirs = true
        options.links(
            "https://ci.md-5.net/job/BungeeCord/ws/api/target/apidocs/", // bungeecord api
        )
        val apiDocs = File(rootProject.projectDir, "RedisBungee-API/build/docs/javadoc")
        options.linksOffline("https://ci.limework.net/RedisBungee/RedisBungee-API/build/docs/javadoc", apiDocs.path)
    }
    runWaterfall {
        waterfallVersion("1.20")
        environment["REDISBUNGEE_PROXY_ID"] = "bungeecord-1"
        environment["REDISBUNGEE_NETWORK_ID"] = "dev"
    }
    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(17)
    }
    javadoc {
        options.encoding = Charsets.UTF_8.name()
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
        relocate("com.squareup.okhttp", "com.imaginarycode.minecraft.redisbungee.internal.okhttp")
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

    }

}
