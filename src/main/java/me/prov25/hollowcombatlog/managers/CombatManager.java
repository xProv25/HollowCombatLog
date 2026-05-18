package me.prov25.hollowcombatlog.managers;

import me.prov25.hollowcombatlog.HollowCombatLog;
import me.prov25.hollowcombatlog.utils.ColorUtils;
import me.prov25.hollowcombatlog.utils.Logger;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class CombatManager {

    private final HollowCombatLog plugin;

    // UUID -> secondi rimasti
    private final Map<UUID, Integer> combatPlayers = new HashMap<>();
    // UUID -> task del timer
    private final Map<UUID, BukkitTask> combatTasks = new HashMap<>();
    // UUID -> UUID dell'avversario (per radius check)
    private final Map<UUID, UUID> combatOpponents = new HashMap<>();

    // Config cache
    private int combatDuration;
    private boolean radiusEnabled;
    private double radiusDistance;
    private boolean killOnLogout;
    private String worldFilterMode;
    private List<String> filteredWorlds;
    private List<String> allowedCommands;

    // Subtitle config
    private boolean subtitleEnabled;
    private String subtitleFormat;
    private int subtitleFadeIn;
    private int subtitleStay;
    private int subtitleFadeOut;

    // Messages
    private String msgPrefix;
    private String msgEnterCombat;
    private String msgLeaveCombat;
    private String msgCommandBlocked;
    private String msgPlayerKilled;
    private String msgLogoutKill;
    private String msgRadiusLeft;

    private BukkitTask radiusTask;
    private BukkitTask subtitleTask;

    public CombatManager(HollowCombatLog plugin) {
        this.plugin = plugin;
        loadConfig();
        startRadiusCheck();
        startSubtitleTask();
    }

    public void reload() {
        loadConfig();

        if (radiusTask != null) radiusTask.cancel();
        if (subtitleTask != null) subtitleTask.cancel();

        startRadiusCheck();
        startSubtitleTask();
    }

    private void loadConfig() {
        FileConfiguration cfg = plugin.getConfig();

        combatDuration = cfg.getInt("combat-duration", 20);
        radiusEnabled = cfg.getBoolean("radius.enabled", false);
        radiusDistance = cfg.getDouble("radius.distance", 20.0);
        killOnLogout = cfg.getBoolean("kill-on-logout", true);
        worldFilterMode = cfg.getString("world-filter.mode", "BLACKLIST").toUpperCase();
        filteredWorlds = cfg.getStringList("world-filter.worlds");
        allowedCommands = cfg.getStringList("allowed-commands");

        subtitleEnabled = cfg.getBoolean("subtitle.enabled", true);
        subtitleFormat = cfg.getString("subtitle.format", "&c⚔ Combat: &f{time}s");
        subtitleFadeIn = cfg.getInt("subtitle.fade-in", 0);
        subtitleStay = cfg.getInt("subtitle.stay", 25);
        subtitleFadeOut = cfg.getInt("subtitle.fade-out", 5);

        msgPrefix = cfg.getString("messages.prefix", "&8[&cHCL&8]");
        msgEnterCombat = cfg.getString("messages.enter-combat", "&cSei entrato in Combat! Hai {time}s.");
        msgLeaveCombat = cfg.getString("messages.leave-combat", "&aSei uscito dal Combat.");
        msgCommandBlocked = cfg.getString("messages.command-blocked", "&cNon puoi usare quel comando mentre sei in Combat!");
        msgPlayerKilled = cfg.getString("messages.player-killed", "&cSei stato eliminato in Combat da &e{killer}&c!");
        msgLogoutKill = cfg.getString("messages.logout-kill", "&e{player} &cè fuggito dal combat!");
        msgRadiusLeft = cfg.getString("messages.radius-left", "&aSei uscito dal raggio di combat.");

        Logger.debug("ᴄᴏɴꜰɪɢ ᴄᴀʀɪᴄᴀᴛᴀ: ᴅᴜʀᴀᴛᴀ=" + combatDuration + "s, ʀᴀᴅɪᴜs=" + radiusEnabled + " (" + radiusDistance + ")");
    }

    // ─── WORLD CHECK ─────────────────────────────────────────────────────────

    public boolean isWorldAllowed(Player player) {
        String worldName = player.getWorld().getName();

        if (worldFilterMode.equals("WHITELIST")) {
            return filteredWorlds.contains(worldName);
        } else {
            // BLACKLIST
            return !filteredWorlds.contains(worldName);
        }
    }

    // ─── COMBAT MANAGEMENT ───────────────────────────────────────────────────

    public void enterCombat(Player attacker, Player victim) {
        if (!isWorldAllowed(attacker) || !isWorldAllowed(victim)) return;

        // Imposta combat per entrambi
        setCombat(attacker, victim);
        setCombat(victim, attacker);
    }

    private void setCombat(Player player, Player opponent) {
        UUID uuid = player.getUniqueId();
        boolean alreadyInCombat = isInCombat(player);

        // Reset timer
        combatPlayers.put(uuid, combatDuration);
        combatOpponents.put(uuid, opponent.getUniqueId());

        // Cancella task precedente
        if (combatTasks.containsKey(uuid)) {
            combatTasks.get(uuid).cancel();
        }

        // Avvia countdown
        BukkitTask task = new BukkitRunnable() {
            int seconds = combatDuration;

            @Override
            public void run() {
                Player p = Bukkit.getPlayer(uuid);

                if (p == null || !p.isOnline()) {
                    cancel();
                    combatTasks.remove(uuid);
                    combatPlayers.remove(uuid);
                    combatOpponents.remove(uuid);
                    return;
                }

                if (seconds <= 0) {
                    leaveCombat(p, false);
                    cancel();
                    return;
                }

                combatPlayers.put(uuid, seconds);
                seconds--;
            }
        }.runTaskTimer(plugin, 0L, 20L);

        combatTasks.put(uuid, task);

        if (!alreadyInCombat) {
            String msg = ColorUtils.colorize(msgPrefix + " " + msgEnterCombat
                    .replace("{time}", String.valueOf(combatDuration)));
            player.sendMessage(msg);
            Logger.debug("ᴘʟᴀʏᴇʀ ᴇɴᴛᴇʀ ᴄᴏᴍʙᴀᴛ: " + player.getName() + " ᴠs " + opponent.getName());
        }
    }

    public void leaveCombat(Player player, boolean silent) {
        UUID uuid = player.getUniqueId();

        if (!isInCombat(player)) return;

        combatPlayers.remove(uuid);
        combatOpponents.remove(uuid);

        if (combatTasks.containsKey(uuid)) {
            combatTasks.get(uuid).cancel();
            combatTasks.remove(uuid);
        }

        // Rimuovi actionbar
        if (subtitleEnabled) {
            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, net.md_5.bungee.api.chat.TextComponent.fromLegacyText(""));
        }

        if (!silent) {
            player.sendMessage(ColorUtils.colorize(msgPrefix + " " + msgLeaveCombat));
        }

        Logger.debug("ᴘʟᴀʏᴇʀ ʟᴇᴀᴠᴇ ᴄᴏᴍʙᴀᴛ: " + player.getName());
    }

    public void handleDeath(Player dead, Player killer) {
        if (isInCombat(dead)) {
            if (killer != null) {
                dead.sendMessage(ColorUtils.colorize(msgPrefix + " " +
                        msgPlayerKilled.replace("{killer}", killer.getName())));
            }
            leaveCombat(dead, true);
        }
        if (killer != null && isInCombat(killer)) {
            leaveCombat(killer, false);
        }
    }

    public void handleLogout(Player player) {
        if (!isInCombat(player)) return;

        if (killOnLogout) {
            // Uccidi il giocatore (spawn un NPC fantasma e poi lo rimuovi, oppure semplicemente kill)
            Logger.info("ᴘʟᴀʏᴇʀ ꜰᴜɢɢɪᴛᴏ ᴅᴀʟ ᴄᴏᴍʙᴀᴛ: " + player.getName());

            // Broadcast del logout kill
            String msg = ColorUtils.colorize(msgPrefix + " " +
                    msgLogoutKill.replace("{player}", player.getName()));
            Bukkit.broadcastMessage(msg);

            // Uccidi il giocatore (sarà eseguito prima della disconnessione)
            player.setHealth(0);
        }

        leaveCombat(player, true);
    }

    // ─── COMMAND CHECK ───────────────────────────────────────────────────────

    public boolean isCommandAllowed(String command) {
        String cmd = command.toLowerCase().replaceFirst("/", "").trim();
        // Prendi solo il primo argomento (il comando base)
        String baseCmd = cmd.split(" ")[0];
        for (String allowed : allowedCommands) {
            if (allowed.equalsIgnoreCase(baseCmd)) return true;
        }
        return false;
    }

    public String getCommandBlockedMessage() {
        return ColorUtils.colorize(msgPrefix + " " + msgCommandBlocked);
    }

    // ─── RADIUS CHECK ────────────────────────────────────────────────────────

    private void startRadiusCheck() {
        if (!radiusEnabled) return;

        radiusTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID uuid : new HashSet<>(combatPlayers.keySet())) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player == null) continue;

                    UUID opponentUUID = combatOpponents.get(uuid);
                    if (opponentUUID == null) continue;

                    Player opponent = Bukkit.getPlayer(opponentUUID);
                    if (opponent == null) continue;

                    // Controlla se sono nello stesso mondo
                    if (!player.getWorld().equals(opponent.getWorld())) {
                        player.sendMessage(ColorUtils.colorize(msgPrefix + " " + msgRadiusLeft));
                        leaveCombat(player, true);
                        continue;
                    }

                    double dist = player.getLocation().distance(opponent.getLocation());
                    if (dist > radiusDistance) {
                        player.sendMessage(ColorUtils.colorize(msgPrefix + " " + msgRadiusLeft));
                        leaveCombat(player, true);
                        Logger.debug("ᴘʟᴀʏᴇʀ ᴜsᴄɪᴛᴏ ᴅᴀʟ ʀᴀᴅɪᴜs: " + player.getName() + " (ᴅɪsᴛ=" + String.format("%.1f", dist) + ")");
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 10L); // Ogni 0.5 secondi
    }

    // ─── ACTIONBAR TASK ───────────────────────────────────────────────────────

    private void startSubtitleTask() {
        if (!subtitleEnabled) return;

        subtitleTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Map.Entry<UUID, Integer> entry : combatPlayers.entrySet()) {
                    Player player = Bukkit.getPlayer(entry.getKey());
                    if (player == null) continue;

                    int secondsLeft = entry.getValue();
                    String text = ColorUtils.colorize(subtitleFormat
                            .replace("{time}", String.valueOf(secondsLeft)));

                    player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, net.md_5.bungee.api.chat.TextComponent.fromLegacyText(text));
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    // ─── UTILITY ─────────────────────────────────────────────────────────────

    public boolean isInCombat(Player player) {
        return combatPlayers.containsKey(player.getUniqueId());
    }

    public int getCombatTime(Player player) {
        return combatPlayers.getOrDefault(player.getUniqueId(), 0);
    }

    public Set<UUID> getCombatPlayers() {
        return Collections.unmodifiableSet(combatPlayers.keySet());
    }

    public void shutdown() {
        // Ferma tutti i task
        for (BukkitTask task : combatTasks.values()) {
            task.cancel();
        }
        combatTasks.clear();
        combatPlayers.clear();
        combatOpponents.clear();

        if (radiusTask != null) radiusTask.cancel();
        if (subtitleTask != null) subtitleTask.cancel();

        Logger.info("ᴄᴏᴍʙᴀᴛᴍᴀɴᴀɢᴇʀ ꜰᴇʀᴍᴀᴛᴏ.");
    }

    // ─── GETTERS CONFIG ──────────────────────────────────────────────────────

    public boolean isRadiusEnabled() { return radiusEnabled; }
    public double getRadiusDistance() { return radiusDistance; }
    public boolean isKillOnLogout() { return killOnLogout; }
    public String getWorldFilterMode() { return worldFilterMode; }
    public List<String> getFilteredWorlds() { return filteredWorlds; }
    public int getCombatDuration() { return combatDuration; }
}
