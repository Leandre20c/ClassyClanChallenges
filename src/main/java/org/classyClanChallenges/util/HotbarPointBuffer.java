package org.classyClanChallenges.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.classyClanChallenges.challenges.ChallengeCategory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HotbarPointBuffer {

    private static final Map<UUID, Buffer> buffers = new HashMap<>();

    public static void addPoints(UUID playerId, ChallengeCategory category, int amount) {
        Buffer buffer = buffers.computeIfAbsent(playerId, id -> new Buffer(playerId));
        buffer.add(category, amount);
    }

    private static class Buffer {
        private final UUID playerId;
        private final Map<ChallengeCategory, Integer> points = new HashMap<>();
        private int taskId = -1;

        public Buffer(UUID playerId) {
            this.playerId = playerId;
        }

        public void add(ChallengeCategory category, int amount) {
            points.merge(category, amount, Integer::sum);
            showHotbar();

            // Reset timer
            if (taskId != -1) Bukkit.getScheduler().cancelTask(taskId);
            taskId = Bukkit.getScheduler().runTaskLaterAsynchronously(
                    org.classyClanChallenges.ClassyClanChallenges.getInstance(),
                    () -> finalizeDisplay(),
                    40L // 2 secondes = 40 ticks
            ).getTaskId();
        }

        private void showHotbar() {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline()) return;

            StringBuilder message = new StringBuilder("§e+");
            int total = points.values().stream().mapToInt(Integer::intValue).sum();
            message.append(total).append(" points");

            player.sendActionBar(message.toString());
        }

        private void finalizeDisplay() {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline()) return;

            for (Map.Entry<ChallengeCategory, Integer> entry : points.entrySet()) {
                ChallengeCategory cat = entry.getKey();
                int amount = entry.getValue();
                player.sendActionBar("§a+" + amount + " points pour le clan dans §6" + cat.name());
            }

            // Reset
            points.clear();
            buffers.remove(playerId);
        }
    }
}
