package me.prov25.hollowcombatlog.listeners;

import me.prov25.hollowcombatlog.HollowCombatLog;
import me.prov25.hollowcombatlog.managers.CombatManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final HollowCombatLog plugin;
    private final CombatManager combatManager;

    public PlayerListener(HollowCombatLog plugin) {
        this.plugin = plugin;
        this.combatManager = plugin.getCombatManager();
    }

    /**
     * Gestisce la disconnessione: uccide o lascia il combat
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        combatManager.handleLogout(event.getPlayer());
    }

    /**
     * Al join: rimuovi eventuali dati combat residui (safety)
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Safety: rimuovi combat al rejoin (già gestito dalla morte, ma per sicurezza)
        // Non necessario di default, ma si può aggiungere logica persistente qui
    }
}
