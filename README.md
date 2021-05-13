# Limework fork of RedisBungee

Spigot link: [click](https://www.spigotmc.org/resources/redisbungee.87700/)

this fork was made due the maintainer of Redis Bungee became inactive, so we took the place to develop it!

RedisBungee bridges [Redis](http://redis.io) and BungeeCord together. 

RedisBungee was used on thechunk server which we think was shutdown due website not loading...
~~This is the solution deployed on [The Chunk](http://thechunk.net) to make sure our multi-Bungee setup flows smoothly together.~~

This currently deployed on [Govindas limework!](https://Limework.net) 

## Compiling

Now you can use maven without installing it using Maven wrapper [github?](https://github.com/takari/maven-wrapper) :)

RedisBungee is distributed as a [maven](http://maven.apache.org) project. To compile it and install it in your local Maven repository:

    git clone https://github.com/Limework/RedisBungee.git
    cd RedisBungee
    mvnw clean install
And also sorry we dont have maven repo at the momment due complicated setup to deal with.
as now we are trying to setup one soon as possible 


## Javadocs
This Java docs hosted on limework website! currently this version is 0.6.0 (note: any version 0.6.* wont have any api changes unless if we change our mind.)
https://limework.net/JavaDocs/RedisBungee/

## Configuration

**REDISBUNGEE REQUIRES A REDIS SERVER**, preferably with reasonably low latency. The default [config](https://github.com/minecrafter/RedisBungee/blob/master/src/main/resources/example_config.yml) is saved when the plugin first starts.

## License!

This project is distributed under Eclipse Public License 1.0

which you can find it [here](https://github.com/Limework/RedisBungee/blob/master/LICENSE)

you can find the original redisBungee by minecrafter [here](https://github.com/minecrafter/RedisBungee) or spigot page [here](https://www.spigotmc.org/resources/redisbungee.13494/)
