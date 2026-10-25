package net.zennin.mushoku.guild;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.zennin.mushoku.registry.ModEntities;
import net.zennin.mushoku.registry.ModItems;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** Los 18 trabajos del gremio del original: rango, objetivo y recompensa (XP de rango y monedas en cobre chico). */
public final class Jobs {
    public enum Type { KILL, COLLECT, GOLEM }

    public record Job(int id, String key, int rank, Type type, int amount, int xp, int reward,
                      Predicate<Entity> kill, Supplier<Item> item) {}

    private static final Predicate<Entity> MONSTER = e -> e instanceof Enemy;

    public static final List<Job> ALL = List.of(
            // F
            new Job(1, "carrot_collection", 1, Type.COLLECT, 25, 10, 2, null, () -> Items.CARROT),
            new Job(2, "potato_collection", 1, Type.COLLECT, 25, 10, 2, null, () -> Items.POTATO),
            new Job(3, "iron_collection", 1, Type.COLLECT, 4, 10, 2, null, () -> Items.IRON_INGOT),
            // E
            new Job(4, "iron_golem_patrol", 2, Type.GOLEM, 1, 20, 4, e -> e.getType() == EntityType.IRON_GOLEM, null),
            new Job(5, "snow_golem_patrol", 2, Type.GOLEM, 3, 20, 4, e -> e.getType() == EntityType.SNOW_GOLEM, null),
            new Job(6, "gold_collection", 2, Type.COLLECT, 6, 20, 4, null, () -> Items.GOLD_INGOT),
            // D
            new Job(7, "creeper_subjugation", 3, Type.KILL, 4, 65, Coins.LARGE, e -> e.getType() == EntityType.CREEPER, null),
            new Job(8, "skeleton_subjugation", 3, Type.KILL, 4, 65, Coins.LARGE, e -> e.getType() == EntityType.SKELETON || e.getType() == EntityType.STRAY, null),
            new Job(9, "minor_monster_subjugation", 3, Type.KILL, 10, 65, Coins.LARGE, MONSTER, null),
            // C
            new Job(11, "mossy_key_retrieval", 4, Type.COLLECT, 1, 250, 4 * Coins.LARGE, null, () -> ModItems.MOSSY_KEY),
            new Job(12, "monster_subjugation_c", 4, Type.KILL, 20, 250, 4 * Coins.LARGE, MONSTER, null),
            new Job(13, "enderman_subjugation", 4, Type.KILL, 10, 250, 4 * Coins.LARGE, e -> e.getType() == EntityType.ENDERMAN, null),
            // B
            new Job(14, "deadly_wraith_subjugation", 5, Type.KILL, 1, 750, 2 * Coins.SILVER, e -> e.getType() == ModEntities.DEADLY_WRAITH, null),
            new Job(15, "monster_subjugation_b", 5, Type.KILL, 45, 750, 2 * Coins.SILVER, MONSTER, null),
            new Job(16, "ice_key_retrieval", 5, Type.COLLECT, 1, 750, 2 * Coins.SILVER, null, () -> ModItems.ICE_KEY),
            // A
            new Job(10, "bandit_subjugation", 6, Type.KILL, 8, 3750, 8 * Coins.SILVER,
                    e -> e.getType().builtInRegistryHolder().key().location().getPath().contains("bandit"), null),
            new Job(17, "snow_drake_mother_subjugation", 6, Type.KILL, 1, 3750, 8 * Coins.SILVER, e -> e.getType() == ModEntities.SNOW_DRAKE_MOM, null),
            new Job(18, "monster_subjugation_a", 6, Type.KILL, 75, 3750, 8 * Coins.SILVER, MONSTER, null));

    private Jobs() {}

    public static Job byId(int id) {
        for (Job j : ALL) if (j.id() == id) return j;
        return null;
    }
}
