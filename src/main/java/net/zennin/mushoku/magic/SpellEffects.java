package net.zennin.mushoku.magic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.zennin.mushoku.entity.MagicProjectile;
import net.zennin.mushoku.registry.ModItems;

import java.util.Comparator;
import java.util.List;

/** Efectos de los hechizos. Devuelve true si el hechizo se lanzo (y se cobra el mana). */
public final class SpellEffects {
    private SpellEffects() {}

    public static boolean cast(Spell spell, ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        switch (spell) {
            case WATER_BALL -> shoot(p, MagicProjectile.WATER, 3f, 1.5f, SoundEvents.PLAYER_SPLASH);
            case ICE_SMASH -> shoot(p, MagicProjectile.ICE, 4f, 2f, SoundEvents.GLASS_BREAK);
            case FIREBALL -> shoot(p, MagicProjectile.FIRE, 4f, 2f, SoundEvents.FIRECHARGE_USE);
            case ROCK_BULLET -> shoot(p, MagicProjectile.ROCK, 4f, 2f, SoundEvents.STONE_BREAK);
            case WATER_ARROW -> waterArrow(p);
            case WATER_SHIELD -> waterShield(p, level);
            case ICE_BLADE -> giveIceBlade(p, level);
            case HEAT_HAND -> heatHand(p, level);
            case BURNING_PLACE -> burningPlace(p, level);
            case BLAST -> blast(p, level);
            case SOFT_LANDING -> selfEffect(p, level, new MobEffectInstance(MobEffects.SLOW_FALLING, 200, 0), ParticleTypes.CLOUD);
            case FLEET_FOOT -> selfEffect(p, level, new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 1), ParticleTypes.CLOUD);
            case SAND_CLOUD -> sandCloud(p, level);
            case DIG -> dig(p, level);
            case HEALING -> {
                p.heal(4f);
                particles(level, ParticleTypes.HEART, p.position().add(0, 1, 0), 8, 0.5);
                sound(level, p, SoundEvents.AMETHYST_BLOCK_CHIME);
            }
            case GROW -> grow(p, level);
            case MAGIC_SHIELD -> {
                p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 300, 1));
                p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 300, 0));
                particles(level, ParticleTypes.ENCHANT, p.position().add(0, 1, 0), 30, 0.8);
                sound(level, p, SoundEvents.EVOKER_CAST_SPELL);
            }
            default -> {
                return AdvancedSpells.cast(spell, p);
            }
        }
        return true;
    }

    // ---------- utilidades ----------

    private static void shoot(ServerPlayer p, int kind, float damage, float speed, SoundEvent snd) {
        MagicProjectile proj = new MagicProjectile(p.level(), p, kind, damage);
        proj.setPos(p.getX(), p.getEyeY() - 0.1, p.getZ());
        proj.shootFromRotation(p, p.getXRot(), p.getYRot(), 0f, speed, 0f);
        p.level().addFreshEntity(proj);
        sound(p.serverLevel(), p, snd);
    }

    private static void sound(ServerLevel level, ServerPlayer p, SoundEvent snd) {
        level.playSound(null, p.getX(), p.getY(), p.getZ(), snd, SoundSource.PLAYERS, 1f, 1f);
    }

    private static void particles(ServerLevel level, ParticleOptions type, Vec3 pos, int count, double spread) {
        level.sendParticles(type, pos.x, pos.y, pos.z, count, spread, spread, spread, 0.02);
    }

    private static BlockHitResult lookBlock(ServerPlayer p, double range) {
        HitResult hit = p.pick(range, 1f, false);
        return hit instanceof BlockHitResult b && hit.getType() == HitResult.Type.BLOCK ? b : null;
    }

    private static LivingEntity lookEntity(ServerPlayer p, double range) {
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        AABB box = p.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0);
        List<LivingEntity> list = p.level().getEntitiesOfClass(LivingEntity.class, box, e -> e != p && e.isAlive());
        return list.stream()
                .filter(e -> {
                    Vec3 to = e.position().add(0, e.getBbHeight() / 2, 0).subtract(eye);
                    return to.normalize().dot(look) > 0.8 && to.length() <= range + 1;
                })
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(p)))
                .orElse(null);
    }

    private static void selfEffect(ServerPlayer p, ServerLevel level, MobEffectInstance effect, ParticleOptions part) {
        p.addEffect(effect);
        particles(level, part, p.position(), 15, 0.6);
        sound(level, p, SoundEvents.PHANTOM_FLAP);
    }

    // ---------- hechizos ----------

    private static void waterArrow(ServerPlayer p) {
        Arrow arrow = new Arrow(p.level(), p, new ItemStack(Items.ARROW), null);
        arrow.setBaseDamage(4.0);
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        arrow.shootFromRotation(p, p.getXRot(), p.getYRot(), 0f, 2.5f, 0f);
        p.level().addFreshEntity(arrow);
        sound(p.serverLevel(), p, SoundEvents.PLAYER_SPLASH);
    }

    /** Muro de hielo de 3x2 a 3 bloques adelante, que dura 8 segundos. */
    private static void waterShield(ServerPlayer p, ServerLevel level) {
        Direction facing = p.getDirection();
        BlockPos center = p.blockPosition().relative(facing, 3);
        Direction side = facing.getClockWise();
        BlockState ice = Blocks.PACKED_ICE.defaultBlockState();
        for (int s = -1; s <= 1; s++)
            for (int h = 0; h <= 1; h++)
                TempBlocks.place(level, center.relative(side, s).above(h), ice, 160);
        particles(level, ParticleTypes.SPLASH, Vec3.atCenterOf(center), 30, 1.0);
        sound(level, p, SoundEvents.PLAYER_SPLASH);
    }

    private static void giveIceBlade(ServerPlayer p, ServerLevel level) {
        ItemStack blade = new ItemStack(ModItems.ICE_BLADE);
        if (!p.getInventory().add(blade)) p.drop(blade, false);
        particles(level, ParticleTypes.SNOWFLAKE, p.position().add(0, 1, 0), 20, 0.5);
        sound(level, p, SoundEvents.GLASS_BREAK);
    }

    /** Mano ardiente: prende fuego a lo que tengas adelante, o evapora el agua que mires. */
    private static void heatHand(ServerPlayer p, ServerLevel level) {
        LivingEntity target = lookEntity(p, 4);
        if (target != null) {
            target.igniteForSeconds(5);
            target.hurt(p.damageSources().onFire(), 3f);
            particles(level, ParticleTypes.FLAME, target.position().add(0, 1, 0), 20, 0.4);
        } else {
            HitResult hit = p.pick(4, 1f, true);
            if (hit instanceof BlockHitResult b) {
                BlockPos pos = b.getBlockPos();
                if (level.getFluidState(pos).is(Fluids.WATER) || level.getFluidState(pos).is(Fluids.FLOWING_WATER)) {
                    level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                    particles(level, ParticleTypes.CLOUD, Vec3.atCenterOf(pos), 15, 0.4);
                } else {
                    particles(level, ParticleTypes.FLAME, Vec3.atCenterOf(pos), 15, 0.4);
                }
            }
        }
        sound(level, p, SoundEvents.FIRECHARGE_USE);
    }

    /** Lugar ardiente: anillo de fuego alrededor del jugador (radio 2). */
    private static void burningPlace(ServerPlayer p, ServerLevel level) {
        BlockPos base = p.blockPosition();
        for (int x = -2; x <= 2; x++)
            for (int z = -2; z <= 2; z++) {
                if (Math.abs(x) < 2 && Math.abs(z) < 2) continue;
                BlockPos pos = base.offset(x, 0, z);
                if (level.getBlockState(pos).isAir() && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP))
                    level.setBlockAndUpdate(pos, BaseFireBlock.getState(level, pos));
            }
        particles(level, ParticleTypes.FLAME, p.position(), 30, 2.0);
        sound(level, p, SoundEvents.FIRECHARGE_USE);
    }

    /** Rafaga: te impulsa hacia donde miras. */
    private static void blast(ServerPlayer p, ServerLevel level) {
        Vec3 look = p.getLookAngle().scale(1.5);
        p.setDeltaMovement(look);
        p.hurtMarked = true;
        p.resetFallDistance();
        particles(level, ParticleTypes.CLOUD, p.position(), 20, 0.5);
        sound(level, p, SoundEvents.PHANTOM_FLAP);
    }

    /** Nube de arena: nube que ciega y ralentiza, 5 bloques adelante. */
    private static void sandCloud(ServerPlayer p, ServerLevel level) {
        Vec3 pos = p.getEyePosition().add(p.getLookAngle().scale(5));
        BlockHitResult b = lookBlock(p, 5);
        if (b != null) pos = b.getLocation();
        AreaEffectCloud cloud = new AreaEffectCloud(level, pos.x, pos.y - 0.5, pos.z);
        cloud.setOwner(p);
        cloud.setRadius(3f);
        cloud.setDuration(200);
        cloud.setParticle(ParticleTypes.WHITE_ASH);
        cloud.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0));
        cloud.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
        level.addFreshEntity(cloud);
        sound(level, p, SoundEvents.SAND_BREAK);
    }

    /** Excavar: rompe un area de 3x3 en el bloque que mires (hasta 3 bloques de distancia). */
    private static void dig(ServerPlayer p, ServerLevel level) {
        BlockHitResult b = lookBlock(p, 3);
        if (b == null) return;
        BlockPos center = b.getBlockPos();
        Direction face = b.getDirection();
        for (int i = -1; i <= 1; i++)
            for (int j = -1; j <= 1; j++) {
                BlockPos pos = switch (face.getAxis()) {
                    case X -> center.offset(0, i, j);
                    case Y -> center.offset(i, 0, j);
                    case Z -> center.offset(i, j, 0);
                };
                BlockState st = level.getBlockState(pos);
                float hardness = st.getDestroySpeed(level, pos);
                if (!st.isAir() && hardness >= 0 && hardness < 50) level.destroyBlock(pos, true, p);
            }
        sound(level, p, SoundEvents.STONE_BREAK);
    }

    /** Crecimiento: aplica polvo de hueso a los cultivos y plantas en radio 3. */
    private static void grow(ServerPlayer p, ServerLevel level) {
        BlockPos base = p.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(base.offset(-3, -1, -3), base.offset(3, 1, 3))) {
            if (BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), level, pos.immutable()))
                particles(level, ParticleTypes.HAPPY_VILLAGER, Vec3.atCenterOf(pos), 4, 0.3);
        }
        sound(level, p, SoundEvents.AMETHYST_BLOCK_CHIME);
    }
}
