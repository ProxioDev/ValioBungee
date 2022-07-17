package com.imaginarycode.minecraft.redisbungee;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.imaginarycode.minecraft.redisbungee.api.AbstractRedisBungeeListener;
import com.imaginarycode.minecraft.redisbungee.api.AbstractDataManager;
import com.imaginarycode.minecraft.redisbungee.api.RedisBungeePlugin;
import com.imaginarycode.minecraft.redisbungee.api.tasks.RedisTask;
import com.imaginarycode.minecraft.redisbungee.events.PubSubMessageEvent;
import com.imaginarycode.minecraft.redisbungee.api.util.RedisUtil;
import net.md_5.bungee.api.AbstractReconnectHandler;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.*;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.Pipeline;

import java.net.InetAddress;
import java.util.*;

public class RedisBungeeBungeeListener extends AbstractRedisBungeeListener<LoginEvent, PostLoginEvent, PlayerDisconnectEvent, ServerConnectedEvent, ProxyPingEvent, PluginMessageEvent, PubSubMessageEvent> implements Listener {


    public RedisBungeeBungeeListener(RedisBungeePlugin<?> plugin, List<InetAddress> exemptAddresses) {
        super(plugin, exemptAddresses);
    }

    @Override
    @EventHandler
    public void onLogin(LoginEvent event) {
        event.registerIntent((Plugin) plugin);
        plugin.executeAsync(new RedisTask<Void>(plugin) {
            @Override
            public Void jedisTask(Jedis jedis) {
                try {
                    if (event.isCancelled()) {
                        return null;
                    }

                    // We make sure they aren't trying to use an existing player's name.
                    // This is problematic for online-mode servers as they always disconnect old clients.
                    if (plugin.isOnlineMode()) {
                        ProxiedPlayer player = (ProxiedPlayer) plugin.getPlayer(event.getConnection().getName());

                        if (player != null) {
                            event.setCancelled(true);
                            // TODO: Make it accept a BaseComponent[] like everything else.
                            event.setCancelReason(ONLINE_MODE_RECONNECT);
                            return null;
                        }
                    }

                    for (String s : plugin.getProxiesIds()) {
                        if (jedis.sismember("proxy:" + s + ":usersOnline", event.getConnection().getUniqueId().toString())) {
                            event.setCancelled(true);
                            // TODO: Make it accept a BaseComponent[] like everything else.
                            event.setCancelReason(ALREADY_LOGGED_IN);
                            return null;
                        }
                    }
                    return null;
                } finally {
                    event.completeIntent((Plugin) plugin);
                }
            }

            @Override
            public Void clusterJedisTask(JedisCluster jedisCluster) {
                try {
                    if (event.isCancelled()) {
                        return null;
                    }

                    // We make sure they aren't trying to use an existing player's name.
                    // This is problematic for online-mode servers as they always disconnect old clients.
                    if (plugin.isOnlineMode()) {
                        ProxiedPlayer player = (ProxiedPlayer) plugin.getPlayer(event.getConnection().getName());

                        if (player != null) {
                            event.setCancelled(true);
                            // TODO: Make it accept a BaseComponent[] like everything else.
                            event.setCancelReason(ONLINE_MODE_RECONNECT);
                            return null;
                        }
                    }

                    for (String s : plugin.getProxiesIds()) {
                        if (jedisCluster.sismember("proxy:" + s + ":usersOnline", event.getConnection().getUniqueId().toString())) {
                            event.setCancelled(true);
                            // TODO: Make it accept a BaseComponent[] like everything else.
                            event.setCancelReason(ALREADY_LOGGED_IN);
                            return null;
                        }
                    }
                    return null;
                } finally {
                    event.completeIntent((Plugin) plugin);
                }
            }
        });
    }

