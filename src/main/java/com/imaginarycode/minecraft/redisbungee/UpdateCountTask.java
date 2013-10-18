/**
 * Copyright © 2013 tuxed <write@imaginarycode.com>
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See http://www.wtfpl.net/ for more details.
 */
package com.imaginarycode.minecraft.redisbungee;

import redis.clients.jedis.Jedis;

public class UpdateCountTask implements Runnable {

    private RedisBungee plugin;
    private boolean kill = false;

    public UpdateCountTask(RedisBungee plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if (kill) {
            return;
        }
        Jedis rsc = plugin.getPool().getResource();
        try {
            int c = plugin.getProxy().getOnlineCount();
            rsc.set("server:" + plugin.getServerId() + ":playerCount", String.valueOf(c));
            for (String i : plugin.getServers()) {
                if (i.equals(plugin.getServerId())) continue;
                if (rsc.exists("server:" + i + ":playerCount"))
                    c += Integer.valueOf(rsc.get("server:" + i + ":playerCount"));
            }
            plugin.setCount(c);
        } finally {
            plugin.getPool().returnResource(rsc);
        }
    }

    protected void kill() {
        kill = true;
    }
}
