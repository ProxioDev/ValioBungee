# RedisBungee fork By Limework

Spigot link: [click](https://www.spigotmc.org/resources/redisbungee.87700/)

The main project of RedisBungee is no longer maintained, so we have forked the plugin.

*if you are here for transferring players to another proxy when the first proxy crashes or whatever this plugin won't do it, tell mojang to implement transfer packet*

RedisBungee uses [Redis](https://redis.io) to Synchronize players data between [BungeeCord](https://github.com/SpigotMC/BungeeCord) or [Velocity](https://github.com/PaperMC/Velocity) proxies

If you are looking to use Original RedisBungee without a change to internals,
with critical bugs fixed, please use version [0.6.5](https://github.com/ProxioDev/RedisBungee/releases/tag/0.6.5)
`you exspect that you will not get support for old versions.`

## Supported Redis versions
| Redis version | Supported |
|:-------------:|:---------:|
|     1.x.x     | &#x2716;	 |
|     2.x.x     | &#x2716;	 |
|     3.x.x     | &#x2716;	 |
|     4.x.x     | &#x2716;	 |
|     5.x.x     | &#x2716;	 |
|     6.x.x     | &#x2714;  |
|     7.x.x     | &#x2714;  |


## Implementing RedisBungee in your plugin: [![RedisBungee Build](https://github.com/proxiodev/RedisBungee/actions/workflows/maven.yml/badge.svg)](https://github.com/Limework/RedisBungee/actions/workflows/maven.yml) [![](https://jitpack.io/v/limework/redisbungee.svg)](https://jitpack.io/#limework/redisbungee)

RedisBungee is distributed as a [maven](https://maven.apache.org) project.

first, install it to your maven local repo as we don't have public maven repo.
```
git clone https://github.com/ProxioDev/RedisBungee.git
cd RedisBungee
mvn clean install
```
then to import for bungeecord use:
```
<dependency>
        <groupId>com.imaginarycode.minecraft</groupId>
        <artifactId>RedisBungee-Bungee</artifactId>
        <version>VERSION</version>
        <scope>provided</scope>
</dependency>
```
Second method by using jitpack [![](https://jitpack.io/v/limework/redisbungee.svg)](https://jitpack.io/#limework/redisbungee)

first, add this repository
```
	<repositories>
		<repository>
		    <id>jitpack.io</id>
		    <url>https://jitpack.io</url>
		</repository>
	</repositories>
```
then add this in your dependencies
```
	<dependency>
	    <groupId>com.github.limework.redisbungee</groupId>
	    <artifactId>RedisBungee-Bungee</artifactId>
	    <version>VERSION</version>
	    <scope>provided</scope>
	</dependency>
	
```
## Javadocs

https://proxiodev.github.io/RedisBungee-JavaDocs/0.7.2-SNAPSHOT

## Configuration

**REDISBUNGEE REQUIRES A REDIS SERVER**, preferably with reasonably low latency. The default [config](https://github.com/ProxioDev/RedisBungee/blob/develop/RedisBungee-API/src/main/resources/example_config.yml) is saved when the plugin first starts.

## License!

This project is distributed under Eclipse Public License 1.0

You can find it [here](https://github.com/proxiodev/RedisBungee/blob/master/LICENSE)

You can find the original RedisBungee by Tux [here](https://github.com/minecrafter/RedisBungee) or spigot page [here](https://www.spigotmc.org/resources/redisbungee.13494/)

## Support

You can join our matrix room [here](https://matrix.to/#/!zhedzmRNSZXfuOPZUB:govindas.net?via=govindas.net&via=matrix.org)

![icon](https://matrix.org/images/matrix-logo-white.svg)


## YourKit

YourKit supports open source projects with innovative and intelligent tools for monitoring and profiling Java and .NET applications. YourKit is the creator of [YourKit Java Profiler](https://www.yourkit.com/java/profiler/), [YourKit .NET Profiler](https://www.yourkit.com/.net/profiler/) and [YourKit YouMonitor](https://www.yourkit.com/youmonitor/).

![YourKit](https://www.yourkit.com/images/yklogo.png)
