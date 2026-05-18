package me.prov25.hollowcombatlog;

import me.prov25.hollowcombatlog.commands.CombatLogCommand;
import me.prov25.hollowcombatlog.listeners.CombatListener;
import me.prov25.hollowcombatlog.listeners.PlayerListener;
import me.prov25.hollowcombatlog.managers.CombatManager;
import me.prov25.hollowcombatlog.utils.Logger;
import org.bukkit.plugin.java.JavaPlugin;

public class HollowCombatLog extends JavaPlugin {

    private static HollowCombatLog instance;
    private CombatManager combatManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        this.combatManager = new CombatManager(this);

        registerListeners();
        registerCommands();

        Logger.info("ʜᴏʟʟᴏᴡᴄᴏᴍʙᴀᴛʟᴏɢ ᴠ" + getDescription().getVersion() + " ᴀᴛᴛɪᴠᴀᴛᴏ ᴄᴏɴ sᴜᴄᴄᴇssᴏ.");
        Logger.info("ᴀᴜᴛʜᴏʀ: ᴘʀᴏᴠ_25");
    }

    @Override
    public void onDisable() {
        if (combatManager != null) {
            combatManager.shutdown();
        }
        Logger.info("ʜᴏʟʟᴏᴡᴄᴏᴍʙᴀᴛʟᴏɢ ᴅɪsᴀʙɪʟɪᴛᴀᴛᴏ.");
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new CombatListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
    }

    private void registerCommands() {
        CombatLogCommand cmd = new CombatLogCommand(this);
        getCommand("combatlog").setExecutor(cmd);
        getCommand("combatlog").setTabCompleter(cmd);
    }

    public void reload() {
        reloadConfig();
        combatManager.reload();
        Logger.info("ᴄᴏɴꜰɪɢ ʀɪᴄᴀʀɪᴄᴀᴛᴀ ᴄᴏɴ sᴜᴄᴄᴇssᴏ.");
    }

    public static HollowCombatLog getInstance() {
        return instance;
    }

    public CombatManager getCombatManager() {
        return combatManager;
    }
}
