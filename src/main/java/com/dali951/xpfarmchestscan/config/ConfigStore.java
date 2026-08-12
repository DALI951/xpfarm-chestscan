package com.dali951.xpfarmchestscan.config;

public final class ConfigStore {

    private static ModConfig config = ModConfig.load();

    private ConfigStore() {
    }

    public static ModConfig get() {
        return config;
    }

    public static void set(ModConfig newConfig) {
        config = newConfig;
    }

    public static void save() {
        config.save();
    }
}