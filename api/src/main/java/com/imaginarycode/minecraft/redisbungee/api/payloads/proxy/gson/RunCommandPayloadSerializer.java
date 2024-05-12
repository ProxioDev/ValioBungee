/*
 * Copyright (c) 2013-present RedisBungee contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *
 *  http://www.eclipse.org/legal/epl-v10.html
 */

package com.imaginarycode.minecraft.redisbungee.api.payloads.proxy.gson;

import com.google.gson.*;
import com.imaginarycode.minecraft.redisbungee.api.payloads.proxy.RunCommandPayload;

import java.lang.reflect.Type;

public class RunCommandPayloadSerializer implements JsonSerializer<RunCommandPayload>, JsonDeserializer<RunCommandPayload> {


    @Override
    public RunCommandPayload deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        String senderProxy = jsonObject.get("proxy").getAsString();
        String proxyToRun = jsonObject.get("proxy-to-run").getAsString();
        String command = jsonObject.get("command").getAsString();
        return new RunCommandPayload(senderProxy, proxyToRun, command);
    }

    @Override
    public JsonElement serialize(RunCommandPayload src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("proxy", new JsonPrimitive(src.senderProxy()));
        jsonObject.add("proxy-to-run", new JsonPrimitive(src.proxyToRun()));
        jsonObject.add("command", context.serialize(src.command()));
        return jsonObject;
    }
}
