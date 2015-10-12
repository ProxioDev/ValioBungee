package com.imaginarycode.minecraft.redisbungee.util;

import com.google.common.io.ByteStreams;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
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
