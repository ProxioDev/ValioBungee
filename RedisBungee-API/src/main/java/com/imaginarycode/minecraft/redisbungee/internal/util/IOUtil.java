package com.imaginarycode.minecraft.redisbungee.internal.util;

import com.google.common.io.ByteStreams;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;


public class IOUtil {
    public static String readInputStreamAsString(InputStream is) {
        String string;
        try {
            string = new String(ByteStreams.toByteArray(is), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
        return string;
    }
}
