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
        SpellMastery.begin(p, spell);
        switch (spell) {
            case WATER_BALL -> shoot(p, MagicProjectile.WATER, 3f, 1.5f, SoundEvents.PLAYER_SPLASH);
            case ICE_SMASH -> shoot(p, MagicProjectile.ICE, 4f, 2f, SoundEvents.GLASS_BREAK);
            case FIREBALL -> shoot(p, MagicProjectile.FIRE, 4f, 2f, SoundEvents.FIRECHARGE_USE);
            case ROCK_BULLET -> shoot(p, MagicProjectile.ROCK, 4f, 2f, SoundEvents.STONE_BREAK);
            case WATER_ARROW -> waterArrow(p);
            case WATER_SHIELD -> waterShield(p, level);
            case ICE_BLADE -> giveIceBlade(p, level);
            case HEAT_HAND -> heatHand(p, level);
            case STONE_REFINING -> { return net.zennin.mushoku.refine.Refining.cast(p); }
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
            case MANA_INFUSION -> {
                // la linea de tiza rojiza que miras (hasta 8 bloques) pasa a azul
                net.minecraft.world.phys.Vec3 eye = p.getEyePosition();
                net.minecraft.world.phys.BlockHitResult hit = level.clip(new net.minecraft.world.level.ClipContext(eye,
                        eye.add(p.getLookAngle().scale(8)), net.minecraft.world.level.ClipContext.Block.OUTLINE,
                        net.minecraft.world.level.ClipContext.Fluid.NONE, p));
                net.minecraft.core.BlockPos at = hit.getBlockPos();
                if (!(level.getBlockState(at).getBlock() instanceof net.zennin.mushoku.block.ChalkLineBlock)) at = at.above();
                int n = level.getBlockState(at).getBlock() instanceof net.zennin.mushoku.block.ChalkLineBlock
                        ? net.zennin.mushoku.block.ChalkLineBlock.infuse(level, at) : 0;
                if (n == 0) {
                    p.displayClientMessage(net.minecraft.network.chat.Component.translatable("msg.remushoku.infusion_nothing")
                            .withStyle(net.minecraft.ChatFormatting.GRAY), true);
                    return false;
                }
                level.playSound(null, at, SoundEvents.AMETHYST_BLOCK_RESONATE, net.minecraft.sounds.SoundSource.PLAYERS, 1f, 1.3f);
                p.displayClientMessage(net.minecraft.network.chat.Component.translatable("msg.remushoku.infusion_done", n)
                        .withStyle(net.minecraft.ChatFormatting.AQUA), true);
            }
            case MAGIC_SHIELD -> {
                p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 300, 1));
                p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 300, 0));
                net.zennin.mushoku.entity.SpellFxEntity.spawn(level, "magic_shield", p.position(), 0, 1f, 300, p);
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
        // la bola de fuego estalla como la carga de un ghast: no rompe bloques pero prende fuego alrededor
        if (kind == MagicProjectile.FIRE) proj.incendiary(1.2f);
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

    /** Flecha de agua: proyectil translucido que vuela recto (modelo water_arrow). */
    private static void waterArrow(ServerPlayer p) {
        MagicProjectile arrow = new MagicProjectile(p.level(), p, MagicProjectile.WATER, 4f).variant(MagicProjectile.VARIANT_ARROW);
        arrow.setPos(p.getX(), p.getEyeY() - 0.1, p.getZ());
        arrow.shootFromRotation(p, p.getXRot(), p.getYRot(), 0f, 2.5f, 0f);
        p.level().addFreshEntity(arrow);
        sound(p.serverLevel(), p, SoundEvents.PLAYER_SPLASH);
    }

    /** Muro de hielo de 3x2 a 3 bloques adelante, que dura 8 segundos. */
    private static void waterShield(ServerPlayer p, ServerLevel level) {
        Direction facing = p.getDirection();
        BlockPos center = p.blockPosition().relative(facing, 3);
        Direction side = facing.getClockWise();
        // Bloques invisibles que frenan golpes y flechas; lo que se ve es la cortina de agua (modelo water_wall)
        BlockState wall = Blocks.BARRIER.defaultBlockState();
        for (int s = -1; s <= 1; s++)
            for (int h = 0; h <= 1; h++)
                TempBlocks.place(level, center.relative(side, s).above(h), wall, 160);
        net.zennin.mushoku.entity.SpellFxEntity.spawn(level, "water_wall", Vec3.atBottomCenterOf(center), facing.toYRot(), 1f, 160, null);
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
            target.hurt(p.damageSources().onFire(), SpellMastery.dmg(p, 3f));
            particles(level, ParticleTypes.FLAME, target.position().add(0, 1, 0), 20, 0.4);
        } else {
            HitResult hit = p.pick(4, 1f, true);
            if (hit instanceof BlockHitResult b && hit.getType() == HitResult.Type.BLOCK) {
                BlockPos pos = b.getBlockPos();
                if (level.getFluidState(pos).is(Fluids.WATER) || level.getFluidState(pos).is(Fluids.FLOWING_WATER)) {
                    level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                    particles(level, ParticleTypes.CLOUD, Vec3.atCenterOf(pos), 15, 0.4);
                } else {
                    // mechero: enciende fogatas y velas, y si no, prende fuego en la cara que miras (como el encendedor)
                    var st = level.getBlockState(pos);
                    if (net.minecraft.world.level.block.CampfireBlock.canLight(st) || net.minecraft.world.level.block.CandleBlock.canLight(st)
                            || net.minecraft.world.level.block.CandleCakeBlock.canLight(st)) {
                        level.setBlock(pos, st.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT, true), 11);
                    } else if (st.is(Blocks.TNT)) {
                        net.minecraft.world.level.block.TntBlock.explode(level, pos);
                        level.removeBlock(pos, false);
                    } else {
                        BlockPos firePos = pos.relative(b.getDirection());
                        if (net.minecraft.world.level.block.BaseFireBlock.canBePlacedAt(level, firePos, b.getDirection()))
                            level.setBlock(firePos, net.minecraft.world.level.block.BaseFireBlock.getState(level, firePos), 11);
                    }
                    level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, net.minecraft.sounds.SoundSource.PLAYERS, 1f, 1f);
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

    /** Rafaga: te impulsa hacia donde miras; cada 5 niveles te lanza mas lejos (x1.5 de velocidad en el nivel 20). */
    private static void blast(ServerPlayer p, ServerLevel level) {
        int tier = Math.min(4, SpellMastery.castLevel(p) / 5);
        Vec3 look = p.getLookAngle().scale(1.5 + 0.2 * tier);
        Escape.grant(p); // no te agarran telaranas ni lodo mientras dura el impulso
        net.zennin.mushoku.network.ModNetworking.gustGlide(p); // pose de planeo (como con elitros) hasta aterrizar
        p.setDeltaMovement(look);
        p.hurtMarked = true;
        p.resetFallDistance();
        particles(level, ParticleTypes.CLOUD, p.position(), 20 + 8 * tier, 0.5 + 0.15 * tier);
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

    /**
     * Excavar: rompe un 3x3 en la cara que miras. Cada 5 niveles cava una capa mas de profundidad (nivel 5 = 3x3x2,
     * 10 = 3x3x3, 15 = 3x3x4, 20 = 3x3x5). Las capas se rompen una por una cada 0.25 s, con su sonido.
     */
    private static void dig(ServerPlayer p, ServerLevel level) {
        BlockHitResult b = lookBlock(p, 3);
        if (b == null) return;
        BlockPos center = b.getBlockPos();
        Direction face = b.getDirection();
        Direction in = face.getOpposite();
        int depth = 1 + Math.min(4, SpellMastery.castLevel(p) / 5);
        for (int layer = 0; layer < depth; layer++) {
            final BlockPos c = center.relative(in, layer);
            Tasks.later(layer * 5, () -> {
                if (!p.isAlive()) return;
                boolean any = false;
                for (int i = -1; i <= 1; i++)
                    for (int j = -1; j <= 1; j++) {
                        BlockPos pos = switch (face.getAxis()) {
                            case X -> c.offset(0, i, j);
                            case Y -> c.offset(i, 0, j);
                            case Z -> c.offset(i, j, 0);
                        };
                        BlockState st = level.getBlockState(pos);
                        float hardness = st.getDestroySpeed(level, pos);
                        if (!st.isAir() && hardness >= 0 && hardness < 50 && level.mayInteract(p, pos)) {
                            level.destroyBlock(pos, true, p);
                            any = true;
                        }
                    }
                if (any) {
                    Vec3 at = Vec3.atCenterOf(c);
                    level.playSound(null, at.x, at.y, at.z, SoundEvents.STONE_BREAK, SoundSource.PLAYERS, 1f, 0.8f + level.random.nextFloat() * 0.15f);
                    level.playSound(null, at.x, at.y, at.z, SoundEvents.ROOTED_DIRT_BREAK, SoundSource.PLAYERS, 0.8f, 0.7f);
                    level.sendParticles(ParticleTypes.CLOUD, at.x, at.y, at.z, 6, 0.8, 0.8, 0.8, 0.01);
                }
            });
        }
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
