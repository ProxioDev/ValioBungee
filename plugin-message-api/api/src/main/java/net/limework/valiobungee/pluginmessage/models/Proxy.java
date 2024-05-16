package net.limework.valiobungee.pluginmessage.models;

public class Proxy {

    private String proxyId;

    private int players;

    public Proxy() {
    }

    public Proxy(String proxyId, int players) {
        this.proxyId = proxyId;
        this.players = players;
    }

    public String proxyId() {
        return proxyId;
    }

    public int players() {
        return players;
    }
}
