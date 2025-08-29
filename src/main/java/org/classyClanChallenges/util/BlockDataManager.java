package org.classyClanChallenges.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.classyClanChallenges.ClassyClanChallenges;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Gestionnaire pour la persistance des blocs placés par les joueurs
 * Les métadonnées Bukkit ne survivent pas aux restarts, donc on utilise un fichier YAML
 */
public class BlockDataManager {

    private final Set<String> playerPlacedBlocks = new HashSet<>();
    private final File dataFile;
    private YamlConfiguration config;

    public BlockDataManager() {
        this.dataFile = new File(ClassyClanChallenges.getInstance().getDataFolder(), "data/player-blocks.yml");
        this.config = YamlConfiguration.loadConfiguration(dataFile);
        loadBlockData();
    }

    /**
     * Marque un bloc comme placé par un joueur
     */
    public void addPlayerBlock(Location location) {
        String key = locationToString(location);
        playerPlacedBlocks.add(key);
        saveBlockData();
    }

    /**
     * Retire un bloc de la liste (quand il est cassé)
     */
    public void removePlayerBlock(Location location) {
        String key = locationToString(location);
        playerPlacedBlocks.remove(key);
        saveBlockData();
    }

    /**
     * Vérifie si un bloc a été placé par un joueur
     */
    public boolean isPlayerPlaced(Location location) {
        String key = locationToString(location);
        return playerPlacedBlocks.contains(key);
    }

    /**
     * Convertit une location en string unique
     */
    private String locationToString(Location loc) {
        return loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    /**
     * Convertit une string en location
     */
    private Location stringToLocation(String str) {
        String[] parts = str.split(":");
        if (parts.length != 4) return null;

        World world = Bukkit.getWorld(parts[0]);
        if (world == null) return null;

        try {
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int z = Integer.parseInt(parts[3]);
            return new Location(world, x, y, z);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Charge les données depuis le fichier
     */
    private void loadBlockData() {
        playerPlacedBlocks.clear();

        List<String> blocks = config.getStringList("player-placed-blocks");
        if (blocks != null) {
            playerPlacedBlocks.addAll(blocks);
        }

        ClassyClanChallenges.getInstance().getLogger().info("Chargé " + playerPlacedBlocks.size() + " blocs placés par les joueurs.");
    }

    /**
     * Sauvegarde les données dans le fichier
     */
    private void saveBlockData() {
        config.set("player-placed-blocks", playerPlacedBlocks.stream().toList());

        try {
            config.save(dataFile);
        } catch (IOException e) {
            ClassyClanChallenges.getInstance().getLogger().severe("Erreur lors de la sauvegarde des blocs placés: " + e.getMessage());
        }
    }

    /**
     * Nettoie les blocs des mondes qui n'existent plus
     */
    public void cleanupInvalidWorlds() {
        Set<String> toRemove = new HashSet<>();

        for (String blockStr : playerPlacedBlocks) {
            Location loc = stringToLocation(blockStr);
            if (loc == null) {
                toRemove.add(blockStr);
            }
        }

        if (!toRemove.isEmpty()) {
            playerPlacedBlocks.removeAll(toRemove);
            saveBlockData();
            ClassyClanChallenges.getInstance().getLogger().info("Nettoyé " + toRemove.size() + " blocs invalides.");
        }
    }

    /**
     * Retourne le nombre de blocs placés stockés
     */
    public int getStoredBlockCount() {
        return playerPlacedBlocks.size();
    }

    /**
     * Vide complètement la liste (admin seulement)
     */
    public void clearAllBlocks() {
        playerPlacedBlocks.clear();
        saveBlockData();
    }
}