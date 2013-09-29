# RedisBungee

RedisBungee bridges [Redis](http://redis.org) and BungeeCord together. This is the solution deployed on [The Chunk](http://thechunk.net) to make sure our multi-Bungee setup flows smoothly together.

## Compiling

RedisBungee is distributed as a [maven](http://maven.apache.org) project. To compile it:

    git clone https://github.com/minecrafter/RedisBungee.git
    cd RedisBungee
    mvn clean package

## Configuration

**REDISBUNGEE REQUIRES A REDIS SERVER**, preferably with reasonably low latency. The default [config](https://github.com/minecrafter/RedisBungee/blob/master/src/main/resources/example_config.yml) is saved when the plugin first starts.
