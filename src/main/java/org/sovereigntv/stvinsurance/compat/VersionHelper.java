package org.sovereigntv.stvinsurance.compat;

import org.bukkit.Bukkit;

public class VersionHelper {

    private static final int MAJOR_VERSION;
    private static final int MINOR_VERSION;
    private static final String VERSION_STRING;

    static {
        String version = Bukkit.getBukkitVersion();
        VERSION_STRING = version;

        String[] parts = version.split("-")[0].split("\\.");
        MAJOR_VERSION = Integer.parseInt(parts[0]);
        MINOR_VERSION = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
    }

    public static int getMajorVersion() { return MAJOR_VERSION; }
    public static int getMinorVersion() { return MINOR_VERSION; }
    public static String getVersionString() { return VERSION_STRING; }

    public static boolean isAtLeast(int major, int minor) {
        if (MAJOR_VERSION > major) return true;
        if (MAJOR_VERSION < major) return false;
        return MINOR_VERSION >= minor;
    }

    public static boolean isBelow(int major, int minor) {
        return !isAtLeast(major, minor);
    }

    public static boolean is1_16() { return MAJOR_VERSION == 1 && MINOR_VERSION == 16; }
    public static boolean is1_17() { return MAJOR_VERSION == 1 && MINOR_VERSION == 17; }
    public static boolean is1_18() { return MAJOR_VERSION == 1 && MINOR_VERSION == 18; }
    public static boolean is1_19() { return MAJOR_VERSION == 1 && MINOR_VERSION == 19; }
    public static boolean is1_20() { return MAJOR_VERSION == 1 && MINOR_VERSION == 20; }
    public static boolean is1_21OrHigher() { return isAtLeast(1, 21); }

    public static boolean usesLegacySkullProfile() {
        return isBelow(1, 20) || (MINOR_VERSION == 20 && getPatchVersion() < 5);
    }

    public static boolean usesResolvableProfile() {
        return isAtLeast(1, 21);
    }

    public static int getPatchVersion() {
        String version = Bukkit.getBukkitVersion();
        String[] parts = version.split("-")[0].split("\\.");
        if (parts.length > 2) {
            try {
                return Integer.parseInt(parts[2]);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    public static boolean supportsHexColors() { return isAtLeast(1, 16); }
    public static boolean supportsPersistentData() { return isAtLeast(1, 14); }

    public static String getNMSVersion() {
        String packageName = Bukkit.getServer().getClass().getPackage().getName();
        if (packageName.contains("v1_")) {
            return packageName.substring(packageName.lastIndexOf('.') + 1);
        }
        return "v" + MAJOR_VERSION + "_" + MINOR_VERSION;
    }
}
