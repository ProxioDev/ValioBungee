package com.imaginarycode.minecraft.redisbungee.test;

import com.imaginarycode.minecraft.redisbungee.RedisUtil;
import org.junit.Assert;
import org.junit.Test;

public class RedisUtilTest {
    @Test
    public void testRedisVersion() {
        Assert.assertTrue(RedisUtil.isRedisVersionSupported("6.2.0"));
        Assert.assertTrue(RedisUtil.isRedisVersionSupported(("6.1.0")));
        Assert.assertTrue(RedisUtil.isRedisVersionSupported(("6.0.0")));
        Assert.assertFalse(RedisUtil.isRedisVersionSupported(("2.6.0")));
        Assert.assertFalse(RedisUtil.isRedisVersionSupported(("2.2.12")));
        Assert.assertFalse(RedisUtil.isRedisVersionSupported(("1.2.4")));
        Assert.assertFalse(RedisUtil.isRedisVersionSupported(("2.8.4")));
        Assert.assertFalse(RedisUtil.isRedisVersionSupported(("3.0.0")));
        Assert.assertFalse(RedisUtil.isRedisVersionSupported(("3.2.1")));
    }
}
