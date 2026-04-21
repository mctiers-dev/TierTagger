package net.uku3lig.tiertagger;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.world.item.Items;
import net.uku3lig.tiertagger.model.GameMode;

public class KitDetector {

    private KitDetector() {}


    public static void currentGameMode() {
        if (currentServerIp().contains("mcpvp.club")) {
            return detectGameMode();
        } else {
            return GameMode.NONE;
        }
    }
    
    public static GameMode detectGameMode() {
        var player = Minecraft.getInstance().player;
        if (player == null) return GameMode.NONE;
        
        var inv = player.getInventory();
        if (inv.contains(s -> s.is(Items.END_CRYSTAL))) {
            return TierCache.findMode("vanilla").orElse(GameMode.NONE);
        }
        if (inv.contains(s -> s.is(Items.MACE))) {
            return TierCache.findMode("mace").orElse(GameMode.NONE);
        }
        return GameMode.NONE;
    }

    
    public static String currentServerIp() {
        ServerData sd = Minecraft.getInstance().getCurrentServer();
        if (sd == null || sd.ip == null || sd.ip.isEmpty()) return null;
        return stripPort(sd.ip);
    }

    static String stripPort(String raw) {
        String s = raw.trim();
        if (s.startsWith("[")) {
            int end = s.indexOf(']');
            return (end > 0) ? s.substring(1, end) : s;
        }
        int colon = s.lastIndexOf(':');
        return (colon >= 0) ? s.substring(0, colon) : s;
    }
}
