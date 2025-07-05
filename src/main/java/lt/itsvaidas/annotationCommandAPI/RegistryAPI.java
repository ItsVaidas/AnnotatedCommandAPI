package lt.itsvaidas.annotationCommandAPI;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Biome;
import org.bukkit.block.BlockType;
import org.bukkit.block.banner.PatternType;
import org.bukkit.damage.DamageType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.generator.structure.Structure;
import org.bukkit.generator.structure.StructureType;
import org.bukkit.inventory.ItemType;
import org.bukkit.inventory.MenuType;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.map.MapCursor;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class RegistryAPI {

    private static Map<Class<? extends Keyed>, RegistryKey<? extends Keyed>> registryKeys = new HashMap<>() {{
        put(GameEvent.class, RegistryKey.GAME_EVENT);
        put(StructureType.class, RegistryKey.STRUCTURE_TYPE);
        put(PotionEffectType.class, RegistryKey.MOB_EFFECT);
        put(BlockType.class, RegistryKey.BLOCK);
        put(ItemType.class, RegistryKey.ITEM);
        put(Cat.Type.class, RegistryKey.CAT_VARIANT);
        put(Frog.Variant.class, RegistryKey.FROG_VARIANT);
        put(Villager.Profession.class, RegistryKey.VILLAGER_PROFESSION);
        put(Villager.Type.class, RegistryKey.VILLAGER_TYPE);
        put(MapCursor.Type.class, RegistryKey.MAP_DECORATION_TYPE);
        put(MenuType.class, RegistryKey.MENU);
        put(Attribute.class, RegistryKey.ATTRIBUTE);
        put(Fluid.class, RegistryKey.FLUID);
        put(Sound.class, RegistryKey.SOUND_EVENT);
        put(DataComponentType.class, RegistryKey.DATA_COMPONENT_TYPE);
        put(Biome.class, RegistryKey.BIOME);
        put(Structure.class, RegistryKey.STRUCTURE);
        put(TrimMaterial.class, RegistryKey.TRIM_MATERIAL);
        put(TrimPattern.class, RegistryKey.TRIM_PATTERN);
        put(DamageType.class, RegistryKey.DAMAGE_TYPE);
        put(Wolf.Variant.class, RegistryKey.WOLF_VARIANT);
        put(Enchantment.class, RegistryKey.ENCHANTMENT);
        put(JukeboxSong.class, RegistryKey.JUKEBOX_SONG);
        put(PatternType.class, RegistryKey.BANNER_PATTERN);
        put(Art.class, RegistryKey.PAINTING_VARIANT);
        put(MusicInstrument.class, RegistryKey.INSTRUMENT);
        put(EntityType.class, RegistryKey.ENTITY_TYPE);
        put(Particle.class, RegistryKey.PARTICLE_TYPE);
        put(PotionType.class, RegistryKey.POTION);
    }};

    public static <T extends Keyed> @Nullable T tryGet(RegistryKey<T> registryKey, String key) {
        return RegistryAccess.registryAccess().getRegistry(registryKey).get(NamespacedKey.minecraft(key));
    }

    public static <T extends Keyed> Registry<@NotNull T> get(RegistryKey<T> registryKey) {
        return RegistryAccess.registryAccess().getRegistry(registryKey);
    }

    public static <T extends Keyed> @NotNull T get(RegistryKey<T> registryKey, String key) {
        T value = tryGet(registryKey, key);
        if (value == null) {
            throw new IllegalArgumentException("No value found for key: " + key + " in registry: " + registryKey);
        }
        return value;
    }

    public static <T extends Keyed> Registry<@NotNull T> get(Class<?> clazz) {
        RegistryKey<T> registryKey = (RegistryKey<T>) registryKeys.get(clazz);
        if (registryKey == null) {
            throw new IllegalArgumentException("No registry key found for class: " + clazz.getName());
        }
        return get(registryKey);
    }

    public static <T extends Keyed> @Nullable T tryGet(Class<?> clazz, String key) {
        RegistryKey<T> registryKey = (RegistryKey<T>) registryKeys.get(clazz);
        if (registryKey == null) {
            throw new IllegalArgumentException("No registry key found for class: " + clazz.getName());
        }
        return tryGet(registryKey, key);
    }

    public static <T extends Keyed> @NotNull T get(Class<?> clazz, String key) {
        RegistryKey<T> registryKey = (RegistryKey<T>) registryKeys.get(clazz);
        if (registryKey == null) {
            throw new IllegalArgumentException("No registry key found for class: " + clazz.getName());
        }
        return get(registryKey, key);
    }

    public static boolean isRegistered(Class<?> clazz) {
        return registryKeys.containsKey(clazz);
    }
}
