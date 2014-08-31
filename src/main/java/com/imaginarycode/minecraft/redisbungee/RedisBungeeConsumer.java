/**
 * Copyright © 2013 tuxed <write@imaginarycode.com>
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See http://www.wtfpl.net/ for more details.
 */
package com.imaginarycode.minecraft.redisbungee;

import com.imaginarycode.minecraft.redisbungee.consumerevents.ConsumerEvent;
import com.imaginarycode.minecraft.redisbungee.consumerevents.PlayerChangedServerConsumerEvent;
import com.imaginarycode.minecraft.redisbungee.consumerevents.PlayerLoggedInConsumerEvent;
import com.imaginarycode.minecraft.redisbungee.consumerevents.PlayerLoggedOffConsumerEvent;
import lombok.RequiredArgsConstructor;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@RequiredArgsConstructor
public class RedisBungeeConsumer implements Runnable {
    private final RedisBungee plugin;
    private final BlockingQueue<ConsumerEvent> consumerQueue = new LinkedBlockingQueue<>();
    private boolean stopped = false;

    @Override
    public void run() {
        Jedis jedis = plugin.getPool().getResource();
        try {
            while (!stopped) {
                ConsumerEvent event;
                try {
                    event = consumerQueue.take();
                } catch (InterruptedException e) {
                    continue;
                }
                handle(event, jedis);
            }
            for (ConsumerEvent event : consumerQueue)
                handle(event, jedis);
            consumerQueue.clear();
        } finally {
            plugin.getPool().returnResource(jedis);
        }
    }

    private void handle(ConsumerEvent event, Jedis jedis) {
        if (event instanceof PlayerLoggedInConsumerEvent) {
            PlayerLoggedInConsumerEvent event1 = (PlayerLoggedInConsumerEvent) event;
            Pipeline pipeline = jedis.pipelined();
            pipeline.sadd("proxy:" + RedisBungee.getApi().getServerId() + ":usersOnline", event1.getPlayer().getUniqueId().toString());
            pipeline.hset("player:" + event1.getPlayer().getUniqueId().toString(), "online", "0");
            pipeline.hset("player:" + event1.getPlayer().getUniqueId().toString(), "ip", event1.getPlayer().getAddress().getAddress().getHostAddress());
            plugin.getUuidTranslator().persistInfo(event1.getPlayer().getName(), event1.getPlayer().getUniqueId(), pipeline);
            pipeline.hset("player:" + event1.getPlayer().getUniqueId().toString(), "proxy", plugin.getServerId());
            pipeline.publish("redisbungee-data", RedisBungee.getGson().toJson(new DataManager.DataManagerMessage<>(
                    event1.getPlayer().getUniqueId(), DataManager.DataManagerMessage.Action.JOIN,
                    new DataManager.LoginPayload(event1.getPlayer().getAddress().getAddress()))));
            pipeline.sync();
        } else if (event instanceof PlayerLoggedOffConsumerEvent) {
            PlayerLoggedOffConsumerEvent event1 = (PlayerLoggedOffConsumerEvent) event;
            Pipeline pipeline = jedis.pipelined();
            long timestamp = System.currentTimeMillis();
            pipeline.hset("player:" + event1.getPlayer().getUniqueId().toString(), "online", String.valueOf(timestamp));
            RedisUtil.cleanUpPlayer(event1.getPlayer().getUniqueId().toString(), pipeline);
            pipeline.publish("redisbungee-data", RedisBungee.getGson().toJson(new DataManager.DataManagerMessage<>(
                    event1.getPlayer().getUniqueId(), DataManager.DataManagerMessage.Action.LEAVE,
                    new DataManager.LogoutPayload(timestamp))));
            pipeline.sync();
        } else if (event instanceof PlayerChangedServerConsumerEvent) {
            PlayerChangedServerConsumerEvent event1 = (PlayerChangedServerConsumerEvent) event;
            Pipeline pipeline = jedis.pipelined();
            pipeline.hset("player:" + event1.getPlayer().getUniqueId().toString(), "server", event1.getNewServer().getName());
            pipeline.publish("redisbungee-data", RedisBungee.getGson().toJson(new DataManager.DataManagerMessage<>(
                    event1.getPlayer().getUniqueId(), DataManager.DataManagerMessage.Action.SERVER_CHANGE,
                    new DataManager.ServerChangePayload(event1.getNewServer().getName()))));
            pipeline.sync();
        }
    }

    public void queue(ConsumerEvent event) {
        if (!stopped)
            consumerQueue.add(event);
    }

    public void stop() {
        stopped = true;
        while (!consumerQueue.isEmpty()) ;
    }
}
