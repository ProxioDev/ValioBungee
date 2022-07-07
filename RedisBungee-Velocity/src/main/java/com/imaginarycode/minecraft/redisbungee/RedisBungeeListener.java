package com.imaginarycode.minecraft.redisbungee;

import com.imaginarycode.minecraft.redisbungee.internal.AbstractRedisBungeeListener;
import com.imaginarycode.minecraft.redisbungee.internal.DataManager;
import com.imaginarycode.minecraft.redisbungee.internal.RedisBungeePlugin;
import com.imaginarycode.minecraft.redisbungee.events.PubSubMessageEvent;
import com.imaginarycode.minecraft.redisbungee.internal.RedisUtil;
import com.imaginarycode.minecraft.redisbungee.internal.util.RedisCallable;
import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.ServerPing;
import net.kyori.adventure.text.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

import java.net.InetAddress;
import java.util.*;

public class RedisBungeeListener extends AbstractRedisBungeeListener<LoginEvent, PostLoginEvent, DisconnectEvent, ServerConnectedEvent, ProxyPingEvent, PluginMessageEvent, PubSubMessageEvent> {


    public RedisBungeeListener(RedisBungeePlugin<?> plugin, List<InetAddress> exemptAddresses) {
        super(plugin, exemptAddresses);
    }

    @Subscribe
    public void onLogin(LoginEvent event, Continuation continuation) {
        plugin.executeAsync(new RedisCallable<Void>(plugin) {
            @Override
            protected Void call(Jedis jedis) {
                try {
                    if (!event.getResult().isAllowed()) {
                        return null;
                    }

                    // We make sure they aren't trying to use an existing player's name.
                    // This is problematic for online-mode servers as they always disconnect old clients.
                    if (plugin.isOnlineMode()) {
                        Player player = (Player) plugin.getPlayer(event.getPlayer().getUsername());

                        if (player != null) {
                            event.setResult(ResultedEvent.ComponentResult.denied(Component.text(ONLINE_MODE_RECONNECT)));
                            return null;
                        }
                    }

                    for (String s : plugin.getServerIds()) {
                        if (jedis.sismember("proxy:" + s + ":usersOnline", event.getPlayer().getUniqueId().toString())) {
                            event.setResult(ResultedEvent.ComponentResult.denied(Component.text(ALREADY_LOGGED_IN)));
                            return null;
                        }
                    }
                    return null;
                } finally {
                    continuation.resume();
                }
            }
        });
    }

    @Override
    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        plugin.executeAsync(new RedisCallable<Void>(plugin) {
            @Override
            protected Void call(Jedis jedis) {
                // this code was moved out from login event due being async..
                // and it can be cancelled but it will show as false in redis-bungee
                // which will register the player into the redis database.
                Pipeline pipeline = jedis.pipelined();
                plugin.getUuidTranslator().persistInfo(event.getPlayer().getUsername(), event.getPlayer().getUniqueId(), pipeline);
                RBUtils.createPlayer(event.getPlayer(), pipeline, false);
                pipeline.sync();
                // the end of moved code.

                jedis.publish("redisbungee-data", gson.toJson(new DataManager.DataManagerMessage<>(
                        event.getPlayer().getUniqueId(), plugin.getApi().getServerId(), DataManager.DataManagerMessage.Action.JOIN,
                        new DataManager.LoginPayload(event.getPlayer().getRemoteAddress().getAddress()))));
                return null;
            }
        });
    }

    @Override
    @Subscribe
    public void onPlayerDisconnect(DisconnectEvent event) {
        plugin.executeAsync(new RedisCallable<Void>(plugin) {
            @Override
            protected Void call(Jedis jedis) {
                Pipeline pipeline = jedis.pipelined();
                RedisUtil.cleanUpPlayer(event.getPlayer().getUniqueId().toString(), pipeline);
                pipeline.sync();
                return null;
            }
        });

    }

    @Override
    @Subscribe
    public void onServerChange(ServerConnectedEvent event) {
        Optional<ServerConnection> optionalServerConnection = event.getPlayer().getCurrentServer();
        final String currentServer = optionalServerConnection.map(serverConnection -> serverConnection.getServerInfo().getName()).orElse(null);
        plugin.executeAsync(new RedisCallable<Void>(plugin) {
            @Override
            protected Void call(Jedis jedis) {
                jedis.hset("player:" + event.getPlayer().getUniqueId().toString(), "server", event.getServer().getServerInfo().getName());
                jedis.publish("redisbungee-data", gson.toJson(new DataManager.DataManagerMessage<>(
                        event.getPlayer().getUniqueId(), plugin.getApi().getServerId(), DataManager.DataManagerMessage.Action.SERVER_CHANGE,
                        new DataManager.ServerChangePayload(event.getServer().getServerInfo().getName(), currentServer))));
                return null;
            }
        });
    }

    @Override
    @Subscribe(order = PostOrder.EARLY)
    public void onPing(ProxyPingEvent event) {
        if (exemptAddresses.contains(event.getConnection().getRemoteAddress().getAddress())) {
            return;
        }
        ServerPing oldPing = event.getPing();
        int max = oldPing.getPlayers().map(ServerPing.Players::getMax).orElse(0);
        List<ServerPing.SamplePlayer> list = oldPing.getPlayers().map(ServerPing.Players::getSample).orElse(Collections.emptyList());
        event.setPing(new ServerPing(oldPing.getVersion(), new ServerPing.Players(plugin.getCount(), max, list), oldPing.getDescriptionComponent(), oldPing.getFavicon().orElse(null)));
    }

    @Override
    public void onPluginMessage(PluginMessageEvent event) {
        /*
         * Ham1255 note: for some reason plugin messages were not working in velocity?
         * not sure how to fix, but for now i have removed the code until a fix is made.
         *
         */
    }


    @Override
    @Subscribe
    public void onPubSubMessage(PubSubMessageEvent event) {
        if (event.getChannel().equals("redisbungee-allservers") || event.getChannel().equals("redisbungee-" + plugin.getApi().getServerId())) {
            String message = event.getMessage();
            if (message.startsWith("/"))
                message = message.substring(1);
            plugin.logInfo("Invoking command via PubSub: /" + message);
            ((RedisBungeeVelocityPlugin)plugin).getProxy().getCommandManager().executeAsync(RedisBungeeCommandSource.getSingleton(), message);//.dispatchCommand(RedisBungeeCommandSource.getSingleton(), message);

        }
    }
}
