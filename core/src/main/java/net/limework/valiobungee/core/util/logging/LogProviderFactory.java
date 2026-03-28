/*
* Copyright (c) 2026-present ValioBungee contributors
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the GNU GENERAL PUBLIC LICENSE Version 3
* which accompanies this distribution, and is available at
* https://www.gnu.org/licenses/gpl-3.0.txt
*/
package net.limework.valiobungee.core.util.logging;

import org.slf4j.Logger;

public class LogProviderFactory {
  private static Logger instance;

  public static void register(Logger logger) {
    if (instance != null) throw new IllegalStateException("Logger already registered");
    instance = logger;
  }

  public static Logger get() {
    if (instance == null) throw new IllegalStateException("No logger registered");
    return instance;
  }
}
