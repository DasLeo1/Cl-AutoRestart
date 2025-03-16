package de.crafterslife.clAutoRestart;

import org.bukkit.plugin.java.JavaPlugin;

public class ClAutoRestart extends JavaPlugin {
    private RestartScheduler restartScheduler;

    @Override
    public void onEnable() {
        restartScheduler = new RestartScheduler(this);
    }

    @Override
    public void onDisable() {
        if (restartScheduler == null)
            return;
        restartScheduler.stop();
        restartScheduler = null;
    }
}