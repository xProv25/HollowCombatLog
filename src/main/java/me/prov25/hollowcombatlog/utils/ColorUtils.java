package me.prov25.hollowcombatlog.utils;

import net.md_5.bungee.api.ChatColor;

public class ColorUtils {

    public static String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
