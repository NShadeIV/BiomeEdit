package org.aztecmc.biomeedit;

import org.bukkit.plugin.java.JavaPlugin;

public class Plugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("BiomeEdit: onEnable is called!");
    }

    @Override
    public void onDisable() {
        getLogger().info("BiomeEdit: onDisable is called!");
    }
}