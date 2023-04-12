/*
 * Copyright (c) 2013-present RedisBungee contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *
 *  http://www.eclipse.org/legal/epl-v10.html
 */

package com.imaginarycode.minecraft.redisbungee.api.config;


import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;
import com.imaginarycode.minecraft.redisbungee.api.RedisBungeeMode;
import com.imaginarycode.minecraft.redisbungee.api.RedisBungeePlugin;
import com.imaginarycode.minecraft.redisbungee.api.summoners.JedisClusterSummoner;
import com.imaginarycode.minecraft.redisbungee.api.summoners.JedisPooledSummoner;
import com.imaginarycode.minecraft.redisbungee.api.summoners.Summoner;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.*;
import redis.clients.jedis.providers.ClusterConnectionProvider;
import redis.clients.jedis.providers.PooledConnectionProvider;

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
        final boolean restoreOldKickBehavior = node.getNode("disable-kick-when-online").getBoolean(false);
        String redisPassword = node.getNode("redis-password").getString("");
        String redisUsername = node.getNode("redis-username").getString("");
        String proxyId = node.getNode("proxy-id").getString("test-1");
        final int maxConnections = node.getNode("max-redis-connections").getInt(10);
        List<String> exemptAddresses;
        try {
            exemptAddresses = node.getNode("exempt-ip-addresses").getList(TypeToken.of(String.class));
        } catch (ObjectMappingException e) {
            exemptAddresses = Collections.emptyList();
        }

        // check redis password
        if ((redisPassword.isEmpty() || redisPassword.equals("none"))) {
            redisPassword = null;
            plugin.logWarn("password is empty");
        }
        if ((redisUsername.isEmpty() || redisUsername.equals("none"))) {
            redisUsername = null;
        }

        if (useSSL) {
            plugin.logInfo("Using ssl");
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
        RedisBungeeConfiguration configuration = new RedisBungeeConfiguration(proxyId, exemptAddresses, registerLegacyCommands, overrideBungeeCommands, getMessagesFromPath(createMessagesFile(dataFolder)), restoreOldKickBehavior);
        Summoner<?> summoner;
        RedisBungeeMode redisBungeeMode;
        if (node.getNode("cluster-mode-enabled").getBoolean(false)) {
            plugin.logInfo("RedisBungee MODE: CLUSTER");
            Set<HostAndPort> hostAndPortSet = new HashSet<>();
            GenericObjectPoolConfig<Connection> poolConfig = new GenericObjectPoolConfig<>();
            poolConfig.setMaxTotal(maxConnections);
            poolConfig.setBlockWhenExhausted(true);
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
            summoner = new JedisClusterSummoner(new ClusterConnectionProvider(hostAndPortSet, DefaultJedisClientConfig.builder().user(redisUsername).password(redisPassword).ssl(useSSL).socketTimeoutMillis(5000).timeoutMillis(10000).build(), poolConfig));
            redisBungeeMode = RedisBungeeMode.CLUSTER;
        } else {
            plugin.logInfo("RedisBungee MODE: SINGLE");
            final String redisServer = node.getNode("redis-server").getString("127.0.0.1");
            final int redisPort = node.getNode("redis-port").getInt(6379);
            if (redisServer != null && redisServer.isEmpty()) {
                throw new RuntimeException("No redis server specified");
            }
            JedisPool jedisPool = null;
            if (node.getNode("enable-jedis-pool-compatibility").getBoolean(true)) {
                JedisPoolConfig config = new JedisPoolConfig();
                config.setMaxTotal(node.getNode("compatibility-max-connections").getInt(3));
                config.setBlockWhenExhausted(true);
                jedisPool = new JedisPool(config, redisServer, redisPort, 5000, redisUsername, redisPassword, useSSL);
                plugin.logInfo("Compatibility JedisPool was created");
            }
            GenericObjectPoolConfig<Connection> poolConfig = new GenericObjectPoolConfig<>();
            poolConfig.setMaxTotal(maxConnections);
            poolConfig.setBlockWhenExhausted(true);
            summoner = new JedisPooledSummoner(new PooledConnectionProvider(new ConnectionFactory(new HostAndPort(redisServer, redisPort), DefaultJedisClientConfig.builder().user(redisUsername).timeoutMillis(5000).ssl(useSSL).password(redisPassword).build()), poolConfig), jedisPool);
            redisBungeeMode = RedisBungeeMode.SINGLE;
        }
        plugin.logInfo("Successfully connected to Redis.");
        onConfigLoad(configuration, summoner, redisBungeeMode);
    }

    void onConfigLoad(RedisBungeeConfiguration configuration, Summoner<?> summoner, RedisBungeeMode mode);

    default ImmutableMap<RedisBungeeConfiguration.MessageType, String> getMessagesFromPath(Path path) throws IOException {
        final YAMLConfigurationLoader yamlConfigurationFileLoader = YAMLConfigurationLoader.builder().setPath(path).build();
        ConfigurationNode node = yamlConfigurationFileLoader.load();
        HashMap<RedisBungeeConfiguration.MessageType, String> messages = new HashMap<>();
        messages.put(RedisBungeeConfiguration.MessageType.LOGGED_IN_OTHER_LOCATION, node.getNode("logged-in-other-location").getString("§cLogged in from another location."));
        messages.put(RedisBungeeConfiguration.MessageType.ALREADY_LOGGED_IN, node.getNode("already-logged-in").getString("§cYou are already logged in!"));
        return ImmutableMap.copyOf(messages);
    }

    default Path createMessagesFile(Path dataFolder) throws IOException {
        if (Files.notExists(dataFolder)) {
            Files.createDirectory(dataFolder);
        }
        Path file = dataFolder.resolve("messages.yml");
        if (Files.notExists(file)) {
            try (InputStream in = getClass().getClassLoader().getResourceAsStream("messages.yml")) {
                Files.createFile(file);
                assert in != null;
                Files.copy(in, file, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        return file;
    }

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
