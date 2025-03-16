package de.crafterslife.clAutoRestart;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.scheduler.BukkitRunnable;
import java.time.LocalTime;
import java.time.Duration;

public class RestartScheduler {
    private static final long RESTART_INTERVAL = 12 * 60 * 60 * 20; // 12 Stunden in Ticks
    private static final long[] WARNING_TIMES = {
            15 * 60 * 20, // 15 Minuten
            10 * 60 * 20, // 10 Minuten
            5 * 60 * 20,  // 5 Minuten
            1 * 60 * 20,  // 1 Minute
            3 * 20,       // 3 Sekunden
            2 * 20,       // 2 Sekunden
            1 * 20        // 1 Sekunde
    };

    private final ClAutoRestart plugin;
    private final BossBar bossBar;

    public RestartScheduler(ClAutoRestart plugin) {
        this.plugin = plugin;
        this.bossBar = Bukkit.createBossBar("", BarColor.RED, BarStyle.SOLID);
    }

    public void scheduleFirstRestart() {
        LocalTime now = LocalTime.now();
        LocalTime firstRestartTime = LocalTime.of(3, 0);

        long initialDelay = now.isAfter(firstRestartTime) ?
                Duration.between(now, firstRestartTime.plusHours(24)).getSeconds() * 20 :
                Duration.between(now, firstRestartTime).getSeconds() * 20;

        new BukkitRunnable() {
            @Override
            public void run() {
                scheduleRestart();
            }
        }.runTaskLater(plugin, initialDelay);
    }

    private void scheduleRestart() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (long warning : WARNING_TIMES) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            sendWarningMessage(warning);
                        }
                    }.runTaskLater(plugin, RESTART_INTERVAL - warning);
                }

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        Bukkit.broadcast(Component.text("Der Server wird jetzt neu gestartet!", NamedTextColor.RED));
                        Bukkit.spigot().restart();
                    }
                }.runTaskLater(plugin, RESTART_INTERVAL);
            }
        }.runTaskTimer(plugin, 0, RESTART_INTERVAL);
    }

    private void sendWarningMessage(long warning) {
        if (warning >= 60 * 20) {
            long minutes = warning / (60 * 20);
            Bukkit.broadcast(Component.text("Der Server startet in " + minutes + " Minuten neu!", NamedTextColor.RED));
        } else {
            long seconds = warning / 20;
            Bukkit.broadcast(Component.text("Der Server startet in " + seconds + " Sekunden neu!", NamedTextColor.RED));
            if (warning == 60 * 20) {
                startBossBarCountdown();
            }
        }
    }

    private void startBossBarCountdown() {
        bossBar.setTitle("Server Neustart in 60 Sekunden!");
        bossBar.setProgress(1.0);
        Bukkit.getOnlinePlayers().forEach(bossBar::addPlayer);

        new BukkitRunnable() {
            int timeLeft = 60;
            @Override
            public void run() {
                if (timeLeft <= 0) {
                    bossBar.removeAll();
                    cancel();
                    return;
                }
                bossBar.setTitle("Server Neustart in " + timeLeft + " Sekunden!");
                bossBar.setProgress(timeLeft / 60.0);
                timeLeft--;
            }
        }.runTaskTimer(plugin, 0, 20);
    }
}