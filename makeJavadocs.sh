rm -rf javadoc
mkdir javadoc
mvn clean install javadoc:javadoc -pl RedisBungee-API,RedisBungee-Bungee,RedisBungee-Velocity -am