package net.zennin.mushoku.registry;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.levelgen.Heightmap;
import net.zennin.mushoku.MushokuMod;
import net.zennin.mushoku.entity.MagicProjectile;
import net.zennin.mushoku.entity.creature.AnimatedBoss;
import net.zennin.mushoku.entity.creature.AnimatedMonster;
import net.zennin.mushoku.entity.npc.BanditEntity;
import net.zennin.mushoku.entity.npc.CharacterEntity;
import net.zennin.mushoku.entity.npc.KalmanEntity;
import net.zennin.mushoku.entity.npc.MageBanditEntity;
import net.zennin.mushoku.entity.npc.TownNpcEntity;

public final class ModEntities {
    public static final EntityType<MagicProjectile> MAGIC_PROJECTILE = Registry.register(BuiltInRegistries.ENTITY_TYPE,
            MushokuMod.id("magic_projectile"),
            EntityType.Builder.<MagicProjectile>of(MagicProjectile::new, MobCategory.MISC)
                    .sized(0.4f, 0.4f).clientTrackingRange(8).updateInterval(2).build("magic_projectile"));

    // --- Bandidos (mismos ids que el original, para que las estructuras y comandos funcionen igual) ---
    public static final EntityType<BanditEntity> SG_BANDIT = humanoid("sg_bandit", MobCategory.MONSTER,
            (t, l) -> new BanditEntity(t, l, BanditEntity.Style.SWORD_GOD));
    public static final EntityType<BanditEntity> WG_BANDIT = humanoid("wg_bandit", MobCategory.MONSTER,
            (t, l) -> new BanditEntity(t, l, BanditEntity.Style.WATER_GOD));
    public static final EntityType<BanditEntity> NG_BANDIT = humanoid("ng_bandit", MobCategory.MONSTER,
            (t, l) -> new BanditEntity(t, l, BanditEntity.Style.NORTH_GOD));
    public static final EntityType<MageBanditEntity> M_BANDIT = humanoid("m_bandit", MobCategory.MONSTER, MageBanditEntity::new);
    public static final EntityType<KalmanEntity> KALMAN = humanoid("kalman_3", MobCategory.MONSTER, KalmanEntity::new);

    // --- NPCs de ciudad ---
    public static final EntityType<TownNpcEntity> GUILD_RECEPTIONIST = humanoid("guild_receptionist", MobCategory.CREATURE,
            (t, l) -> new TownNpcEntity(t, l, "receptionist"));
    public static final EntityType<TownNpcEntity> BLACK_SMITH_KEEP = humanoid("black_smith_keep", MobCategory.CREATURE,
            (t, l) -> new TownNpcEntity(t, l, "blacksmith"));
    public static final EntityType<TownNpcEntity> GENERAL_STORE_KEEP = humanoid("general_store_keep", MobCategory.CREATURE,
            (t, l) -> new TownNpcEntity(t, l, "general_store"));
    public static final EntityType<TownNpcEntity> MAGIC_SHOP_KEEPER = humanoid("magic_shop_keeper", MobCategory.CREATURE,
            (t, l) -> new TownNpcEntity(t, l, "magic_shop"));

    // --- Criaturas animadas (GeckoLib); tamaños y stats del original ---
    public static final EntityType<AnimatedMonster> ASSAULT_DOG = creature("ent_assault_dog", 1.5f, 1.0f,
            (t, l) -> new AnimatedMonster(t, l, new AnimatedMonster.Anims("idle", "walk", null)));
    public static final EntityType<AnimatedMonster> MUCKY_RAT = creature("ent_mucky_rat", 0.6f, 0.5f,
            (t, l) -> new AnimatedMonster(t, l, new AnimatedMonster.Anims("idle", "walk", null)));
    public static final EntityType<AnimatedMonster> DEADLY_WRAITH = creature("deadly_wraith", 1.5f, 5.0f,
            (t, l) -> new AnimatedMonster(t, l, new AnimatedMonster.Anims("idle", "walk", "attack")));
    public static final EntityType<AnimatedMonster> SNOW_DRAKE = creature("snow_drake", 0.6f, 1.8f,
            (t, l) -> new AnimatedMonster(t, l, new AnimatedMonster.Anims("Idle", "Walk", "Bite")));
    public static final EntityType<AnimatedMonster> GIANT_TURTLE = creature("giant_turtle", 17f, 10f,
            (t, l) -> new AnimatedBoss(t, l, new AnimatedMonster.Anims("Idle", "Walk", "Attack"), AnimatedBoss.Kind.TURTLE));
    public static final EntityType<AnimatedMonster> SNOW_DRAKE_MOM = creature("snow_drake_mom", 2f, 5f,
            (t, l) -> new AnimatedBoss(t, l, new AnimatedMonster.Anims("Idle", "Walk", "Bite"), AnimatedBoss.Kind.DRAKE_MOM));

