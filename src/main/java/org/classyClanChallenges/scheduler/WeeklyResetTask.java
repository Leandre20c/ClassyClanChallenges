package org.classyClanChallenges.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitRunnable;
import org.classyClanChallenges.ClassyClanChallenges;

import java.time.DayOfWeek;
import java.time.LocalDateTime;

public class WeeklyResetTask extends BukkitRunnable {

    private final ClassyClanChallenges plugin;
    private final FileConfiguration config;

    public WeeklyResetTask(ClassyClanChallenges plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    @Override
    public void run() {
        if (!config.getBoolean("weekly-reset.enabled", false)) return;

        LocalDateTime now = LocalDateTime.now();

        String configDay = config.getString("weekly-reset.day", "SUNDAY").toUpperCase();
        int configHour = config.getInt("weekly-reset.hour", 18);
        int configMinute = config.getInt("weekly-reset.minute", 0);

        DayOfWeek currentDay = now.getDayOfWeek();
        int currentHour = now.getHour();
        int currentMinute = now.getMinute();

        if (currentDay.name().equals(configDay) && currentHour == configHour && currentMinute == configMinute) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.getChallengeManager().resetWeeklyChallenges();
            });
        }
    }
}
