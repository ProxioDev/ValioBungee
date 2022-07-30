package com.imaginarycode.minecraft.redisbungee.api.events;

import java.util.UUID;

public interface IPlayerJoinedNetworkEvent extends RedisBungeeEvent {

    UUID getUuid();

}
