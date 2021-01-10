# Limework fork of RedisBungee

this fork was made due the maintainer of redisBungee became inactive, so we took the place to develop it!

RedisBungee bridges [Redis](http://redis.io) and BungeeCord together. 

RedisBungee was used on thechunk server which we think was shutdown due website not loading...
~~This is the solution deployed on [The Chunk](http://thechunk.net) to make sure our multi-Bungee setup flows smoothly together.~~

this will be deployed soon on [Govindas limework!](https://Limework.net) 

## Compiling

Now you can use maven without installing it using Maven wrapper [github?](https://github.com/takari/maven-wrapper) :)

RedisBungee is distributed as a [maven](http://maven.apache.org) project. To compile it and install it in your local Maven repository:

    git clone https://github.com/Limework/RedisBungee.git
    cd RedisBungee
    mvnw clean install

## Javadocs
The current version is 0.6-snapshot! will be updated every version number change!

https://limework.net/JavaDocs/RedisBungee/

## Configuration

**REDISBUNGEE REQUIRES A REDIS SERVER**, preferably with reasonably low latency. The default [config](https://github.com/minecrafter/RedisBungee/blob/master/src/main/resources/example_config.yml) is saved when the plugin first starts.

## License!

This project is distributed under Eclipse Public License 1.0

which you can find it [here](https://github.com/Limework/RedisBungee/blob/master/LICENSE)

you can find the original redisBungee by minecrafter [here](https://github.com/minecrafter/RedisBungee) or spigot page [here](https://www.spigotmc.org/resources/redisbungee.13494/)
