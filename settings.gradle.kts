@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

rootProject.name = "ValioBungee"

fun configureRootProjects(name: String) {
    val projectName = ":valiobungee-$name"
    configureProject(projectName, name)
}

fun configureAPISubProject(name: String) {
    val projectName = ":valiobungee-$name-api"
    configureProject(projectName, "api/$name")
}

fun configureCoreSubProject(name: String) {
    val projectName = ":valiobungee-core-$name"
    configureProject(projectName, "core/$name")
}

fun configureProject(name: String, path: String) {
    include(name)
    project(name).projectDir = file(path)
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://oss.sonatype.org/content/repositories/snapshots")
        maven("https://jitpack.io")
    }
}

// main project stuff
sequenceOf("api", "core", "velocity").forEach { configureRootProjects(it) }
// core data implementations
sequenceOf("redisson", "standalone").forEach { configureCoreSubProject(it) }
// api
sequenceOf("velocity").forEach { configureAPISubProject(it) }

// RedisBunggee Project
// configureProject(":RedisBungee-API", "redisbungee/api")
// configureProject(":RedisBungee-Lang", "redisbungee/lang")
// configureProject(":RedisBungee-Commands", "redisbungee/commands")

// configureProject(":RedisBungee-Bungee", "redisbungee/proxies/bungeecord/bungeecord-api")
// configureProject(":RedisBungee-Proxy-Bungee", "redisbungee/proxies/bungeecord")

// configureProject(":RedisBungee-Velocity", "redisbungee/proxies/velocity/velocity-api")
// configureProject(":RedisBungee-Proxy-Velocity", "redisbungee/proxies/velocity")