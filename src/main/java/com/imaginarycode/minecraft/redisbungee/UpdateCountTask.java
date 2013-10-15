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
    private Jedis rsc;
    private boolean kill = false;

    public UpdateCountTask(RedisBungee plugin) {
        this.plugin = plugin;
        rsc = plugin.getPool().getResource();
    }

    @Override
    public void run() {
        if (kill) {
            if (rsc != null) {
                plugin.getPool().returnResource(rsc);
                rsc = null;
            }
            return;
        }
        int c = plugin.getProxy().getOnlineCount();
        rsc.set("server:" + plugin.getServerId() + ":playerCount", String.valueOf(c));
        for (String i : plugin.getServers()) {
            if (i.equals(plugin.getServerId())) continue;
            if (rsc.exists("server:" + i + ":playerCount"))
                c += Integer.valueOf(rsc.get("server:" + i + ":playerCount"));
        }
        plugin.setCount(c);
    }

    protected void kill() {
        kill = true;
    }
}
