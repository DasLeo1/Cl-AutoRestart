package de.crafterslife.clAutoRestart;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.LocalTime;
import java.time.temporal.ChronoField;
import java.util.concurrent.TimeUnit;

public class RestartScheduler {
    private static final long INITIAL_RESTART = TimeUnit.HOURS.toMillis(3);
    private static final long RESTART_INTERVAL = TimeUnit.HOURS.toMillis(12);

    private static final long MINUTE = 60 * 1000;

    private static final long[] WARNING_TIMES = {
            15 * MINUTE, // 15 Minuten
            10 * MINUTE, // 10 Minuten
            5 * MINUTE,  // 5 Minuten
            MINUTE,      // 1 Minute
            3 * 1000,    // 3 Sekunden
            2 * 1000,    // 2 Sekunden
            1000         // 1 Sekunde
    };

    private final ClAutoRestart plugin;
    private final BossBar bossBar;
    private final Thread thread;

    public RestartScheduler(ClAutoRestart plugin) {
        this.plugin = plugin;
        this.bossBar = Bukkit.createBossBar("", BarColor.RED, BarStyle.SOLID);
        this.thread = Thread.ofPlatform()
                .daemon()
                .name("RestartScheduler")
                .priority(Thread.MAX_PRIORITY)
                .start(this::run);
    }

    public void stop() {
        if (!thread.isAlive())
            return;
        thread.interrupt();
    }

    private void run() {
        try {
            LocalTime now = LocalTime.now();
            int millis = now.get(ChronoField.MILLI_OF_DAY);
            long restart = INITIAL_RESTART;
            while (millis > restart) {
                restart += RESTART_INTERVAL;
            }

            Thread.sleep(Math.max(restart - millis - WARNING_TIMES[0], 0));

            for (int i = 0; i < WARNING_TIMES.length; i++) {
                long time = WARNING_TIMES[i];
                sendWarningMessage(time);

                if (i < WARNING_TIMES.length - 1) {
                    Thread.sleep(time - WARNING_TIMES[i + 1]);
                }
            }

            Bukkit.spigot().restart();
        } catch (InterruptedException ignored) {}
    }

    private void sendWarningMessage(long warning) {
        if (warning >= MINUTE) {
            Bukkit.broadcast(Component.text("Der Server startet in " + warning / MINUTE + " Minuten neu!", NamedTextColor.RED));
        } else {
            Bukkit.broadcast(Component.text("Der Server startet in " + warning / 1000 + " Sekunden neu!", NamedTextColor.RED));
        }
        if (warning == MINUTE) {
            startBossBarCountdown();
        }
    }

    private void startBossBarCountdown() {
        bossBar.setTitle("Server Neustart in 60 Sekunden!");
        bossBar.setProgress(1.0);
        Bukkit.getOnlinePlayers().forEach(bossBar::addPlayer);

        long shutdown = System.currentTimeMillis() + MINUTE;
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                if (now >= shutdown) {
                    bossBar.removeAll();
                    cancel();
                    return;
                }
                long remaining = shutdown - now;
                bossBar.setTitle("Server Neustart in " + remaining / 1000 + " Sekunden!");
                bossBar.setProgress(remaining / 60000.0);
            }
        }.runTaskTimer(plugin, 0, 1);
    }
}