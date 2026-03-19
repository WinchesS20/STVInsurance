package org.sovereigntv.stvinsurance.utils;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.sovereigntv.stvinsurance.compat.VersionHelper;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

public class SkullUtils {

    private static Method setOwnerProfileMethod = null;
    private static boolean ownerProfileMethodSearched = false;

    public static ItemStack createCustomSkull(String textureValue) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        if (textureValue == null || textureValue.isEmpty()) return skull;

        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta == null) return skull;

        try {
            GameProfile profile = new GameProfile(UUID.randomUUID(), "STV_UpgradeStone");
            profile.getProperties().put("textures", new Property("textures", textureValue));

            boolean success = false;

            if (VersionHelper.is1_21OrHigher()) {
                success = trySetResolvableProfile(meta, profile);
            }

            if (!success && VersionHelper.isAtLeast(1, 18)) {
                success = trySetOwnerProfile(meta, profile);
            }

            if (!success) {
                success = trySetGameProfileDirectly(meta, profile);
            }

            if (!success) {
                Bukkit.getLogger().warning("[STVInsurance] Could not apply skull texture");
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("[STVInsurance] Failed to apply skull texture: " +
                    e.getClass().getSimpleName() + ": " + e.getMessage());
        }

        skull.setItemMeta(meta);
        return skull;
    }

    private static boolean trySetOwnerProfile(SkullMeta meta, GameProfile gameProfile) {
        if (!ownerProfileMethodSearched) {
            ownerProfileMethodSearched = true;
            try {
                for (Method method : meta.getClass().getMethods()) {
                    if (method.getName().equals("setOwnerProfile") && method.getParameterCount() == 1) {
                        setOwnerProfileMethod = method;
                        break;
                    }
                }
            } catch (Exception ignored) {}
        }

        if (setOwnerProfileMethod != null) {
            try {
                Object playerProfile = createPlayerProfile(gameProfile);
                if (playerProfile != null) {
                    setOwnerProfileMethod.invoke(meta, playerProfile);
                    return true;
                }
            } catch (Exception ignored) {}
        }
        return false;
    }

    private static Object createPlayerProfile(GameProfile gameProfile) {
        try {
            Method createProfileMethod = Bukkit.class.getMethod("createPlayerProfile", UUID.class, String.class);
            return createProfileMethod.invoke(null, gameProfile.getId(), gameProfile.getName());
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean trySetResolvableProfile(SkullMeta meta, GameProfile gameProfile) {
        try {
            Class<?> resolvableProfileClass = Class.forName("net.minecraft.world.item.component.ResolvableProfile");
            Constructor<?> constructor = resolvableProfileClass.getConstructor(GameProfile.class);
            Object resolvableProfile = constructor.newInstance(gameProfile);

            Field profileField = getProfileField(meta);
            if (profileField != null) {
                profileField.setAccessible(true);
                profileField.set(meta, resolvableProfile);
                return true;
            }
        } catch (ClassNotFoundException e) {
            return false;
        } catch (Exception e) {
            Bukkit.getLogger().warning("[STVInsurance] Error setting ResolvableProfile: " + e.getMessage());
        }
        return false;
    }

    private static boolean trySetGameProfileDirectly(SkullMeta meta, GameProfile gameProfile) {
        try {
            Field profileField = getProfileField(meta);
            if (profileField != null) {
                profileField.setAccessible(true);
                profileField.set(meta, gameProfile);
                return true;
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("[STVInsurance] Error setting GameProfile directly: " + e.getMessage());
        }
        return false;
    }

    private static Field getProfileField(SkullMeta meta) {
        String[] possibleFieldNames = {"profile", "skullProfile", "gameProfile", "serializedProfile"};
        Class<?> clazz = meta.getClass();

        while (clazz != null && clazz != Object.class) {
            for (String fieldName : possibleFieldNames) {
                try { return clazz.getDeclaredField(fieldName); } catch (NoSuchFieldException ignored) {}
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }

    public static boolean isCustomSkull(ItemStack item, String expectedTexture) {
        if (item == null || item.getType() != Material.PLAYER_HEAD || !item.hasItemMeta()) return false;

        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta == null) return false;

        try {
            Field profileField = getProfileField(meta);
            if (profileField == null) return false;

            profileField.setAccessible(true);
            Object profileObj = profileField.get(meta);
            if (profileObj == null) return false;

            GameProfile gameProfile = extractGameProfileFromObject(profileObj);
            if (gameProfile == null) return false;

            for (Property property : gameProfile.getProperties().get("textures")) {
                String value = getPropertyValueSafe(property);
                if (expectedTexture.equals(value)) return true;
            }
        } catch (Exception ignored) {}

        return false;
    }

    private static GameProfile extractGameProfileFromObject(Object profileObj) {
        try {
            if (profileObj instanceof GameProfile) return (GameProfile) profileObj;

            Class<?> profileClass = profileObj.getClass();
            if (profileClass.getName().contains("ResolvableProfile")) {
                try {
                    Method gameProfileMethod = profileClass.getMethod("gameProfile");
                    return (GameProfile) gameProfileMethod.invoke(profileObj);
                } catch (NoSuchMethodException e) {
                    for (Method method : profileClass.getMethods()) {
                        if (method.getReturnType() == GameProfile.class && method.getParameterCount() == 0) {
                            try { return (GameProfile) method.invoke(profileObj); } catch (Exception ignored) {}
                        }
                    }
                }
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("[STVInsurance] Error extracting GameProfile: " + e.getMessage());
        }
        return null;
    }

    private static String getPropertyValueSafe(Property property) {
        try {
            try {
                return (String) property.getClass().getMethod("getValue").invoke(property);
            } catch (NoSuchMethodException e) {
                return (String) property.getClass().getMethod("value").invoke(property);
            }
        } catch (Exception e) {
            try {
                for (Field field : property.getClass().getDeclaredFields()) {
                    if (field.getType() == String.class) {
                        field.setAccessible(true);
                        Object value = field.get(property);
                        if (value != null) return value.toString();
                    }
                }
            } catch (Exception ignored) {}
        }
        return null;
    }
}
