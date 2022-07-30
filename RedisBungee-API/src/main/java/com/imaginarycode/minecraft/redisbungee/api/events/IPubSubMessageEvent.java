package com.imaginarycode.minecraft.redisbungee.api.events;

public interface IPubSubMessageEvent extends RedisBungeeEvent {

    String getChannel();

    String getMessage();


}
