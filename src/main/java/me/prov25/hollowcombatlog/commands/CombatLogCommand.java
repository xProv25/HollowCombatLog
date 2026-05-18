package me.prov25.hollowcombatlog.commands;

import me.prov25.hollowcombatlog.HollowCombatLog;
import me.prov25.hollowcombatlog.managers.CombatManager;
import me.prov25.hollowcombatlog.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class CombatLogCommand implements CommandExecutor, TabCompleter {

    private final HollowCombatLog plugin;
    private final CombatManager combatManager;

    public CombatLogCommand(HollowCombatLog plugin) {
        this.plugin = plugin;
        this.combatManager = plugin.getCombatManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("hollowcombatlog.admin")) {
            sender.sendMessage(ColorUtils.colorize("&cNon hai il permesso."));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {

            case "reload" -> {
                plugin.reload();
                sender.sendMessage(ColorUtils.colorize(
                        "&8[&cHCL&8] &aConfigurazione ricaricata con successo."));
            }

            case "status" -> {
                int count = combatManager.getCombatPlayers().size();
                sender.sendMessage(ColorUtils.colorize("&8[&cHCL&8] &7Giocatori in combat: &c" + count));
                sender.sendMessage(ColorUtils.colorize("&8[&cHCL&8] &7Durata combat: &f" + combatManager.getCombatDuration() + "s"));
                sender.sendMessage(ColorUtils.colorize("&8[&cHCL&8] &7Radius: &f" +
                        (combatManager.isRadiusEnabled() ? "&aAbilitato &7(" + combatManager.getRadiusDistance() + " blocks)" : "&cDisabilitato")));
                sender.sendMessage(ColorUtils.colorize("&8[&cHCL&8] &7Filtro mondi: &f" + combatManager.getWorldFilterMode() +
                        " &7-> " + combatManager.getFilteredWorlds()));
                sender.sendMessage(ColorUtils.colorize("&8[&cHCL&8] &7Kill on logout: &f" +
                        (combatManager.isKillOnLogout() ? "&aAbilitato" : "&cDisabilitato")));

                if (count > 0) {
                    sender.sendMessage(ColorUtils.colorize("&8[&cHCL&8] &7Giocatori:"));
                    for (UUID uuid : combatManager.getCombatPlayers()) {
                        Player p = Bukkit.getPlayer(uuid);
                        if (p != null) {
                            sender.sendMessage(ColorUtils.colorize("  &c- &f" + p.getName() +
                                    " &7(&f" + combatManager.getCombatTime(p) + "s&7 rimasti)"));
                        }
                    }
                }
            }

            case "force" -> {
                if (args.length < 3) {
                    sender.sendMessage(ColorUtils.colorize("&cUso: /hcl force <player> <on|off>"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(ColorUtils.colorize("&cGiocatore non trovato."));
                    return true;
                }
                if (args[2].equalsIgnoreCase("off")) {
                    combatManager.leaveCombat(target, false);
                    sender.sendMessage(ColorUtils.colorize("&8[&cHCL&8] &aCombat rimosso da &f" + target.getName() + "&a."));
                } else {
                    sender.sendMessage(ColorUtils.colorize("&8[&cHCL&8] &7Per forzare il combat usa /hcl force <player> off"));
                }
            }

            default -> sendHelp(sender);
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ColorUtils.colorize("&8&m          &r &cHollowCombatLog &7by &fProv_25 &8&m          "));
        sender.sendMessage(ColorUtils.colorize(" &c/hcl reload &8» &7Ricarica la configurazione"));
        sender.sendMessage(ColorUtils.colorize(" &c/hcl status &8» &7Mostra lo stato del combat log"));
        sender.sendMessage(ColorUtils.colorize(" &c/hcl force <player> off &8» &7Forza la rimozione del combat"));
        sender.sendMessage(ColorUtils.colorize("&8&m                                                  "));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("reload", "status", "force");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("force")) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("force")) {
            return Collections.singletonList("off");
        }
        return Collections.emptyList();
    }
}
