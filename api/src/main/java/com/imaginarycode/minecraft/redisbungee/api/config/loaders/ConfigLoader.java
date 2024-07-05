/*
 * Copyright (c) 2013-present RedisBungee contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *
 *  http://www.eclipse.org/legal/epl-v10.html
 */

package com.imaginarycode.minecraft.redisbungee.api.config.loaders;


import com.google.common.reflect.TypeToken;
import com.imaginarycode.minecraft.redisbungee.api.RedisBungeeMode;
import com.imaginarycode.minecraft.redisbungee.api.RedisBungeePlugin;
import com.imaginarycode.minecraft.redisbungee.api.config.HandleMotdOrder;
import com.imaginarycode.minecraft.redisbungee.api.config.RedisBungeeConfiguration;
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

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public interface ConfigLoader extends GenericConfigLoader {

    int CONFIG_VERSION = 2;

    default void loadConfig(RedisBungeePlugin<?> plugin, Path dataFolder) throws IOException {
        Path configFile = createConfigFile(dataFolder, "config.yml", "config.yml");
        final YAMLConfigurationLoader yamlConfigurationFileLoader = YAMLConfigurationLoader.builder().setPath(configFile).build();
        ConfigurationNode node = yamlConfigurationFileLoader.load();
        if (node.getNode("config-version").getInt(0) != CONFIG_VERSION) {
            handleOldConfig(dataFolder, "config.yml", "config.yml");
            node = yamlConfigurationFileLoader.load();
        }
        final boolean useSSL = node.getNode("useSSL").getBoolean(false);
        final boolean kickWhenOnline = node.getNode("kick-when-online").getBoolean(true);
        String redisPassword = node.getNode("redis-password").getString("");
        String redisUsername = node.getNode("redis-username").getString("");
        String networkId = node.getNode("network-id").getString("main");
        String proxyId = node.getNode("proxy-id").getString("proxy-1");

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
        // env var
        String proxyIdFromEnv = System.getenv("REDISBUNGEE_PROXY_ID");
        if (proxyIdFromEnv != null) {
            plugin.logInfo("Overriding current configured proxy id {} and been set to {} by Environment variable REDISBUNGEE_PROXY_ID", proxyId, proxyIdFromEnv);
            proxyId = proxyIdFromEnv;
        }

        String networkIdFromEnv = System.getenv("REDISBUNGEE_NETWORK_ID");
        if (networkIdFromEnv != null) {
            plugin.logInfo("Overriding current configured network id {} and been set to {} by Environment variable REDISBUNGEE_NETWORK_ID", networkId, networkIdFromEnv);
            networkId = networkIdFromEnv;
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

        if (networkId.isEmpty()) {
            networkId = "main";
            plugin.logWarn("network id was empty and replaced with 'main'");
        }

        plugin.logInfo("Loaded network id " + networkId);



        boolean reconnectToLastServer = node.getNode("reconnect-to-last-server").getBoolean();
        boolean handleMotd = node.getNode("handle-motd").getBoolean(true);
        plugin.logInfo("handle reconnect to last server: {}", reconnectToLastServer);
        plugin.logInfo("handle motd: {}", handleMotd);

        HandleMotdOrder handleMotdOrder = HandleMotdOrder.NORMAL;
        String handleMotdOrderName = node.getNode("handle-motd-priority").getString();
        if (handleMotdOrderName != null) {
            try {
                handleMotdOrder = HandleMotdOrder.valueOf(handleMotdOrderName.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                plugin.logWarn("handle motd order value '{}' is unsupported (allowed: {})", handleMotdOrderName, HandleMotdOrder.values());
            }
        }
        plugin.logInfo("handle motd order: {}", handleMotdOrder);

        // commands
        boolean redisBungeeEnabled = node.getNode("commands", "redisbungee", "enabled").getBoolean(true);
        boolean redisBungeeLegacyEnabled =node.getNode("commands", "redisbungee-legacy", "enabled").getBoolean(false);

        boolean glistEnabled = node.getNode("commands", "redisbungee-legacy", "subcommands", "glist", "enabled").getBoolean(false);
        boolean findEnabled = node.getNode("commands", "redisbungee-legacy", "subcommands", "find", "enabled").getBoolean(false);
        boolean lastseenEnabled = node.getNode("commands", "redisbungee-legacy", "subcommands", "lastseen", "enabled").getBoolean(false);
        boolean ipEnabled = node.getNode("commands", "redisbungee-legacy", "subcommands", "ip", "enabled").getBoolean(false);
        boolean pproxyEnabled = node.getNode("commands", "redisbungee-legacy", "subcommands", "pproxy", "enabled").getBoolean(false);
        boolean sendToAllEnabled = node.getNode("commands", "redisbungee-legacy", "subcommands", "sendtoall", "enabled").getBoolean(false);
        boolean serverIdEnabled = node.getNode("commands", "redisbungee-legacy", "subcommands", "serverid", "enabled").getBoolean(false);
        boolean serverIdsEnabled = node.getNode("commands", "redisbungee-legacy", "subcommands", "serverids", "enabled").getBoolean(false);
        boolean pListEnabled = node.getNode("commands", "redisbungee-legacy", "subcommands", "plist", "enabled").getBoolean(false);

        boolean installGlist = node.getNode("commands", "redisbungee-legacy", "subcommands", "glist", "install").getBoolean(false);
        boolean installFind = node.getNode("commands", "redisbungee-legacy", "subcommands", "find", "install").getBoolean(false);
        boolean installLastseen = node.getNode("commands", "redisbungee-legacy", "subcommands", "lastseen", "install").getBoolean(false);
        boolean installIp = node.getNode("commands", "redisbungee-legacy", "subcommands", "ip", "install").getBoolean(false);
        boolean installPproxy = node.getNode("commands", "redisbungee-legacy", "subcommands", "pproxy", "install").getBoolean(false);
        boolean installSendToAll = node.getNode("commands", "redisbungee-legacy", "subcommands", "sendtoall", "install").getBoolean(false);
        boolean installServerid = node.getNode("commands", "redisbungee-legacy", "subcommands", "serverid", "install").getBoolean(false);
        boolean installServerIds = node.getNode("commands", "redisbungee-legacy", "subcommands", "serverids", "install").getBoolean(false);
        boolean installPlist = node.getNode("commands", "redisbungee-legacy", "subcommands", "plist", "install").getBoolean(false);


        RedisBungeeConfiguration configuration = new RedisBungeeConfiguration(networkId, proxyId, exemptAddresses, kickWhenOnline, reconnectToLastServer, handleMotd, handleMotdOrder,
            new RedisBungeeConfiguration.CommandsConfiguration(
                redisBungeeEnabled, redisBungeeLegacyEnabled,
                new RedisBungeeConfiguration.LegacySubCommandsConfiguration(
                        findEnabled, glistEnabled, ipEnabled,
                        lastseenEnabled, pListEnabled, pproxyEnabled,
                        sendToAllEnabled, serverIdEnabled, serverIdsEnabled,
                        installFind, installGlist, installIp,
                        installLastseen, installPlist, installPproxy,
                        installSendToAll, installServerid, installServerIds)
        ));
        Summoner<?> summoner;
        RedisBungeeMode redisBungeeMode;
        if (useSSL) {
            plugin.logInfo("Using ssl");
        }
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
            if (node.getNode("enable-jedis-pool-compatibility").getBoolean(false)) {
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


}
