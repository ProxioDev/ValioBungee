package com.imaginarycode.minecraft.redisbungee.api.events;

import java.util.UUID;

public interface IPlayerChangedServerNetworkEvent extends RedisBungeeEvent {

    UUID getUuid();

    String getServer();

    String getPreviousServer();

}
