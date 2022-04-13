# RedisBungee By Limework

[![RedisBungee Build](https://github.com/proxiodev/RedisBungee/actions/workflows/maven.yml/badge.svg)](https://github.com/Limework/RedisBungee/actions/workflows/maven.yml) [![](https://jitpack.io/v/limework/redisbungee.svg)](https://jitpack.io/#limework/redisbungee)

Spigot link: [click](https://www.spigotmc.org/resources/redisbungee.87700/)

The main project of RedisBungee is no longer maintained, so we have forked the plugin.

RedisBungee uses [Redis](https://redis.io) to Synchronize data between [BungeeCord](https://github.com/SpigotMC/BungeeCord) proxies

## Notice 1: about older versions of redis than redis 6.0

any versions of redis-bungee 0.6.4 or above only supports 6.0
Do not Open issues regarding it.

## Notice 2: users on git.limework.net

please create the issues on GitHub as its main project source.

## Compiling
RedisBungee is distributed as a [maven](https://maven.apache.org) project. 

To compile the plugin:

    git clone https://github.com/Limework/RedisBungee.git .
    mvn clean package
    mvn clean install # to install it in your maven local repo

then you should find it in target folder.

if you want to use it on your project considering you have installed it in your local repo

    <dependency>
      <groupId>com.imaginarycode.minecraft</groupId>
      <artifactId>RedisBungee</artifactId>
      <version>VERSION</version>
      <scope>provided</scope>
    </dependency>


## Javadocs
~~Hosted on limework website. Version 0.6.0~~ 
~~https://limework.net/JavaDocs/RedisBungee/~~

Check out our Java docs on github pages
https://proxiodev.github.io/RedisBungee-JavaDocs/0.6.5-SNAPSHOT

## Configuration

**REDISBUNGEE REQUIRES A REDIS SERVER**, preferably with reasonably low latency. The default [config](https://github.com/proxiodev/RedisBungee/blob/master/src/main/resources/example_config.yml) is saved when the plugin first starts.

## License!

This project is distributed under Eclipse Public License 1.0

You can find it [here](https://github.com/proxiodev/RedisBungee/blob/master/LICENSE)

You can find the original RedisBungee by minecrafter [here](https://github.com/minecrafter/RedisBungee) or spigot page [here](https://www.spigotmc.org/resources/redisbungee.13494/)

## Support

You can join our matrix room [here](https://matrix.to/#/!zhedzmRNSZXfuOPZUB:govindas.net?via=govindas.net&via=matrix.org)

![icon](https://matrix.org/images/matrix-logo-white.svg)


## YourKit

YourKit supports open source projects with innovative and intelligent tools for monitoring and profiling Java and .NET applications. YourKit is the creator of [YourKit Java Profiler](https://www.yourkit.com/java/profiler/), [YourKit .NET Profiler](https://www.yourkit.com/.net/profiler/) and [YourKit YouMonitor](https://www.yourkit.com/youmonitor/).

![YourKit](https://www.yourkit.com/images/yklogo.png)