    public static final EntityType<net.zennin.mushoku.entity.ThrownMushokuItem> THROWN_ITEM = Registry.register(BuiltInRegistries.ENTITY_TYPE,
            MushokuMod.id("thrown_item"), EntityType.Builder.<net.zennin.mushoku.entity.ThrownMushokuItem>of(net.zennin.mushoku.entity.ThrownMushokuItem::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f).clientTrackingRange(4).updateInterval(10).build("thrown_item"));
    public static final EntityType<net.zennin.mushoku.entity.SwapTotemEntity> SWAP_TOTEM = Registry.register(BuiltInRegistries.ENTITY_TYPE,
            MushokuMod.id("ent_swap_totem"), EntityType.Builder.<net.zennin.mushoku.entity.SwapTotemEntity>of(net.zennin.mushoku.entity.SwapTotemEntity::new, MobCategory.MISC)
                    .sized(0.6f, 1.8f).clientTrackingRange(10).build("ent_swap_totem"));

    // --- Personajes extra (skins slim) ---
    public static final EntityType<CharacterEntity> ERIS = humanoid("eris_greyrat", MobCategory.CREATURE, (t, l) -> new CharacterEntity(t, l, false));
    public static final EntityType<CharacterEntity> SYLPHIETTE = humanoid("sylphiette_greyrat", MobCategory.CREATURE, (t, l) -> new CharacterEntity(t, l, true));
    public static final EntityType<CharacterEntity> SYLPHIETTE_YOUNG = humanoid("sylphiette_joven", MobCategory.CREATURE, (t, l) -> new CharacterEntity(t, l, false));

    private ModEntities() {}

