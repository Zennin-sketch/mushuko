package net.zennin.mushoku.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.zennin.mushoku.MushokuMod;
import net.zennin.mushoku.entity.MagicProjectile;

public final class ModEntities {
    public static final EntityType<MagicProjectile> MAGIC_PROJECTILE = Registry.register(BuiltInRegistries.ENTITY_TYPE,
            MushokuMod.id("magic_projectile"),
            EntityType.Builder.<MagicProjectile>of(MagicProjectile::new, MobCategory.MISC)
                    .sized(0.4f, 0.4f).clientTrackingRange(8).updateInterval(2).build("magic_projectile"));

    private ModEntities() {}

    public static void init() {}
}
