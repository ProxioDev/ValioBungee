/**
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 *
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information, please refer to <http://unlicense.org/>
 */
package com.imaginarycode.minecraft.redisbungee.test;

import com.imaginarycode.minecraft.redisbungee.util.InternalCache;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public class InternalCacheTest {
    @Test
    public void testNonCached() {
        InternalCache<String, String> cache = new InternalCache<>();
        try {
            Assert.assertEquals("hi", cache.get("hi", new Callable<String>() {
                @Override
                public String call() throws Exception {
                    return "hi";
                }
            }));
        } catch (ExecutionException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    public void testCached() {
        InternalCache<String, String> cache = new InternalCache<>();
        try {
            Assert.assertEquals("hi", cache.get("hi", new Callable<String>() {
                @Override
                public String call() throws Exception {
                    return "hi";
                }
            }));
            Assert.assertEquals("hi", cache.get("hi", new Callable<String>() {
                @Override
                public String call() throws Exception {
                    Assert.fail("Cache is using loader!");
                    return null;
                }
            }));
        } catch (ExecutionException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    public void testWriteExpiry() {
        final Object one = new Object();
        InternalCache<String, Object> cache = new InternalCache<>(100); // not very long
        try {
            // Successive calls should always work.
            Assert.assertEquals(one, cache.get("hi", new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    return one;
                }
            }));
            Assert.assertEquals(one, cache.get("hi", new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    Assert.fail("Cache is using loader!");
                    return null;
                }
            }));

            // But try again in a second and a bit:
            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {
                throw new AssertionError(e);
            }

            final Object two = new Object();
            Assert.assertEquals(two, cache.get("hi", new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    return two;
                }
            }));
        } catch (ExecutionException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    public void testCleanup() {
        InternalCache<String, Object> cache = new InternalCache<>(10);
        final Object one = new Object();
        final Object two = new Object();
        try {
            Assert.assertEquals(one, cache.get("hi", new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    return one;
                }
            }));
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                throw new AssertionError(e);
            }
            cache.cleanup();
            Assert.assertEquals(two, cache.get("hi", new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    return two;
                }
            }));
        } catch (ExecutionException e) {
            throw new AssertionError(e);
        }
    }
}
