@file:Suppress("UnstableApiUsage")
pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

rootProject.name = "ValioBungee"

fun configureProject(name: String, path: String) {
    include(name)
    project(name).projectDir = file(path)
}

configureProject(":RedisBungee-API", "api")
configureProject(":RedisBungee-Commands", "commands")

configureProject(":RedisBungee-Bungee", "proxies/bungeecord/bungeecord-api")
configureProject(":RedisBungee-Proxy-Bungee", "proxies/bungeecord")

configureProject(":RedisBungee-Velocity", "proxies/velocity/velocity-api")
configureProject(":RedisBungee-Proxy-Velocity", "proxies/velocity")

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://oss.sonatype.org/content/repositories/snapshots")
        maven("https://jitpack.io")
    }
}
