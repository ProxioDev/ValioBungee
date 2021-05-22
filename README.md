# Limework fork of RedisBungee

[![RedisBungee Build](https://github.com/Limework/RedisBungee/actions/workflows/maven.yml/badge.svg)](https://github.com/Limework/RedisBungee/actions/workflows/maven.yml) [![](https://jitpack.io/v/limework/redisbungee.svg)](https://jitpack.io/#limework/redisbungee)

Spigot link: [click](https://www.spigotmc.org/resources/redisbungee.87700/)

The maintainer of RedisBungee has became inactive, so we have taken the development of the plugin.

RedisBungee bridges [Redis](https://redis.io) and [BungeeCord](https://github.com/SpigotMC/BungeeCord) together. 


This is currently deployed on [Govindas Limework!](https://Limework.net) 

## Compiling

Now you can use maven without installing it using Maven wrapper [github?](https://github.com/takari/maven-wrapper) :)

RedisBungee is distributed as a [maven](https://maven.apache.org) project. To compile it and install it in your local Maven repository:
for latest commits you can use this way.
    
    git clone https://github.com/Limework/RedisBungee.git
    cd RedisBungee
    mvnw clean install

And use it in your pom file.

    <dependency>
      <groupId>com.imaginarycode.minecraft</groupId>
      <artifactId>RedisBungee</artifactId>
      <version>0.6.3</version>
      <scope>provided</scope>
    </dependency>

Or if you want to use the jitpack maven server

    <repositories>
		<repository>
		    <id>jitpack.io</id>
		    <url>https://jitpack.io</url>
		</repository>
	</repositories>
    
And use it in your pom file.
    
    <dependency>
	    <groupId>com.github.limework</groupId>
	    <artifactId>redisbungee</artifactId>
	    <version>0.6.3</version>
	</dependency>


## Javadocs
Hosted on limework website. Version 0.6.0 (note: any version 0.6.* will not have API changes.)
https://limework.net/JavaDocs/RedisBungee/

## Configuration

**REDISBUNGEE REQUIRES A REDIS SERVER**, preferably with reasonably low latency. The default [config](https://github.com/limework/RedisBungee/blob/master/src/main/resources/example_config.yml) is saved when the plugin first starts.

## License!

This project is distributed under Eclipse Public License 1.0

You can find it [here](https://github.com/Limework/RedisBungee/blob/master/LICENSE)

You can find the original RedisBungee by minecrafter [here](https://github.com/minecrafter/RedisBungee) or spigot page [here](https://www.spigotmc.org/resources/redisbungee.13494/)
