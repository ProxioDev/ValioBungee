package com.imaginarycode.minecraft.redisbungee.api.events;

import java.util.UUID;

public interface IPlayerLeftNetworkEvent extends RedisBungeeEvent {

    UUID getUuid();

}
