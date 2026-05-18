package me.prov25.hollowcombatlog.listeners;

import me.prov25.hollowcombatlog.HollowCombatLog;
import me.prov25.hollowcombatlog.managers.CombatManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class CombatListener implements Listener {

    private final HollowCombatLog plugin;
    private final CombatManager combatManager;

    public CombatListener(HollowCombatLog plugin) {
        this.plugin = plugin;
        this.combatManager = plugin.getCombatManager();
    }

    /**
     * Gestisce il danno tra giocatori → attiva il combat log
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDamagePlayer(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;

        Player attacker = null;

        // Danno diretto
        if (event.getDamager() instanceof Player) {
            attacker = (Player) event.getDamager();
        }
        // Freccia sparata da un giocatore
        else if (event.getDamager() instanceof org.bukkit.entity.Projectile proj) {
            if (proj.getShooter() instanceof Player shooter) {
                attacker = shooter;
            }
        }

        if (attacker == null) return;
        if (attacker.equals(victim)) return;

        me.prov25.hollowcombatlog.utils.Logger.debug("Danno rilevato tra " + attacker.getName() + " e " + victim.getName());

        // Bypass permission
        if (attacker.hasPermission("hollowcombatlog.bypass") || victim.hasPermission("hollowcombatlog.bypass")) {
            me.prov25.hollowcombatlog.utils.Logger.debug("Combat ignorato: bypass permission.");
            return;
        }

        if (!combatManager.isWorldAllowed(attacker) || !combatManager.isWorldAllowed(victim)) {
            me.prov25.hollowcombatlog.utils.Logger.debug("Combat ignorato: mondo non consentito.");
            return;
        }

        combatManager.enterCombat(attacker, victim);
    }

    /**
     * Gestisce la morte di un giocatore in combat
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player dead = event.getEntity();
        Player killer = dead.getKiller();

        combatManager.handleDeath(dead, killer);
    }

    /**
     * Blocca i comandi non permessi durante il combat
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();

        if (!combatManager.isInCombat(player)) return;
        if (player.hasPermission("hollowcombatlog.admin")) return;

        String command = event.getMessage(); // Include il /

        if (!combatManager.isCommandAllowed(command)) {
            event.setCancelled(true);
            player.sendMessage(combatManager.getCommandBlockedMessage());
        }
    }
}
