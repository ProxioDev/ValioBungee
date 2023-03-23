package com.imaginarycode.minecraft.redisbungee;

public class Constants {

    public final static String VERSION = "@version@";
    public final static String GIT_COMMIT = "@git_commit@";

    public static String getGithubCommitLink() {
        return "https://github.com/ProxioDev/RedisBungee/commit/" + GIT_COMMIT;
    }

}
