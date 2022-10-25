package com.imaginarycode.minecraft.redisbungee.api.summoners;

import java.io.Closeable;


/**
 * This class intended for future release to support redis sentinel or redis clusters
 *
 * @author Ham1255
 * @since 0.7.0
 *
 */
public interface Summoner<P> extends Closeable {

    P obtainResource();

}
