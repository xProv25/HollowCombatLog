package me.prov25.hollowcombatlog.utils;

import me.prov25.hollowcombatlog.HollowCombatLog;

import java.util.logging.Level;

/**
 * Logger personalizzato per HollowCombatLog.
 * Usa font small-caps nei log, by Prov_25.
 */
public class Logger {

    private static final String PREFIX = "[ʜᴄʟ] ";

    public static void info(String message) {
        HollowCombatLog.getInstance().getLogger().info(PREFIX + message);
    }

    public static void warning(String message) {
        HollowCombatLog.getInstance().getLogger().warning(PREFIX + message);
    }

    public static void severe(String message) {
        HollowCombatLog.getInstance().getLogger().severe(PREFIX + message);
    }

    public static void debug(String message) {
        if (HollowCombatLog.getInstance().getConfig().getBoolean("debug", false)) {
            HollowCombatLog.getInstance().getLogger().log(Level.INFO, PREFIX + "[ᴅᴇʙᴜɢ] " + message);
        }
    }
}
