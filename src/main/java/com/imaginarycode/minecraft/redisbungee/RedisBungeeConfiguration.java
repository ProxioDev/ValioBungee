/**
 * Copyright © 2013 tuxed <write@imaginarycode.com>
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See http://www.wtfpl.net/ for more details.
 */
package com.imaginarycode.minecraft.redisbungee;

// How about some nasty relection magic, my friend?
// I don't think so.

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * This class is purely internal.
 */
@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RedisBungeeConfiguration {
    private String serverId = "iluvbungee";
    private boolean canonicalGlist = true;
    private boolean playerListInPing = false;
    private List<String> linkedServers = Collections.emptyList();
}
