/**
 * This is free and unencumbered software released into the public domain.
 * <p/>
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 * <p/>
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 * <p/>
 * For more information, please refer to <http://unlicense.org/>
 */
package com.imaginarycode.minecraft.redisbungee.util;

import lombok.Data;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

// I would use the Guava cache, but can't because I need a few more properties.
public class InternalCache<K, V> {
    private final ConcurrentMap<K, Holder> map = new ConcurrentHashMap<>(128, 0.75f, 4);
    private final long entryWriteExpiry;

    public InternalCache() {
        this.entryWriteExpiry = 0;
    }

    public InternalCache(long entryWriteExpiry) {
        this.entryWriteExpiry = entryWriteExpiry;
    }

    public V get(K key, Callable<V> loader) throws ExecutionException {
        Holder value = map.get(key);

        if (value == null || (entryWriteExpiry > 0 && System.currentTimeMillis() > value.expiry)) {
            V freshValue;

            try {
                freshValue = loader.call();
            } catch (Exception e) {
                throw new ExecutionException(e);
            }

            if (freshValue == null)
                return null;

            map.putIfAbsent(key, value = new Holder(freshValue, System.currentTimeMillis() + entryWriteExpiry));
        }

        return value.value;
    }

    public V put(K key, V value) {
        Holder holder = map.put(key, new Holder(value, System.currentTimeMillis() + entryWriteExpiry));

        if (holder == null)
            return null;

        return holder.value;
    }

    public void invalidate(K key) {
        map.remove(key);
    }

    // Run periodically to clean up the cache mappings.
    public void cleanup() {
        if (entryWriteExpiry <= 0)
            return;

        long fixedReference = System.currentTimeMillis();
        for (Iterator<Map.Entry<K, Holder>> it = map.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<K, Holder> entry = it.next();
            if (entry.getValue().expiry > fixedReference)
                it.remove();
        }
    }

    @Data
    private class Holder {
        private final V value;
        private final long expiry;
    }
}
