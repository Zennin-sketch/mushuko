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
import net.zennin.mushoku.companion.CompanionEntity;
import net.zennin.mushoku.companion.CompanionKind;
import net.zennin.mushoku.entity.npc.VillagerGuardEntity;
import net.zennin.mushoku.entity.npc.KalmanEntity;
import net.zennin.mushoku.entity.npc.MageBanditEntity;
import net.zennin.mushoku.entity.npc.TownNpcEntity;

public final class ModEntities {
    public static final EntityType<MagicProjectile> MAGIC_PROJECTILE = Registry.register(BuiltInRegistries.ENTITY_TYPE,
            MushokuMod.id("magic_projectile"),
            EntityType.Builder.<MagicProjectile>of(MagicProjectile::new, MobCategory.MISC)
                    .sized(0.4f, 0.4f).clientTrackingRange(8).updateInterval(2).build("magic_projectile"));
    public static final EntityType<net.zennin.mushoku.entity.MineEntity> MINE = Registry.register(BuiltInRegistries.ENTITY_TYPE,
            MushokuMod.id("minefield_mine"),
            EntityType.Builder.<net.zennin.mushoku.entity.MineEntity>of(net.zennin.mushoku.entity.MineEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.4f).clientTrackingRange(8).updateInterval(5).noSave().build("minefield_mine"));
    public static final EntityType<net.zennin.mushoku.entity.SpellFxEntity> SPELL_FX = Registry.register(BuiltInRegistries.ENTITY_TYPE,
            MushokuMod.id("spell_fx"),
            EntityType.Builder.<net.zennin.mushoku.entity.SpellFxEntity>of(net.zennin.mushoku.entity.SpellFxEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f).clientTrackingRange(10).updateInterval(20).noSave().build("spell_fx"));

    // --- Bandidos (mismos ids que el original, para que las estructuras y comandos funcionen igual) ---
    public static final EntityType<BanditEntity> SG_BANDIT = humanoid("sg_bandit", MobCategory.MONSTER,
            (t, l) -> new BanditEntity(t, l, BanditEntity.Style.SWORD_GOD));
    public static final EntityType<BanditEntity> WG_BANDIT = humanoid("wg_bandit", MobCategory.MONSTER,
            (t, l) -> new BanditEntity(t, l, BanditEntity.Style.WATER_GOD));
    public static final EntityType<BanditEntity> NG_BANDIT = humanoid("ng_bandit", MobCategory.MONSTER,
            (t, l) -> new BanditEntity(t, l, BanditEntity.Style.NORTH_GOD));
    public static final EntityType<MageBanditEntity> M_BANDIT = humanoid("m_bandit", MobCategory.MONSTER, MageBanditEntity::new);
    // --- Bandidos comunes (hombres y mujeres; cuerpo a cuerpo y arqueros) ---
    public static final EntityType<net.zennin.mushoku.entity.npc.OutlawEntity> OUTLAW_M_MELEE = humanoid("outlaw_m_melee", MobCategory.MONSTER,
            (t, l) -> new net.zennin.mushoku.entity.npc.OutlawEntity(t, l, false));
    public static final EntityType<net.zennin.mushoku.entity.npc.OutlawEntity> OUTLAW_M_ARCHER = humanoid("outlaw_m_archer", MobCategory.MONSTER,
            (t, l) -> new net.zennin.mushoku.entity.npc.OutlawEntity(t, l, true));
    public static final EntityType<net.zennin.mushoku.entity.npc.OutlawEntity> OUTLAW_F_MELEE = humanoid("outlaw_f_melee", MobCategory.MONSTER,
            (t, l) -> new net.zennin.mushoku.entity.npc.OutlawEntity(t, l, false));
    public static final EntityType<net.zennin.mushoku.entity.npc.OutlawEntity> OUTLAW_F_ARCHER = humanoid("outlaw_f_archer", MobCategory.MONSTER,
            (t, l) -> new net.zennin.mushoku.entity.npc.OutlawEntity(t, l, true));
    /** id -> {arquero} de los bandidos comunes. */
    public static final java.util.List<EntityType<net.zennin.mushoku.entity.npc.OutlawEntity>> OUTLAWS =
            java.util.List.of(OUTLAW_M_MELEE, OUTLAW_M_ARCHER, OUTLAW_F_MELEE, OUTLAW_F_ARCHER);

    // --- Hombres y mujeres bestia (oso, perro, gato) ---
    public static final java.util.Map<String, EntityType<net.zennin.mushoku.entity.npc.BeastfolkEntity>> BEASTFOLK = new java.util.LinkedHashMap<>();
    static {
        for (var tribe : net.zennin.mushoku.entity.npc.BeastfolkEntity.Tribe.values())
            for (String g : new String[]{"m", "f"}) {
                String id = "beast_" + tribe.name().toLowerCase(java.util.Locale.ROOT) + "_" + g;
                BEASTFOLK.put(id, humanoid(id, MobCategory.CREATURE, (t, l) -> new net.zennin.mushoku.entity.npc.BeastfolkEntity(t, l, tribe)));
            }
    }

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

