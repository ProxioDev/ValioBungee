/**
 * Copyright © 2013 tuxed <write@imaginarycode.com>
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See http://www.wtfpl.net/ for more details.
 */
package com.imaginarycode.minecraft.redisbungee;

import redis.clients.jedis.Jedis;

class RedisUtil {
    public static void cleanUpPlayer(String player, Jedis rsc) {
        rsc.srem("server:" + RedisBungee.getApi().getServerId() + ":usersOnline", player);
        rsc.hdel("player:" + player, "server");
        rsc.hdel("player:" + player, "ip");
    }
}
