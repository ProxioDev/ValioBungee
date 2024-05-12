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
import com.imaginarycode.minecraft.redisbungee.api.payloads.proxy.HeartbeatPayload;

import java.lang.reflect.Type;

public class HeartbeatPayloadSerializer implements JsonSerializer<HeartbeatPayload>, JsonDeserializer<HeartbeatPayload> {


    @Override
    public HeartbeatPayload deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        String senderProxy = jsonObject.get("proxy").getAsString();
        long heartbeat = jsonObject.get("heartbeat").getAsLong();
        int players = jsonObject.get("players").getAsInt();
        return new HeartbeatPayload(senderProxy, new HeartbeatPayload.HeartbeatData(heartbeat, players));
    }

    @Override
    public JsonElement serialize(HeartbeatPayload src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("proxy", new JsonPrimitive(src.senderProxy()));
        jsonObject.add("heartbeat", new JsonPrimitive(src.data().heartbeat()));
        jsonObject.add("players", new JsonPrimitive(src.data().players()));
        return jsonObject;
    }
}
