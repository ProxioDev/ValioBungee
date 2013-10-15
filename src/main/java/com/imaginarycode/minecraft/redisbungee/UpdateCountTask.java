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
        if (kill) return;
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
