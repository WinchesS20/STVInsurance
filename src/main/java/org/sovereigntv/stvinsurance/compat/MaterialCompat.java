package org.sovereigntv.stvinsurance.compat;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class MaterialCompat {

    private static final Map<String, Material> MATERIAL_ALIASES = new HashMap<>();

    static {
        registerAlias("GRASS_BLOCK", "GRASS_BLOCK", "GRASS");
        registerAlias("OAK_SIGN", "OAK_SIGN", "SIGN");
        registerAlias("OAK_WALL_SIGN", "OAK_WALL_SIGN", "WALL_SIGN");

        registerIfExists("COPPER_BLOCK");
        registerIfExists("RAW_COPPER");
        registerIfExists("RAW_IRON");
        registerIfExists("RAW_GOLD");
        registerIfExists("AMETHYST_SHARD");
        registerIfExists("SPYGLASS");
        registerIfExists("TINTED_GLASS");

        registerIfExists("SCULK");
        registerIfExists("SCULK_CATALYST");
        registerIfExists("SCULK_SHRIEKER");
        registerIfExists("SCULK_SENSOR");
        registerIfExists("MANGROVE_LOG");
        registerIfExists("MUD");
        registerIfExists("MUD_BRICKS");

        registerIfExists("CHERRY_LOG");
        registerIfExists("CHERRY_PLANKS");
        registerIfExists("BAMBOO_BLOCK");
        registerIfExists("CALIBRATED_SCULK_SENSOR");
        registerIfExists("SUSPICIOUS_GRAVEL");
        registerIfExists("DECORATED_POT");

        registerIfExists("COPPER_BULB");
        registerIfExists("CRAFTER");
        registerIfExists("TRIAL_SPAWNER");
        registerIfExists("VAULT");
        registerIfExists("HEAVY_CORE");
        registerIfExists("MACE");
        registerIfExists("BREEZE_ROD");
    }

    private static void registerAlias(String primaryName, String... aliases) {
        Material primary = getMaterialSafe(primaryName);
        if (primary != null) {
            MATERIAL_ALIASES.put(primaryName, primary);
        } else {
            for (String alias : aliases) {
                Material mat = getMaterialSafe(alias);
                if (mat != null) {
                    MATERIAL_ALIASES.put(primaryName, mat);
                    break;
                }
            }
        }
    }

    private static void registerIfExists(String name) {
        Material mat = getMaterialSafe(name);
        if (mat != null) {
            MATERIAL_ALIASES.put(name, mat);
        }
    }

    public static Material getMaterialSafe(String name) {
        if (name == null || name.isEmpty()) return null;
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static Material getMaterial(String name, Material fallback) {
        Material mat = MATERIAL_ALIASES.get(name);
        if (mat != null) return mat;
        mat = getMaterialSafe(name);
        return mat != null ? mat : fallback;
    }

    public static boolean materialExists(String name) {
        return getMaterialSafe(name) != null;
    }

    public static boolean isTool(Material material) {
        String name = material.name();
        return name.endsWith("_PICKAXE") || name.endsWith("_AXE") || name.endsWith("_SHOVEL") ||
                name.endsWith("_HOE") || name.equals("FISHING_ROD") ||
                name.equals("FLINT_AND_STEEL") || name.equals("SHEARS");
    }

    public static boolean isWeapon(Material material) {
        String name = material.name();
        return name.endsWith("_SWORD") || name.equals("BOW") || name.equals("CROSSBOW") ||
                name.equals("TRIDENT") || name.equals("MACE");
    }

    public static boolean isArmor(Material material) {
        String name = material.name();
        return name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE") ||
                name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS") ||
                name.equals("ELYTRA") || name.equals("SHIELD") || name.contains("_CAP");
    }

    public static boolean isEnchantable(ItemStack item) {
        if (item == null) return false;
        Material mat = item.getType();
        return isTool(mat) || isWeapon(mat) || isArmor(mat) ||
                mat.name().equals("BOOK") || mat.name().equals("ENCHANTED_BOOK");
    }

    public static Material getPlayerHead() {
        return Material.PLAYER_HEAD;
    }

    public static Material getStainedGlassPane(String color) {
        return getMaterial(color.toUpperCase() + "_STAINED_GLASS_PANE", Material.GRAY_STAINED_GLASS_PANE);
    }

    public static Material getWool(String color) {
        return getMaterial(color.toUpperCase() + "_WOOL", Material.WHITE_WOOL);
    }
}