    // --- Criaturas del mundo de Mushoku Tensei (diseno propio; mecanicas basicas por ahora) ---
    /** id -> {geo, textura, ancho, alto, escala, vida, dano, velocidad}. */
    public record Beast(String id, String geo, String texture, float w, float h, float scale, double health, double damage, double speed) {}
    public static final java.util.List<Beast> BEASTS = java.util.List.of(
            new Beast("pack_coyote", "pack_coyote", "pack_coyote", 0.8f, 0.9f, 1f, 16, 3, 0.36),
            new Beast("acid_wolf", "acid_wolf", "acid_wolf", 1.0f, 1.2f, 1f, 28, 5, 0.34),
            new Beast("sacred_beast", "sacred_beast", "sacred_beast", 1.6f, 2.0f, 1f, 200, 10, 0.34),
            new Beast("rock_turtle", "rock_turtle", "rock_turtle", 2.4f, 2.3f, 1f, 120, 8, 0.18),
            new Beast("almond_anaconda", "almond_anaconda", "almond_anaconda", 1.4f, 0.6f, 1f, 60, 7, 0.26),
            new Beast("cobra_ivory", "fang_cobra", "cobra_ivory", 0.7f, 1.2f, 1f, 18, 5, 0.3),
            new Beast("cobra_ruby", "fang_cobra", "cobra_ruby", 0.7f, 1.2f, 1f, 22, 6, 0.3),
            new Beast("treant_lesser", "treant", "treant_lesser", 1.0f, 3.0f, 0.75f, 40, 5, 0.22),
            new Beast("treant_elder", "treant", "treant_elder", 1.6f, 5.0f, 1.25f, 140, 10, 0.2),
            new Beast("treant_zombie", "treant", "treant_zombie", 1.2f, 4.0f, 1f, 70, 7, 0.22),
            new Beast("treant_stone", "treant", "treant_stone", 1.3f, 4.0f, 1f, 110, 8, 0.18),
            new Beast("red_wyrm", "red_wyrm", "red_wyrm", 9f, 8f, net.zennin.mushoku.entity.creature.WyrmEntity.SCALE, 500, 18, 0.32),
            new Beast("manatite_hydra", "manatite_hydra", "manatite_hydra", 4.5f, 5.0f, 1.4f, 600, 16, 0.24));
    public static final java.util.Map<String, EntityType<AnimatedMonster>> BEAST_TYPES = new java.util.LinkedHashMap<>();
    static {
        for (Beast b : BEASTS)
            BEAST_TYPES.put(b.id(), b.id().equals("red_wyrm")
                    ? Registry.register(BuiltInRegistries.ENTITY_TYPE, MushokuMod.id(b.id()), EntityType.Builder.<AnimatedMonster>of(
                            (t, l) -> new net.zennin.mushoku.entity.creature.WyrmEntity(t, l), MobCategory.MONSTER)
                            .sized(b.w(), b.h()).fireImmune().clientTrackingRange(16).build(b.id()))
                    : creature(b.id(), b.w(), b.h(), (t, l) -> new AnimatedMonster(t, l, new AnimatedMonster.Anims("idle", "walk", "attack"))));
    }

    public static final EntityType<net.zennin.mushoku.entity.ThrownMushokuItem> THROWN_ITEM = Registry.register(BuiltInRegistries.ENTITY_TYPE,
            MushokuMod.id("thrown_item"), EntityType.Builder.<net.zennin.mushoku.entity.ThrownMushokuItem>of(net.zennin.mushoku.entity.ThrownMushokuItem::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f).clientTrackingRange(4).updateInterval(10).build("thrown_item"));
    public static final EntityType<net.zennin.mushoku.entity.SwapTotemEntity> SWAP_TOTEM = Registry.register(BuiltInRegistries.ENTITY_TYPE,
            MushokuMod.id("ent_swap_totem"), EntityType.Builder.<net.zennin.mushoku.entity.SwapTotemEntity>of(net.zennin.mushoku.entity.SwapTotemEntity::new, MobCategory.MISC)
                    .sized(0.6f, 1.8f).clientTrackingRange(10).build("ent_swap_totem"));

    // --- Personajes extra (skins slim) ---
    // Personajes con vinculos (estilo MCA). Mismos ids que el addon original para Roxy, Rudeus, Ghislaine, Ruijerd y Kishirika.
    public static final java.util.Map<CompanionKind, EntityType<CompanionEntity>> COMPANIONS = new java.util.EnumMap<>(CompanionKind.class);
    static {
        for (CompanionKind k : CompanionKind.values())
            COMPANIONS.put(k, humanoid(k.id, MobCategory.CREATURE, (t, l) -> new CompanionEntity(t, l, k)));
    }
    /** Hitogami amistoso (dimension del vacio). */
    public static final EntityType<net.zennin.mushoku.hitogami.HitogamiEntity> HITOGAMI = humanoid("hitogami", MobCategory.MISC,
            net.zennin.mushoku.hitogami.HitogamiEntity::new);
    public static final EntityType<VillagerGuardEntity> VILLAGER_GUARD = humanoid("villager_guard_entity", MobCategory.CREATURE, VillagerGuardEntity::new);

