package org.classyClanChallenges.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public abstract class SubCommand {

    /**
     * Le nom de la sous-commande (ex: "stats", "reset", etc.)
     */
    public abstract String getName();

    /**
     * La description courte pour le help.
     */
    public abstract String getDescription();

    /**
     * L’usage affiché dans le help (ex: "/jdc stats <player>").
     */
    public abstract String getUsage();

    /**
     * Si cette commande est réservée aux admins.
     */
    public abstract boolean isAdminOnly();

    /**
     * Exécution de la commande.
     */
    public abstract void execute(CommandSender sender, String[] args);
}
