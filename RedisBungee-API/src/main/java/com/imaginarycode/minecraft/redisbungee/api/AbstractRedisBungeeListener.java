package com.imaginarycode.minecraft.redisbungee.api;


import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.io.ByteArrayDataOutput;
import com.google.gson.Gson;

import java.net.InetAddress;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public abstract class AbstractRedisBungeeListener<LE, PLE, PD, SC, PP, PM, PS> {

    protected static final String ALREADY_LOGGED_IN = "§cYou are already logged on to this server. \n\nIt may help to try logging in again in a few minutes.\nIf this does not resolve your issue, please contact staff.";

    protected static final String ONLINE_MODE_RECONNECT = "§cWhoops! You need to reconnect\n\nWe found someone online using your username. They were kicked and you may reconnect.\nIf this does not work, please contact staff.";

    protected final RedisBungeePlugin<?> plugin;
    protected final List<InetAddress> exemptAddresses;
    protected final Gson gson = new Gson();

    public AbstractRedisBungeeListener(RedisBungeePlugin<?> plugin, List<InetAddress> exemptAddresses) {
        this.plugin = plugin;
        this.exemptAddresses = exemptAddresses;
    }

    public void onLogin(LE event) {}

    public abstract void onPostLogin(PLE event);

    public abstract void onPlayerDisconnect(PD event);

    public abstract void onServerChange(SC event);

    public abstract void onPing(PP event);

    public abstract void onPluginMessage(PM event);

    public abstract void onPubSubMessage(PS event);


}