    /** Cuerpo que queda quieto mientras usas el ojo de Clarividencia. */
    public static final EntityType<net.zennin.mushoku.entity.SpiritBodyEntity> SPIRIT_BODY = humanoid("spirit_body", MobCategory.MISC,
            net.zennin.mushoku.entity.SpiritBodyEntity::new);

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
        FabricDefaultAttributeRegistry.register(SPIRIT_BODY, net.zennin.mushoku.entity.SpiritBodyEntity.attributes());
        FabricDefaultAttributeRegistry.register(HITOGAMI, net.zennin.mushoku.hitogami.HitogamiEntity.attributes());
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
        int[][] colors = {{0xB0302A, 0xE8C080}, {0x2E7D4F, 0xF0F0F0}, {0x4FAF6F, 0xF0F0F0}, {0x2A4FA0, 0x7FB0FF},
                {0x6B4A2E, 0x3E6EB0}, {0x3A2A20, 0xC0A070}, {0x2E7A5A, 0xE0E0E0}, {0x4A2A5A, 0xD0A0E0}};
        for (CompanionKind k : CompanionKind.values()) {
            FabricDefaultAttributeRegistry.register(COMPANIONS.get(k), CompanionEntity.attributes(k));
            egg(k.id, COMPANIONS.get(k), colors[k.ordinal()][0], colors[k.ordinal()][1]);
        }
        FabricDefaultAttributeRegistry.register(VILLAGER_GUARD, VillagerGuardEntity.attributes());
        egg("villager_guard_entity", VILLAGER_GUARD, 0x6B4A2E, 0xAAAAAA);
        // Aparicion natural del addon: un personaje suelto por el mundo (pesos originales)
        int[][] spawn = {{CompanionKind.GHISLAINE.ordinal(), 8}, {CompanionKind.KISHIRIKA.ordinal(), 1}, {CompanionKind.ROXY.ordinal(), 3},
                {CompanionKind.RUDEUS.ordinal(), 2}, {CompanionKind.RUIJERD.ordinal(), 5}};
        for (int[] sp : spawn) {
            EntityType<CompanionEntity> t = COMPANIONS.get(CompanionKind.values()[sp[0]]);
            SpawnPlacements.register(t, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.Mob::checkMobSpawnRules);
            BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(), MobCategory.CREATURE, t, sp[1], 1, 1);
        }
        for (var t : OUTLAWS) {
            boolean archer = t == OUTLAW_M_ARCHER || t == OUTLAW_F_ARCHER;
            FabricDefaultAttributeRegistry.register(t, net.zennin.mushoku.entity.npc.OutlawEntity.attributes(archer));
            String id = BuiltInRegistries.ENTITY_TYPE.getKey(t).getPath();
            egg(id, t, 0x5A3E2A, archer ? 0x6E8B3D : 0xA03030);
            SpawnPlacements.register(t, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules);
            BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(), MobCategory.MONSTER, t, 4, 1, 3);
        }
        int[] beastColors = {0x6B4A2E, 0x8C8C8C, 0xD08A3A};
        for (var e : BEASTFOLK.entrySet()) {
            var tribe = net.zennin.mushoku.entity.npc.BeastfolkEntity.Tribe.valueOf(e.getKey().split("_")[1].toUpperCase(java.util.Locale.ROOT));
            FabricDefaultAttributeRegistry.register(e.getValue(), net.zennin.mushoku.entity.npc.BeastfolkEntity.attributes(tribe));
            egg(e.getKey(), e.getValue(), beastColors[tribe.ordinal()], e.getKey().endsWith("_f") ? 0xF0C0D0 : 0x3A5A8A);
            SpawnPlacements.register(e.getValue(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.Mob::checkMobSpawnRules);
            BiomeModifications.addSpawn(BiomeSelectors.tag(net.minecraft.tags.BiomeTags.IS_FOREST), MobCategory.CREATURE, e.getValue(), 2, 1, 2);
        }
        int[][] beastEgg = {{0xB08A5A, 0x6E5232}, {0x2C3A2A, 0x6CFF3A}, {0xF4F2EC, 0xFFD24A}, {0x8A8478, 0x5E8A3A}, {0xC8A46A, 0x6A4A24},
                {0xE8E2D0, 0x3A6AD0}, {0x3A1A1E, 0xD02030}, {0x6A4A2E, 0x7AB83C}, {0x5A4430, 0xC8A03A}, {0x5A5450, 0xC060FF},
                {0x8A8680, 0xFFA040}, {0xA02218, 0xD8803A}, {0x3A5A6A, 0x6A4AD8}};
        for (int i = 0; i < BEASTS.size(); i++) {
            Beast b = BEASTS.get(i);
            FabricDefaultAttributeRegistry.register(BEAST_TYPES.get(b.id()), b.id().equals("red_wyrm")
                    ? net.zennin.mushoku.entity.creature.WyrmEntity.wyrmAttributes()
                    : AnimatedMonster.attributes(b.health(), b.damage(), b.speed(), 24));
            egg(b.id(), BEAST_TYPES.get(b.id()), beastEgg[i][0], beastEgg[i][1]);
        }
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
