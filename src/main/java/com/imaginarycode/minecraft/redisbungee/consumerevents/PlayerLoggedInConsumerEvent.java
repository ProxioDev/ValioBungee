/**
 * Copyright © 2013 tuxed <write@imaginarycode.com>
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See http://www.wtfpl.net/ for more details.
 */
package com.imaginarycode.minecraft.redisbungee.consumerevents;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.md_5.bungee.api.connection.ProxiedPlayer;

@AllArgsConstructor
@Getter
public class PlayerLoggedInConsumerEvent implements ConsumerEvent {
    private final ProxiedPlayer player;
}
