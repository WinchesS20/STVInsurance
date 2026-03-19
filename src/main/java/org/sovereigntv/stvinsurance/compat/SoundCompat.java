package org.sovereigntv.stvinsurance.compat;

import org.bukkit.Sound;

import java.util.HashMap;
import java.util.Map;

public class SoundCompat {

    private static final Map<String, Sound> SOUND_ALIASES = new HashMap<>();

    static {
        registerSound("ENTITY_PLAYER_LEVELUP", "ENTITY_PLAYER_LEVELUP", "LEVEL_UP");
        registerSound("ENTITY_ITEM_BREAK", "ENTITY_ITEM_BREAK", "ITEM_BREAK");
        registerSound("ENTITY_VILLAGER_YES", "ENTITY_VILLAGER_YES", "VILLAGER_YES");
        registerSound("ENTITY_VILLAGER_NO", "ENTITY_VILLAGER_NO", "VILLAGER_NO");
        registerSound("BLOCK_CHEST_OPEN", "BLOCK_CHEST_OPEN", "CHEST_OPEN");
        registerSound("ENTITY_EXPERIENCE_ORB_PICKUP", "ENTITY_EXPERIENCE_ORB_PICKUP", "ORB_PICKUP");
        registerSound("UI_TOAST_CHALLENGE_COMPLETE", "UI_TOAST_CHALLENGE_COMPLETE");
        registerSound("ENTITY_WITHER_DEATH", "ENTITY_WITHER_DEATH", "WITHER_DEATH");
        registerSound("BLOCK_NOTE_BLOCK_PLING", "BLOCK_NOTE_BLOCK_PLING", "NOTE_PLING");
        registerSound("ENTITY_ENDER_DRAGON_GROWL", "ENTITY_ENDER_DRAGON_GROWL", "ENDERDRAGON_GROWL");

        registerSoundIfExists("BLOCK_AMETHYST_BLOCK_CHIME");
        registerSoundIfExists("BLOCK_COPPER_PLACE");
        registerSoundIfExists("BLOCK_SCULK_SPREAD");
        registerSoundIfExists("ENTITY_WARDEN_EMERGE");
        registerSoundIfExists("ENTITY_BREEZE_INHALE");
        registerSoundIfExists("BLOCK_TRIAL_SPAWNER_SPAWN_MOB");
        registerSoundIfExists("BLOCK_VAULT_OPEN_SHUTTER");
    }

    private static void registerSound(String primaryName, String... aliases) {
        Sound primary = getSoundSafe(primaryName);
        if (primary != null) {
            SOUND_ALIASES.put(primaryName, primary);
        } else {
            for (String alias : aliases) {
                Sound sound = getSoundSafe(alias);
                if (sound != null) {
                    SOUND_ALIASES.put(primaryName, sound);
                    break;
                }
            }
        }
    }

    private static void registerSoundIfExists(String name) {
        Sound sound = getSoundSafe(name);
        if (sound != null) {
            SOUND_ALIASES.put(name, sound);
        }
    }

    public static Sound getSoundSafe(String name) {
        if (name == null || name.isEmpty()) return null;
        try {
            return Sound.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static Sound getSound(String name, Sound fallback) {
        Sound sound = SOUND_ALIASES.get(name);
        if (sound != null) return sound;
        sound = getSoundSafe(name);
        return sound != null ? sound : fallback;
    }

    public static Sound getSound(String name) {
        return getSound(name, Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
    }

    public static boolean soundExists(String name) {
        return getSoundSafe(name) != null;
    }
}
