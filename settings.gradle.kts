pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

rootProject.name = "ValioBungee"

val projects = arrayOf(
    ":RedisBungee-API" to "api",
    ":RedisBungee-Commands" to "commands",
    ":RedisBungee-Velocity" to "proxies/velocity",
    ":RedisBungee-Bungee" to "proxies/bungeecord/bungeecord-api",
    ":RedisBungee-Proxy-Bungee" to "proxies/bungeecord",
    ":RedisBungee-Velocity" to "proxies/velocity/velocity-api",
    ":RedisBungee-Proxy-Velocity" to "proxies/velocity",
)


projects.forEach { (projectName, filePath) ->
    include(projectName)
    project(projectName).projectDir = file(filePath)
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven {
            name = "PaperMC"
            url = uri("https://repo.papermc.io/repository/maven-public/")
        }
        maven {
            // hosts the bungeecord apis
            name = "sonatype"
            url = uri("https://oss.sonatype.org/content/repositories/snapshots")
        }
        maven {
            name = "aikar repo"
            url = uri("https://repo.aikar.co/content/groups/aikar/")
        }

    }
    versionCatalogs {
        val jedisVersion = "5.1.2"
        val configurateVersion = "3.7.3"
        val guavaVersion = "31.1-jre"
        val okHttpVersion = "2.7.5"
        val caffeineVersion = "3.1.8"
        val adventureVersion = "4.16.0"
        val acf = "0.5.1-SNAPSHOT"
        val bungeecordApiVersion = "1.20-R0.1-SNAPSHOT"
        val velocityVersion = "3.3.0-SNAPSHOT";


        create("libs") {

            library("guava", "com.google.guava:guava:$guavaVersion")
            library("jedis", "redis.clients:jedis:$jedisVersion")
            library("okhttp", "com.squareup.okhttp:okhttp:$okHttpVersion")
            library("configurate", "org.spongepowered:configurate-yaml:$configurateVersion")
            library("caffeine", "com.github.ben-manes.caffeine:caffeine:$caffeineVersion")

            library("adventure-api", "net.kyori:adventure-api:$adventureVersion")
            library("adventure-gson", "net.kyori:adventure-text-serializer-gson:$adventureVersion")
            library("adventure-legacy", "net.kyori:adventure-text-serializer-legacy:$adventureVersion")
            library("adventure-plain", "net.kyori:adventure-text-serializer-plain:$adventureVersion")
            library("adventure-miniMessage", "net.kyori:adventure-text-minimessage:$adventureVersion")

            library("acf-core", "co.aikar:acf-core:$acf")
            library("acf-bungeecord", "co.aikar:acf-bungee:$acf")
            library("acf-velocity", "co.aikar:acf-velocity:$acf")

            library("platform-bungeecord","net.md-5:bungeecord-api:$bungeecordApiVersion")
            library("adventure-platforms-bungeecord", "net.kyori:adventure-platform-bungeecord:4.3.2")

            library("platform-velocity", "com.velocitypowered:velocity-api:$velocityVersion")




        }


    }


}
