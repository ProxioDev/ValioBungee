# RedisBungee fork By Limework

Spigot link: [click](https://www.spigotmc.org/resources/redisbungee.87700/)

The main project of RedisBungee is no longer maintained, so we have forked the plugin.

*if you are here for transferring players to another proxy when the first proxy crashes or whatever this plugin won't do it, tell mojang to implement transfer packet*

RedisBungee uses [Redis](https://redis.io) to Synchronize players data between [BungeeCord](https://github.com/SpigotMC/BungeeCord) or [Velocity*](https://github.com/PaperMC/Velocity) proxies

Velocity*: *version 3.1.2 or above is only supported, any version below that might work but might be unstable* [#40](https://github.com/ProxioDev/RedisBungee/pull/40)

If you are looking to use Original RedisBungee without a change to internals,
with critical bugs fixed, please use version [0.6.5](https://github.com/ProxioDev/RedisBungee/releases/tag/0.6.5) 
as its last version before internal changes.


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

By using jitpack [![](https://jitpack.io/v/limework/redisbungee.svg)](https://jitpack.io/#limework/redisbungee)

## Setup jitpack repository
```
	<repositories>
		<repository>
		    <id>jitpack.io</id>
		    <url>https://jitpack.io</url>
		</repository>
	</repositories>
```
## [BungeeCord](https://github.com/SpigotMC/BungeeCord)
add this in your project dependencies 
```
	<dependency>
	    <groupId>com.github.limework.redisbungee</groupId>
	    <artifactId>RedisBungee-Bungee</artifactId>
	    <version>VERSION</version>
	    <scope>provided</scope>
	</dependency>
	
```
then in your project plugin.yml add `RedisBungee` to `depends` like this
```
name: "yourplugin"
main: your.main.class
version: 1.0.0-SNAPSHOT
author: idk
depends: [ RedisBungee ]
```


## [Velocity](https://github.com/PaperMC/Velocity)
```
	<dependency>
	    <groupId>com.github.limework.redisbungee</groupId>
	    <artifactId>RedisBungee-Velocity</artifactId>
	    <version>VERSION</version>
	    <scope>provided</scope>
	</dependency>
```
then to make your plugin depends on RedisBungee, make sure your plugin class Annotation have `@Dependency(id = "redisbungee")` like this
```
@Plugin(
  id = "myplugin",
  name = "My Plugin",
  version = "0.1.0-beta",
  dependencies = {
    @Dependency(id = "redisbungee")
  }
)
public class PluginMainClass {

}
```
## Getting the latest commits to your code
If you want to use the latest commits without waiting for releases.
first, install it to your maven local repo
```
git clone https://github.com/ProxioDev/RedisBungee.git
cd RedisBungee
mvn clean install
```
then use any of these in your project.
```
<dependency>
        <groupId>com.imaginarycode.minecraft</groupId>
        <artifactId>RedisBungee-Bungee</artifactId>
        <version>VERSION</version>
        <scope>provided</scope>
</dependency>
```
```
<dependency>
        <groupId>com.imaginarycode.minecraft</groupId>
        <artifactId>RedisBungee-Velocity</artifactId>
        <version>VERSION</version>
        <scope>provided</scope>
</dependency>
```
## Javadocs

For current version [0.8.0](https://proxiodev.github.io/RedisBungee-JavaDocs/0.8.0-SNAPSHOT/) 

For legacy Version [0.6.5](https://proxiodev.github.io/RedisBungee-JavaDocs/0.6.5-SNAPSHOT/)



## Configuration

**REDISBUNGEE REQUIRES A REDIS SERVER**, preferably with reasonably low latency. The default [config](https://github.com/ProxioDev/RedisBungee/blob/develop/RedisBungee-API/src/main/resources/example_config.yml) is saved when the plugin first starts.

## License!

This project is distributed under Eclipse Public License 1.0

You can find it [here](https://github.com/proxiodev/RedisBungee/blob/master/LICENSE)

You can find the original RedisBungee by Tux [here](https://github.com/minecrafter/RedisBungee) or spigot page [here](https://www.spigotmc.org/resources/redisbungee.13494/) "**NO LONGER WORKS**"

## Support

You can join our matrix room [here](https://matrix.to/#/!zhedzmRNSZXfuOPZUB:govindas.net?via=govindas.net&via=matrix.org)

![icon](https://matrix.org/images/matrix-logo-white.svg)


## YourKit

YourKit supports open source projects with innovative and intelligent tools for monitoring and profiling Java and .NET applications. YourKit is the creator of [YourKit Java Profiler](https://www.yourkit.com/java/profiler/), [YourKit .NET Profiler](https://www.yourkit.com/.net/profiler/) and [YourKit YouMonitor](https://www.yourkit.com/youmonitor/).

![YourKit](https://www.yourkit.com/images/yklogo.png)
