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
import com.imaginarycode.minecraft.redisbungee.api.payloads.proxy.DeathPayload;

import java.lang.reflect.Type;

public class DeathPayloadSerializer implements JsonSerializer<DeathPayload>, JsonDeserializer<DeathPayload> {

    private static final Gson gson = new Gson();


    @Override
    public DeathPayload deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        String senderProxy = jsonObject.get("proxy").getAsString();
        return new DeathPayload(senderProxy);
    }

    @Override
    public JsonElement serialize(DeathPayload src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("proxy", new JsonPrimitive(src.senderProxy()));
        return jsonObject;
    }
}
