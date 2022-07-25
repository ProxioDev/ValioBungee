package com.imaginarycode.minecraft.redisbungee.api.config;


import com.google.common.reflect.TypeToken;
import com.imaginarycode.minecraft.redisbungee.api.RedisBungeeMode;
import com.imaginarycode.minecraft.redisbungee.api.RedisBungeePlugin;
import com.imaginarycode.minecraft.redisbungee.api.summoners.ClusterJedisSummoner;
import com.imaginarycode.minecraft.redisbungee.api.summoners.JedisSummoner;
import com.imaginarycode.minecraft.redisbungee.api.summoners.Summoner;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

public interface ConfigLoader {

    default void loadConfig(RedisBungeePlugin<?> plugin, File dataFolder) throws IOException {
        loadConfig(plugin, dataFolder.toPath());
    }

    default void loadConfig(RedisBungeePlugin<?> plugin, Path dataFolder) throws IOException {
        Path configFile = createConfigFile(dataFolder);
        final YAMLConfigurationLoader yamlConfigurationFileLoader = YAMLConfigurationLoader.builder().setPath(configFile).build();
        ConfigurationNode node = yamlConfigurationFileLoader.load();
        if (node.getNode("config-version").getInt(0) != RedisBungeeConfiguration.CONFIG_VERSION) {
            handleOldConfig(dataFolder);
            node = yamlConfigurationFileLoader.load();
        }
        final boolean useSSL = node.getNode("useSSL").getBoolean(false);
        final boolean overrideBungeeCommands = node.getNode("override-bungee-commands").getBoolean(false);
        final boolean registerLegacyCommands = node.getNode("register-legacy-commands").getBoolean(false);
        String redisPassword = node.getNode("redis-password").getString(null);
        String proxyId = node.getNode("proxy-id").getString("test-1");
        final int maxConnections = node.getNode("max-redis-connections").getInt(10);
        List<String> exemptAddresses;
        try {
            exemptAddresses = node.getNode("exempt-ip-addresses").getList(TypeToken.of(String.class));
        } catch (ObjectMappingException e) {
            exemptAddresses = Collections.emptyList();
        }

        // check redis password
        if (redisPassword != null && (redisPassword.isEmpty() || redisPassword.equals("none"))) {
            redisPassword = null;
            plugin.logWarn("INSECURE setup was detected Please set password for your redis instance.");
        } else if (redisPassword == null) {
            plugin.logWarn("INSECURE setup was detected Please set password for your redis instance.");
        }
        if (!useSSL) {
            plugin.logWarn("INSECURE setup was detected Please setup ssl for your redis instance.");
        }
        // Configuration sanity checks.
        if (proxyId == null || proxyId.isEmpty()) {
            String genId = UUID.randomUUID().toString();
            plugin.logInfo("Generated proxy id " + genId + " and saving it to config.");
            node.getNode("proxy-id").setValue(genId);
            yamlConfigurationFileLoader.save(node);
            proxyId = genId;
            plugin.logInfo("proxy id was generated: " + proxyId);
        } else {
            plugin.logInfo("Loaded proxy id " + proxyId);
        }
        RedisBungeeConfiguration configuration = new RedisBungeeConfiguration(proxyId, exemptAddresses, registerLegacyCommands, overrideBungeeCommands);
        Summoner<?> summoner;
        RedisBungeeMode redisBungeeMode;
        if (node.getNode("cluster-mode-enabled").getBoolean(false)) {
            plugin.logInfo("RedisBungee MODE: CLUSTER");
            Set<HostAndPort> hostAndPortSet = new HashSet<>();
            GenericObjectPoolConfig<Connection> poolConfig = new GenericObjectPoolConfig<>();
            poolConfig.setMaxTotal(maxConnections);
            node.getNode("redis-cluster-servers").getChildrenList().forEach((childNode) -> {
                Map<Object, ? extends ConfigurationNode> hostAndPort = childNode.getChildrenMap();
                String host = hostAndPort.get("host").getString();
                int port = hostAndPort.get("port").getInt();
                hostAndPortSet.add(new HostAndPort(host, port));
            });
            plugin.logInfo(hostAndPortSet.size() + " cluster nodes were specified");
            if (hostAndPortSet.isEmpty()) {
                throw new RuntimeException("No redis cluster servers specified");
            }
            if (redisPassword != null) {
                summoner = new ClusterJedisSummoner(new JedisCluster(hostAndPortSet, 5000, 5000, 60, proxyId, redisPassword, poolConfig, useSSL));
            } else {
                plugin.logWarn("SSL option is ignored in Cluster mode if no PASSWORD is set");
                summoner = new ClusterJedisSummoner(new JedisCluster(hostAndPortSet, 5000, 5000, 60, poolConfig));
            }
            redisBungeeMode = RedisBungeeMode.CLUSTER;
        } else {
            plugin.logInfo("RedisBungee MODE: SINGLE");
            final String redisServer = node.getNode("redis-server").getString("127.0.0.1");
            final int redisPort = node.getNode("redis-port").getInt(6379);
            if (redisServer != null && redisServer.isEmpty()) {
                throw new RuntimeException("No redis server specified");
            }
            JedisPoolConfig config = new JedisPoolConfig();
            config.setMaxTotal(maxConnections);
            summoner = new JedisSummoner(new JedisPool(config, redisServer, redisPort, 0, redisPassword, useSSL));
            redisBungeeMode = RedisBungeeMode.SINGLE;
        }
        plugin.logInfo("Successfully connected to Redis.");
        onConfigLoad(configuration, summoner, redisBungeeMode);
    }

    void onConfigLoad(RedisBungeeConfiguration configuration, Summoner<?> summoner, RedisBungeeMode mode);

    default Path createConfigFile(Path dataFolder) throws IOException {
        if (Files.notExists(dataFolder)) {
            Files.createDirectory(dataFolder);
        }
        Path file = dataFolder.resolve("config.yml");
        if (Files.notExists(file)) {
            try (InputStream in = getClass().getClassLoader().getResourceAsStream("config.yml")) {
                Files.createFile(file);
                assert in != null;
                Files.copy(in, file, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        return file;
    }

    default void handleOldConfig(Path dataFolder) throws IOException {
        Path oldConfigFolder = dataFolder.resolve("old_config");
        if (Files.notExists(oldConfigFolder)) {
            Files.createDirectory(oldConfigFolder);
        }
        Path oldConfigPath = dataFolder.resolve("config.yml");
        Files.move(oldConfigPath, oldConfigFolder.resolve(UUID.randomUUID() + "_config.yml"));
        createConfigFile(dataFolder);

    }


}