    private static <T extends Mob> EntityType<T> creature(String name, float w, float h, EntityType.EntityFactory<T> factory) {
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, MushokuMod.id(name),
                EntityType.Builder.of(factory, MobCategory.MONSTER).sized(w, h).clientTrackingRange(10).build(name));
    }

    private static <T extends Mob> EntityType<T> humanoid(String name, MobCategory cat, EntityType.EntityFactory<T> factory) {
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, MushokuMod.id(name),
                EntityType.Builder.of(factory, cat).sized(0.6f, 1.8f).clientTrackingRange(10).build(name));
    }

    private static void egg(String name, EntityType<? extends Mob> type, int bg, int fg) {
        Item item = Registry.register(BuiltInRegistries.ITEM, MushokuMod.id(name + "_spawn_egg"),
                new SpawnEggItem(type, bg, fg, new Item.Properties()));
        ModItems.ALL.add(item);
    }

    public static void init() {
        FabricDefaultAttributeRegistry.register(SG_BANDIT, BanditEntity.attributes(40));
        FabricDefaultAttributeRegistry.register(NG_BANDIT, BanditEntity.attributes(40));
        FabricDefaultAttributeRegistry.register(WG_BANDIT, BanditEntity.attributes(60));
        FabricDefaultAttributeRegistry.register(M_BANDIT, MageBanditEntity.attributes());
        FabricDefaultAttributeRegistry.register(KALMAN, KalmanEntity.attributes());
        for (EntityType<TownNpcEntity> t : java.util.List.of(GUILD_RECEPTIONIST, BLACK_SMITH_KEEP, GENERAL_STORE_KEEP, MAGIC_SHOP_KEEPER))
            FabricDefaultAttributeRegistry.register(t, TownNpcEntity.attributes());

        FabricDefaultAttributeRegistry.register(SWAP_TOTEM, net.zennin.mushoku.entity.SwapTotemEntity.attributes());
        FabricDefaultAttributeRegistry.register(ASSAULT_DOG, AnimatedMonster.attributes(30, 5, 0.4, 16));
        FabricDefaultAttributeRegistry.register(MUCKY_RAT, AnimatedMonster.attributes(10, 4, 0.4, 16));
        FabricDefaultAttributeRegistry.register(DEADLY_WRAITH, AnimatedMonster.attributes(400, 6, 0.3, 16));
        FabricDefaultAttributeRegistry.register(SNOW_DRAKE, AnimatedMonster.attributes(50, 9, 0.4, 16));
        FabricDefaultAttributeRegistry.register(GIANT_TURTLE, AnimatedMonster.attributes(420, 8, 0.3, 16)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE, 1));
        FabricDefaultAttributeRegistry.register(SNOW_DRAKE_MOM, AnimatedMonster.attributes(800, 15, 0.4, 45)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE, 1));

        egg("ent_assault_dog", ASSAULT_DOG, 0x555555, 0x993300);
        egg("ent_mucky_rat", MUCKY_RAT, 0x554433, 0x887766);
        egg("deadly_wraith", DEADLY_WRAITH, 0x111111, 0x6600AA);
        egg("snow_drake", SNOW_DRAKE, 0xDDEEFF, 0x6699CC);
        egg("snow_drake_mom", SNOW_DRAKE_MOM, 0xDDEEFF, 0x224488);
        egg("giant_turtle", GIANT_TURTLE, 0x335522, 0x887744);
        FabricDefaultAttributeRegistry.register(ERIS, CharacterEntity.stats(256, 20, 16));
        FabricDefaultAttributeRegistry.register(SYLPHIETTE, CharacterEntity.stats(150, 10, 4));
        FabricDefaultAttributeRegistry.register(SYLPHIETTE_YOUNG, CharacterEntity.stats(40, 2, 4));
        egg("eris_greyrat", ERIS, 0xB0302A, 0xE8C080);
        egg("sylphiette_greyrat", SYLPHIETTE, 0x2E7D4F, 0xF0F0F0);
        egg("sylphiette_joven", SYLPHIETTE_YOUNG, 0x4FAF6F, 0xF0F0F0);
        egg("sg_bandit", SG_BANDIT, 0x993300, 0xCC0033);
        egg("ng_bandit", NG_BANDIT, 0x993300, 0x0000FF);
        egg("wg_bandit", WG_BANDIT, 0x993300, 0x00CCCC);
        egg("m_bandit", M_BANDIT, 0x993300, 0x9933FF);
        egg("guild_receptionist", GUILD_RECEPTIONIST, 0x333333, 0xFFCC00);
        egg("magic_shop_keeper", MAGIC_SHOP_KEEPER, 0x333333, 0x6633FF);
        egg("black_smith_keep", BLACK_SMITH_KEEP, 0x333333, 0x666666);
        egg("general_store_keep", GENERAL_STORE_KEEP, 0x333333, 0x33AA33);

        // Aparicion natural de bandidos (pesos y grupos del original)
        SpawnPlacements.register(SG_BANDIT, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules);
        SpawnPlacements.register(NG_BANDIT, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules);
        SpawnPlacements.register(WG_BANDIT, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules);
        SpawnPlacements.register(M_BANDIT, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules);
        SpawnPlacements.register(ASSAULT_DOG, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules);
        SpawnPlacements.register(MUCKY_RAT, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules);
        BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(), MobCategory.MONSTER, ASSAULT_DOG, 5, 4, 4);
        BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(), MobCategory.MONSTER, MUCKY_RAT, 1, 4, 4);
        BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(), MobCategory.MONSTER, SG_BANDIT, 5, 2, 3);
        BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(), MobCategory.MONSTER, NG_BANDIT, 6, 2, 3);
        BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(), MobCategory.MONSTER, WG_BANDIT, 5, 2, 3);
        BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(), MobCategory.MONSTER, M_BANDIT, 5, 2, 3);
    }
}