    @Override
    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        plugin.executeAsync(new RedisTask<Void>(plugin) {
            @Override
            public Void jedisTask(Jedis jedis) {
                // this code was moved out from login event due being async..
                // and it can be cancelled but it will show as false in redis-bungee
                // which will register the player into the redis database.
                Pipeline pipeline = jedis.pipelined();
                plugin.getUuidTranslator().persistInfo(event.getPlayer().getName(), event.getPlayer().getUniqueId(), pipeline);
                PlayerUtils.createPlayer(event.getPlayer(), pipeline, false);
                pipeline.sync();
                // the end of moved code.

                jedis.publish("redisbungee-data", gson.toJson(new AbstractDataManager.DataManagerMessage<>(
                        event.getPlayer().getUniqueId(), plugin.getApi().getProxyId(), AbstractDataManager.DataManagerMessage.Action.JOIN,
                        new AbstractDataManager.LoginPayload(event.getPlayer().getAddress().getAddress()))));
                return null;
            }

            @Override
            public Void clusterJedisTask(JedisCluster jedisCluster) {
                // this code was moved out from login event due being async..
                // and it can be cancelled but it will show as false in redis-bungee
                // which will register the player into the redis database.

                // due no support to pipeline in jedis cluster just use the instance instead.
                plugin.getUuidTranslator().persistInfo(event.getPlayer().getName(), event.getPlayer().getUniqueId(), jedisCluster);
                PlayerUtils.createPlayer(event.getPlayer(), jedisCluster, false);
                // the end of moved code.

                jedisCluster.publish("redisbungee-data", gson.toJson(new AbstractDataManager.DataManagerMessage<>(
                        event.getPlayer().getUniqueId(), plugin.getApi().getProxyId(), AbstractDataManager.DataManagerMessage.Action.JOIN,
                        new AbstractDataManager.LoginPayload(event.getPlayer().getAddress().getAddress()))));
                return null;
            }
        });
    }

    @Override
    @EventHandler
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        plugin.executeAsync(new RedisTask<Void>(plugin) {


            @Override
            public Void jedisTask(Jedis jedis) {
                Pipeline pipeline = jedis.pipelined();
                RedisUtil.cleanUpPlayer(event.getPlayer().getUniqueId().toString(), pipeline);
                pipeline.sync();
                return null;
            }

            @Override
            public Void clusterJedisTask(JedisCluster jedisCluster) {
                // Due some reason JedisCluster does not support pipeline, use instance instead
                RedisUtil.cleanUpPlayer(event.getPlayer().getUniqueId().toString(), jedisCluster);
                return null;
            }
        });

    }

    @Override
    @EventHandler
    public void onServerChange(ServerConnectedEvent event) {
        final String currentServer = event.getPlayer().getServer() == null ? null : event.getPlayer().getServer().getInfo().getName();
        plugin.executeAsync(new RedisTask<Void>(plugin) {
            @Override
            public Void jedisTask(Jedis jedis) {
                jedis.hset("player:" + event.getPlayer().getUniqueId().toString(), "server", event.getServer().getInfo().getName());
                jedis.publish("redisbungee-data", gson.toJson(new AbstractDataManager.DataManagerMessage<>(
                        event.getPlayer().getUniqueId(), plugin.getApi().getProxyId(), AbstractDataManager.DataManagerMessage.Action.SERVER_CHANGE,
                        new AbstractDataManager.ServerChangePayload(event.getServer().getInfo().getName(), currentServer))));
                return null;
            }

            @Override
            public Void clusterJedisTask(JedisCluster jedisCluster) {
                jedisCluster.hset("player:" + event.getPlayer().getUniqueId().toString(), "server", event.getServer().getInfo().getName());
                jedisCluster.publish("redisbungee-data", gson.toJson(new AbstractDataManager.DataManagerMessage<>(
                        event.getPlayer().getUniqueId(), plugin.getApi().getProxyId(), AbstractDataManager.DataManagerMessage.Action.SERVER_CHANGE,
                        new AbstractDataManager.ServerChangePayload(event.getServer().getInfo().getName(), currentServer))));
                return null;
            }
        });
    }

    @Override
    @EventHandler
    public void onPing(ProxyPingEvent event) {
        if (exemptAddresses.contains(event.getConnection().getAddress().getAddress())) {
            return;
        }
        ServerInfo forced = AbstractReconnectHandler.getForcedHost(event.getConnection());

        if (forced != null && event.getConnection().getListener().isPingPassthrough()) {
            return;
        }
        event.getResponse().getPlayers().setOnline(plugin.getCount());
    }

    @Override
    @SuppressWarnings("UnstableApiUsage")
    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) {
        if ((event.getTag().equals("legacy:redisbungee") || event.getTag().equals("RedisBungee")) && event.getSender() instanceof Server) {
            final String currentChannel = event.getTag();
            final byte[] data = Arrays.copyOf(event.getData(), event.getData().length);
            plugin.executeAsync(() -> {
                ByteArrayDataInput in = ByteStreams.newDataInput(data);

                String subchannel = in.readUTF();
                ByteArrayDataOutput out = ByteStreams.newDataOutput();
                String type;

                switch (subchannel) {
                    case "PlayerList":
                        out.writeUTF("PlayerList");
                        Set<UUID> original = Collections.emptySet();
                        type = in.readUTF();
                        if (type.equals("ALL")) {
                            out.writeUTF("ALL");
                            original = plugin.getPlayers();
                        } else {
                            try {
                                original = plugin.getApi().getPlayersOnServer(type);
                            } catch (IllegalArgumentException ignored) {
                            }
                        }
                        Set<String> players = new HashSet<>();
                        for (UUID uuid : original)
                            players.add(plugin.getUuidTranslator().getNameFromUuid(uuid, false));
                        out.writeUTF(Joiner.on(',').join(players));
                        break;
                    case "PlayerCount":
                        out.writeUTF("PlayerCount");
                        type = in.readUTF();
                        if (type.equals("ALL")) {
                            out.writeUTF("ALL");
                            out.writeInt(plugin.getCount());
                        } else {
                            out.writeUTF(type);
                            try {
                                out.writeInt(plugin.getApi().getPlayersOnServer(type).size());
                            } catch (IllegalArgumentException e) {
                                out.writeInt(0);
                            }
                        }
                        break;
                    case "LastOnline":
                        String user = in.readUTF();
                        out.writeUTF("LastOnline");
                        out.writeUTF(user);
                        out.writeLong(plugin.getApi().getLastOnline(Objects.requireNonNull(plugin.getUuidTranslator().getTranslatedUuid(user, true))));
                        break;
                    case "ServerPlayers":
                        String type1 = in.readUTF();
                        out.writeUTF("ServerPlayers");
                        Multimap<String, UUID> multimap = plugin.getApi().getServerToPlayers();

                        boolean includesUsers;

                        switch (type1) {
                            case "COUNT":
                                includesUsers = false;
                                break;
                            case "PLAYERS":
                                includesUsers = true;
                                break;
                            default:
                                // TODO: Should I raise an error?
                                return;
                        }

                        out.writeUTF(type1);

                        if (includesUsers) {
                            Multimap<String, String> human = HashMultimap.create();
                            for (Map.Entry<String, UUID> entry : multimap.entries()) {
                                human.put(entry.getKey(), plugin.getUuidTranslator().getNameFromUuid(entry.getValue(), false));
                            }
                            serializeMultimap(human, true, out);
                        } else {
                            serializeMultiset(multimap.keys(), out);
                        }
                        break;
                    case "Proxy":
                        out.writeUTF("Proxy");
                        out.writeUTF(plugin.getConfiguration().getProxyId());
                        break;
                    case "PlayerProxy":
                        String username = in.readUTF();
                        out.writeUTF("PlayerProxy");
                        out.writeUTF(username);
                        out.writeUTF(plugin.getApi().getProxy(Objects.requireNonNull(plugin.getUuidTranslator().getTranslatedUuid(username, true))));
                        break;
                    default:
                        return;
                }

                ((Server) event.getSender()).sendData(currentChannel, out.toByteArray());
            });
        }
    }

    @Override
    @EventHandler
    public void onPubSubMessage(PubSubMessageEvent event) {
        if (event.getChannel().equals("redisbungee-allservers") || event.getChannel().equals("redisbungee-" + plugin.getApi().getProxyId())) {
            String message = event.getMessage();
            if (message.startsWith("/"))
                message = message.substring(1);
            plugin.logInfo("Invoking command via PubSub: /" + message);
            ((Plugin) plugin).getProxy().getPluginManager().dispatchCommand(RedisBungeeCommandSender.getSingleton(), message);
        }
    }
}
