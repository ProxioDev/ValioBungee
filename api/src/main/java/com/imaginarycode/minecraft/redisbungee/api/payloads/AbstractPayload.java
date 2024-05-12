
package com.imaginarycode.minecraft.redisbungee.api.payloads;

public abstract class AbstractPayload {

    private final String senderProxy;

    public AbstractPayload(String proxyId) {
        this.senderProxy = proxyId;
    }

    public AbstractPayload(String senderProxy, String className) {
        this.senderProxy = senderProxy;
    }

    public String senderProxy() {
        return senderProxy;
    }

    public String getClassName() {
        return getClass().getName();
    }

}
