/*
 * Copyright (c) 2013-present RedisBungee contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *
 *  http://www.eclipse.org/legal/epl-v10.html
 */


package com.imaginarycode.minecraft.redisbungee;

public class Constants {

    public final static String VERSION = "@version@";
    public final static String GIT_COMMIT = "@git_commit@";
    public final static long BUILD_DATE = Long.parseLong("@build_date@");

    public static String getGithubCommitLink() {
        return "https://github.com/ProxioDev/RedisBungee/commit/" + GIT_COMMIT;
    }

}
