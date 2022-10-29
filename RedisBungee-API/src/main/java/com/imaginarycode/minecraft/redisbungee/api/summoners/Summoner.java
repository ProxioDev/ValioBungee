/*
 * Copyright (c) 2013-present RedisBungee contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *
 *  http://www.eclipse.org/legal/epl-v10.html
 */

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
