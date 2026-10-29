package net.zennin.mushoku.magic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.zennin.mushoku.entity.MagicProjectile;

import java.util.List;

/**
 * Hechizos de nivel Intermedio a Imperial, mas curacion y magia variada avanzadas.
 * Los valores de dano siguen al original donde se pudieron leer (Canon de Agua 6, Ex Llama 8, etc.).
 */
public final class AdvancedSpells {
    private AdvancedSpells() {}

    public static boolean cast(Spell spell, ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        switch (spell) {
            // ================= AGUA =================
            case WATER_CANNON -> shoot(p, proj(p, MagicProjectile.WATER, 6f).size(2f).knockback(1.5f), 1.8f, 0f, SoundEvents.PLAYER_SPLASH_HIGH_SPEED);
            case ICICLE_LANCE -> {
                shoot(p, proj(p, MagicProjectile.ICE, 6f).size(1.3f), 2.2f, -4f, SoundEvents.GLASS_BREAK);
                shoot(p, proj(p, MagicProjectile.ICE, 6f).size(1.3f), 2.2f, 4f, null);
            }
            case ICE_PILLAR -> {
                BlockHitResult hit = lookBlock(p, 11);
                boolean ok = pillar(p, level, net.zennin.mushoku.registry.ModBlocks.ICE_PILLAR, ParticleTypes.SNOWFLAKE, SoundEvents.GLASS_BREAK);
                if (ok && hit != null)
                    net.zennin.mushoku.entity.SpellFxEntity.spawn(level, "ice_crystals", Vec3.atBottomCenterOf(hit.getBlockPos().above()), p.getYRot(), 2.2f, 120, null);
                return ok;
            }
            case FLOOD_FLUSH -> wave(p, level);
            case BLIZZARD_STORM -> {
                for (float yaw = -15f; yaw <= 15f; yaw += 10f)
                    shoot(p, proj(p, MagicProjectile.ICE, 6f).size(1.3f), 1.8f, yaw, null);
                for (LivingEntity e : around(level, p.position(), 8, p)) {
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2));
                    e.clearFire();
                }
                particles(level, ParticleTypes.SNOWFLAKE, p.position().add(0, 1, 0), 120, 4.0);
                sound(level, p.position(), SoundEvents.POWDER_SNOW_PLACE, 1.5f);
            }
            case ICE_FORTRESS -> dome(p, level, Blocks.PACKED_ICE.defaultBlockState(), 300, SoundEvents.GLASS_PLACE);
            case CUMULONIMBUS -> {
                // Nube enorme (36 bloques de radio): se arma en espiral, destella y suelta 4 rayos mientras llueve
                Vec3 c = targetPoint(p, 40);
                double R = net.zennin.mushoku.entity.SpellFxEntity.CUMULO_RADIUS;
                net.zennin.mushoku.entity.SpellFxEntity.spawn(level, "cumulonimbus", c.add(0, net.zennin.mushoku.entity.SpellFxEntity.CUMULO_HEIGHT, 0),
                        p.getRandom().nextFloat() * 360, net.zennin.mushoku.entity.SpellFxEntity.CUMULO_SCALE, net.zennin.mushoku.entity.SpellFxEntity.CUMULO_LIFE, null);
                sound(level, c, SoundEvents.ELYTRA_FLYING, 1.0f);
                Tasks.later(net.zennin.mushoku.entity.SpellFxEntity.CUMULO_FORM - 20, () -> sound(level, c, SoundEvents.LIGHTNING_BOLT_THUNDER, 0.6f));
                int[] strikes = net.zennin.mushoku.entity.SpellFxEntity.CUMULO_STRIKES;
                for (int i = 0; i < strikes.length; i++) {
                    final int n = i;
                    Tasks.later(strikes[i], () -> {
                        List<LivingEntity> near = around(level, c, R * 0.85, p);
                        Vec3 at = n < near.size() ? near.get(n).position()
                                : c.add((p.getRandom().nextDouble() - 0.5) * R * 1.4, 0, (p.getRandom().nextDouble() - 0.5) * R * 1.4);
                        lightning(level, p, at);
                    });
                }
                Tasks.later(net.zennin.mushoku.entity.SpellFxEntity.CUMULO_FORM, () -> sound(level, c, SoundEvents.WEATHER_RAIN, 2f));
                Tasks.repeat(net.zennin.mushoku.entity.SpellFxEntity.CUMULO_FORM, 10, net.zennin.mushoku.entity.SpellFxEntity.CUMULO_RAIN_END - net.zennin.mushoku.entity.SpellFxEntity.CUMULO_FORM, tick -> {
                    // La lluvia apaga a quien este debajo y el fuego del suelo (tambien el que prenden los rayos)
                    for (LivingEntity e : around(level, c, R, null)) e.clearFire();
                    BlockPos b = BlockPos.containing(c);
                    int r = (int) Math.ceil(R);
                    for (BlockPos pos : BlockPos.betweenClosed(b.offset(-r, -4, -r), b.offset(r, 6, r)))
                        if (level.getBlockState(pos).is(Blocks.FIRE)) level.removeBlock(pos, false);
                    if (tick % 20 == 0) sound(level, c, SoundEvents.WEATHER_RAIN, 2f);
                    return true;
                });
            }
            case LIGHTNING -> {
                // Una nube chica se arma en espiral sobre el objetivo y suelta el rayo
                Vec3 c = targetPoint(p, 50);
                net.zennin.mushoku.entity.SpellFxEntity.spawn(level, "storm_cloud", c.add(0, 11, 0), p.getRandom().nextFloat() * 360, 3f, net.zennin.mushoku.entity.SpellFxEntity.STORM_LIFE, null);
                Tasks.later(net.zennin.mushoku.entity.SpellFxEntity.STORM_FORM, () -> {
                    List<LivingEntity> targets = around(level, c, 8, p);
                    if (targets.isEmpty()) lightning(level, p, c);
                    for (int i = 0; i < Math.min(5, targets.size()); i++) lightning(level, p, targets.get(i).position());
                });
            }
            case ABSOLUTE_ZERO -> {
                for (LivingEntity e : around(level, p.position(), 12, p)) {
                    e.setTicksFrozen(e.getTicksRequiredToFreeze() + 300);
                    e.clearFire();
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 10));
                    e.hurt(p.damageSources().freeze(), SpellMastery.dmg(p, 12f));
                }
                BlockPos base = p.blockPosition();
                for (BlockPos pos : BlockPos.betweenClosed(base.offset(-8, -3, -8), base.offset(8, 3, 8))) {
                    if (level.getFluidState(pos).is(Fluids.WATER) && level.getBlockState(pos).is(Blocks.WATER))
                        level.setBlockAndUpdate(pos, Blocks.ICE.defaultBlockState());
                }
                particles(level, ParticleTypes.SNOWFLAKE, p.position().add(0, 1, 0), 400, 8.0);
                // Grupos de cristales que brotan alrededor
                for (int i = 0; i < 9; i++) {
                    double a = i * 0.7 + p.getRandom().nextDouble();
                    double r = 3 + p.getRandom().nextDouble() * 7;
                    double x = p.getX() + Math.cos(a) * r, z = p.getZ() + Math.sin(a) * r;
                    int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(x), (int) Math.floor(z));
                    if (Math.abs(y - p.getY()) > 6) y = (int) Math.floor(p.getY());
                    final Vec3 at = new Vec3(x, y, z);
                    final float sc = 1.1f + p.getRandom().nextFloat() * 0.9f, yaw = p.getRandom().nextFloat() * 360;
                    Tasks.later(i * 2, () -> net.zennin.mushoku.entity.SpellFxEntity.spawn(level, "ice_crystals", at, yaw, sc, 100, null));
                }
                sound(level, p.position(), SoundEvents.GLASS_BREAK, 2f);
            }

            // ================= FUEGO =================
            case EX_FLAME -> shoot(p, proj(p, MagicProjectile.FIRE, 8f).size(2f).incendiary(2.2f), 1.6f, 0f, SoundEvents.BLAZE_SHOOT);
            case FLAME_SLICE -> {
                // Se ve un solo tajo de fuego ancho; los costados son golpes invisibles para cubrir el mismo abanico
                for (float yaw = -10f; yaw <= 10f; yaw += 10f)
                    // atraviesa hasta 3 objetivos
                    shoot(p, yaw == 0f ? proj(p, MagicProjectile.FIRE, 6f).size(1.4f).variant(MagicProjectile.VARIANT_SLICE).pierce(3)
                            : proj(p, MagicProjectile.FIRE, 6f).variant(MagicProjectile.VARIANT_HIDDEN).pierce(3), 2f, yaw, null);
                sound(level, p.position(), SoundEvents.BLAZE_SHOOT, 1f);
            }
            case EXPLOSION -> {
                BlockHitResult b = lookBlock(p, 80);
                if (b == null) return false;
                Vec3 at = b.getLocation();
                particles(level, ParticleTypes.FLAME, at, 60, 1.5);
                net.zennin.mushoku.entity.SpellFxEntity.spawn(level, "fire_burst", at, 0, 4f, 20, null);
                level.explode(p, at.x, at.y, at.z, 3f, Level.ExplosionInteraction.MOB);
            }
            case EXODUS_FLAME -> shoot(p, proj(p, MagicProjectile.FIRE, 15f).size(3f).explosion(2f), 1.2f, 0f, SoundEvents.BLAZE_SHOOT);
            case MAGMA_GUSH -> {
                Vec3 c = targetPoint(p, 25);
                Tasks.repeat(0, 5, 60, t -> {
                    level.sendParticles(ParticleTypes.LAVA, c.x, c.y + 0.5, c.z, 8, 1.0, 0.2, 1.0, 0.1);
                    level.sendParticles(ParticleTypes.FLAME, c.x, c.y + 1.5, c.z, 20, 0.8, 1.5, 0.8, 0.05);
                    if (t % 10 == 0) {
                        for (LivingEntity e : around(level, c, 3, p)) {
                            e.hurt(p.damageSources().indirectMagic(p, p), SpellMastery.dmg(p, 4f));
                            e.igniteForSeconds(4);
                            e.push(0, 0.6, 0);
                            e.hurtMarked = true;
                        }
                    }
                    return true;
                });
                sound(level, c, SoundEvents.LAVA_POP, 2f);
            }
            case FLASH_OVER -> {
                // Fogonazo: radio 10
                for (LivingEntity e : around(level, p.position(), 10, p)) {
                    e.hurt(p.damageSources().indirectMagic(p, p), SpellMastery.dmg(p, 12f));
                    e.igniteForSeconds(8);
                }
                for (int r = 1; r <= 10; r += 2) {
                    final int rad = r;
                    Tasks.later(r, () -> ring(level, p.position(), rad, ParticleTypes.FLAME));
                }
                sound(level, p.position(), SoundEvents.FIRECHARGE_USE, 2f);
            }
            case MINEFIELD -> minefield(p, level);
            case NUCLEAR_STRIKE -> {
                // la explosion crece con el nivel del hechizo: 8, y +2 en los niveles 5, 10, 15 y 20
                Vec3 c = targetPoint(p, 100);
                int tier = SpellMastery.castLevel(p) / 5;
                float power = 8f + 2f * tier;
                double ringR = 6 + 1.5 * tier;
                Tasks.repeat(0, 4, 60, t -> {
                    level.sendParticles(ParticleTypes.END_ROD, c.x, c.y + 10 - t / 6.0, c.z, 6 + tier * 2, 0.3, 0.3, 0.3, 0.01);
                    ring(level, c, ringR, ParticleTypes.FLAME);
                    return true;
                });
                Tasks.later(60, () -> {
                    level.explode(p, c.x, c.y, c.z, power, Level.ExplosionInteraction.MOB);
                    level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y + 1, c.z, 6 + tier * 3, 3 + tier, 2 + tier * 0.5, 3 + tier, 0);
                    net.zennin.mushoku.entity.SpellFxEntity.spawn(level, "fire_burst", c, 0, 6f + tier * 2f, 20, null);
                    // onda de calor que crece con el nivel
                    for (int r = 2; r <= ringR * 2; r += 2) {
                        final int rad = r;
                        Tasks.later(r / 2, () -> ring(level, c, rad, ParticleTypes.FLAME));
                    }
                });
                sound(level, c, SoundEvents.BEACON_ACTIVATE, 3f);
            }

            // ================= TIERRA =================
            case STONE_CANNON -> shoot(p, proj(p, MagicProjectile.ROCK, 6f).size(1.6f).knockback(1f), 1.8f, 0f, SoundEvents.STONE_BREAK);
            case EARTH_PILLAR -> {
                BlockHitResult hit = lookBlock(p, 11);
                boolean ok = pillar(p, level, net.zennin.mushoku.registry.ModBlocks.EARTH_PILLAR, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()), SoundEvents.STONE_PLACE);
                if (ok && hit != null)
                    net.zennin.mushoku.entity.SpellFxEntity.spawn(level, "earth_pillar_burst", Vec3.atBottomCenterOf(hit.getBlockPos().above()), p.getYRot(), 1.2f, 70, null);
                return ok;
            }
            case EARTH_LANCE -> earthLance(p, level);
            case EARTH_WALL -> {
                Direction facing = p.getDirection();
                Direction side = facing.getClockWise();
                BlockPos center = p.blockPosition().relative(facing, 3);
                for (int s = -2; s <= 2; s++)
                    for (int h = 0; h <= 2; h++)
                        TempBlocks.place(level, center.relative(side, s).above(h), Blocks.COBBLESTONE.defaultBlockState(), 300);
                sound(level, Vec3.atCenterOf(center), SoundEvents.STONE_PLACE, 1.5f);
            }
            case EARTH_FORTRESS -> dome(p, level, Blocks.STONE.defaultBlockState(), 300, SoundEvents.STONE_PLACE);
            case DUST_STORM -> {
                Vec3 c = targetPoint(p, 15);
                AreaEffectCloud cloud = new AreaEffectCloud(level, c.x, c.y, c.z);
                cloud.setOwner(p);
                cloud.setRadius(6f);
                cloud.setDuration(300);
                cloud.setParticle(ParticleTypes.WHITE_ASH);
                cloud.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0));
                cloud.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2));
                cloud.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 1));
                level.addFreshEntity(cloud);
                sound(level, c, SoundEvents.SAND_BREAK, 2f);
            }
            case SANDSTORM -> {
                Tasks.repeat(0, 20, 200, t -> {
                    if (!p.isAlive()) return false;
                    for (LivingEntity e : around(level, p.position(), 10, p)) {
                        e.hurt(p.damageSources().indirectMagic(p, p), SpellMastery.dmg(p, 2f));
                        e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
                    }
                    level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()),
                            p.getX(), p.getY() + 1.5, p.getZ(), 150, 8, 2, 8, 0.1);
                    return true;
                });
                sound(level, p.position(), SoundEvents.SAND_BREAK, 2f);
            }
            case EARTH_DRILL -> {
                Vec3 eye = p.getEyePosition();
                Vec3 dir = p.getLookAngle();
                for (int i = 1; i <= 20; i++) {
                    BlockPos c = BlockPos.containing(eye.add(dir.scale(i)));
                    for (BlockPos pos : BlockPos.betweenClosed(c.offset(-1, -1, -1), c.offset(1, 1, 1))) breakable(level, pos.immutable(), p);
                }
                sound(level, p.position(), SoundEvents.STONE_BREAK, 2f);
            }
            case MOUNTAIN -> {
                BlockHitResult b = lookBlock(p, 40);
                if (b == null) return false;
                BlockPos base = b.getBlockPos();
                for (int dx = -6; dx <= 6; dx++)
                    for (int dz = -6; dz <= 6; dz++) {
                        double d = Math.sqrt(dx * dx + dz * dz);
                        if (d > 6) continue;
                        int h = (int) Math.round(9 * (1 - d / 6.5));
                        for (int y = 1; y <= h; y++) TempBlocks.place(level, base.offset(dx, y, dz), Blocks.STONE.defaultBlockState(), 600);
                    }
                for (LivingEntity e : around(level, Vec3.atCenterOf(base), 7, p)) {
                    e.push(0, 1.5, 0);
                    e.hurtMarked = true;
                    e.hurt(p.damageSources().indirectMagic(p, p), SpellMastery.dmg(p, 15f));
                }
                sound(level, Vec3.atCenterOf(base), SoundEvents.STONE_PLACE, 3f);
            }

            // ================= VIENTO =================
            case SONIC_BOOM -> {
                shoot(p, proj(p, MagicProjectile.WIND, 4f).knockback(1f), 2.2f, -3f, SoundEvents.PHANTOM_FLAP);
                shoot(p, proj(p, MagicProjectile.WIND, 4f).knockback(1f), 2.2f, 3f, null);
            }
            case WIND_SLICE -> shoot(p, proj(p, MagicProjectile.WIND, 4f), 2.5f, 0f, SoundEvents.PLAYER_ATTACK_SWEEP);
            case GALE_FORCE -> {
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 400, 1));
                p.addEffect(new MobEffectInstance(MobEffects.JUMP, 400, 1));
                p.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 400, 1));
                particles(level, ParticleTypes.CLOUD, p.position(), 30, 0.8);
                sound(level, p.position(), SoundEvents.PHANTOM_FLAP, 1f);
            }
            case WIND_GUST -> {
                for (LivingEntity e : around(level, p.position(), 12, p)) {
                    Vec3 away = e.position().subtract(p.position()).normalize().scale(3.0);
                    e.push(away.x, 0.6, away.z);
                    e.hurtMarked = true;
                }
                ring(level, p.position(), 4, ParticleTypes.CLOUD);
                sound(level, p.position(), SoundEvents.PHANTOM_FLAP, 2f);
            }
            case SONIC_BLAST -> shoot(p, proj(p, MagicProjectile.WIND, 8f).size(2f).knockback(2f), 2.2f, 0f, SoundEvents.PHANTOM_FLAP);
            case TORNADO_IMPACT -> shoot(p, proj(p, MagicProjectile.WIND, 6f).size(1.6f).lift(), 2f, 0f, SoundEvents.PHANTOM_FLAP);
            case VIOLENT_STORM -> {
                Tasks.repeat(0, 10, 160, t -> {
                    if (!p.isAlive()) return false;
                    for (LivingEntity e : around(level, p.position(), 10, p)) {
                        Vec3 away = e.position().subtract(p.position()).normalize().scale(1.2);
                        e.push(away.x, 0.3, away.z);
                        e.hurtMarked = true;
                        e.hurt(p.damageSources().indirectMagic(p, p), SpellMastery.dmg(p, 3f));
                    }
                    ring(level, p.position(), 3 + (t / 10) % 7, ParticleTypes.CLOUD);
                    return true;
                });
                sound(level, p.position(), SoundEvents.ELYTRA_FLYING, 1.5f);
            }
            case TORNADO -> {
                Vec3 c = targetPoint(p, 30);
                Tasks.repeat(0, 5, 160, t -> {
                    for (LivingEntity e : around(level, c, 8, p)) {
                        Vec3 in = c.subtract(e.position()).normalize().scale(0.4);
                        e.push(in.x, 0.35, in.z);
                        e.hurtMarked = true;
                        if (t % 10 == 0) e.hurt(p.damageSources().indirectMagic(p, p), SpellMastery.dmg(p, 2f));
                    }
                    for (int h = 0; h < 8; h++) {
                        double a = (t * 0.6 + h) % (Math.PI * 2);
                        double r = 0.5 + h * 0.35;
                        level.sendParticles(ParticleTypes.CLOUD, c.x + Math.cos(a) * r, c.y + h, c.z + Math.sin(a) * r, 2, 0.1, 0.1, 0.1, 0);
                    }
                    return true;
                });
                sound(level, c, SoundEvents.ELYTRA_FLYING, 2f);
            }
            case EYE_OF_THE_STORM -> {
                List<LivingEntity> targets = around(level, p.position(), 20, p);
                for (LivingEntity e : targets) {
                    e.push(0, 2.0, 0);
                    e.hurtMarked = true;
                    e.hurt(p.damageSources().indirectMagic(p, p), SpellMastery.dmg(p, 15f));
                }
                for (int i = 0; i < Math.min(5, targets.size()); i++) lightning(level, p, targets.get(i).position());
                for (int r = 2; r <= 20; r += 3) ring(level, p.position(), r, ParticleTypes.CLOUD);
                sound(level, p.position(), SoundEvents.LIGHTNING_BOLT_THUNDER, 2f);
            }

            // ================= SIN ELEMENTO =================
            case X_HEALING -> {
                p.heal(10f);
                p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
                particles(level, ParticleTypes.HEART, p.position().add(0, 1, 0), 15, 0.6);
                sound(level, p.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1f);
            }
            case FEAST -> {
                for (Player pl : level.getEntitiesOfClass(Player.class, p.getBoundingBox().inflate(8))) {
                    pl.getFoodData().eat(20, 20f);
                    particles(level, ParticleTypes.HAPPY_VILLAGER, pl.position().add(0, 1, 0), 15, 0.5);
                }
                sound(level, p.position(), SoundEvents.PLAYER_BURP, 1f);
            }
            case SHRINE_HEALING -> {
                for (Player pl : level.getEntitiesOfClass(Player.class, p.getBoundingBox().inflate(10))) {
                    pl.heal(20f);
                    pl.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0));
                    pl.removeEffect(MobEffects.POISON);
                    pl.removeEffect(MobEffects.WITHER);
                    particles(level, ParticleTypes.HEART, pl.position().add(0, 1, 0), 15, 0.6);
                }
                ring(level, p.position(), 10, ParticleTypes.END_ROD);
                sound(level, p.position(), SoundEvents.BEACON_POWER_SELECT, 1f);
            }
            case DISTURB_MAGIC -> {
                for (MagicProjectile mp : level.getEntitiesOfClass(MagicProjectile.class, p.getBoundingBox().inflate(12)))
                    if (mp.getOwner() != p) mp.discard();
                for (LivingEntity e : around(level, p.position(), 12, p)) {
                    e.removeAllEffects();
                    e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1));
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 0));
                }
                ring(level, p.position(), 6, ParticleTypes.WITCH);
                sound(level, p.position(), SoundEvents.ILLUSIONER_CAST_SPELL, 1f);
            }
            default -> {
                return AddonSpells.cast(spell, p);
            }
        }
        return true;
    }

    // ---------------------------------------------------------------- utilidades

    private static MagicProjectile proj(ServerPlayer p, int kind, float damage) {
        return new MagicProjectile(p.level(), p, kind, damage);
    }

    private static void shoot(ServerPlayer p, MagicProjectile proj, float speed, float yawOffset, SoundEvent snd) {
        proj.setPos(p.getX(), p.getEyeY() - 0.1, p.getZ());
        proj.shootFromRotation(p, p.getXRot(), p.getYRot() + yawOffset, 0f, speed, 0f);
        p.level().addFreshEntity(proj);
        if (snd != null) sound(p.serverLevel(), p.position(), snd, 1f);
    }

    private static void sound(ServerLevel level, Vec3 at, SoundEvent snd, float volume) {
        level.playSound(null, at.x, at.y, at.z, snd, SoundSource.PLAYERS, volume, 1f);
    }

    private static void particles(ServerLevel level, ParticleOptions type, Vec3 pos, int count, double spread) {
        level.sendParticles(type, pos.x, pos.y, pos.z, count, spread, spread * 0.5, spread, 0.02);
    }

    private static void ring(ServerLevel level, Vec3 c, double radius, ParticleOptions type) {
        int n = (int) Math.max(12, radius * 10);
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * i / n;
            level.sendParticles(type, c.x + Math.cos(a) * radius, c.y + 0.3, c.z + Math.sin(a) * radius, 1, 0, 0.1, 0, 0);
        }
    }

    private static List<LivingEntity> around(ServerLevel level, Vec3 c, double radius, ServerPlayer caster) {
        AABB box = new AABB(c, c).inflate(radius);
        return level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != caster && e.isAlive() && e.distanceToSqr(c) <= radius * radius);
    }

    private static BlockHitResult lookBlock(ServerPlayer p, double range) {
        HitResult hit = p.pick(range, 1f, false);
        return hit instanceof BlockHitResult b && hit.getType() == HitResult.Type.BLOCK ? b : null;
    }

    /** Punto que mira el jugador (bloque) o, si no hay, el punto a esa distancia. */
    private static Vec3 targetPoint(ServerPlayer p, double range) {
        BlockHitResult b = lookBlock(p, range);
        return b != null ? b.getLocation() : p.getEyePosition().add(p.getLookAngle().scale(range));
    }

    private static void lightning(ServerLevel level, ServerPlayer p, Vec3 at) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt == null) return;
        bolt.moveTo(at);
        bolt.setCause(p);
        level.addFreshEntity(bolt);
    }

    private static void breakable(ServerLevel level, BlockPos pos, ServerPlayer p) {
        BlockState st = level.getBlockState(pos);
        float hardness = st.getDestroySpeed(level, pos);
        if (!st.isAir() && hardness >= 0 && hardness < 50 && level.getBlockEntity(pos) == null) level.destroyBlock(pos, true, p);
    }

    /** Pilar de 7 bloques que sale del bloque que mires (hasta 11) y lanza a quien este encima. */
    private static boolean pillar(ServerPlayer p, ServerLevel level, net.zennin.mushoku.block.SpellPillarBlock block, ParticleOptions part, SoundEvent snd) {
        BlockHitResult b = lookBlock(p, 11);
        if (b == null) return false;
        BlockPos base = b.getBlockPos();
        for (LivingEntity e : around(level, Vec3.atCenterOf(base.above()), 2, p)) {
            e.push(0, 1.4, 0);
            e.hurtMarked = true;
            e.hurt(p.damageSources().indirectMagic(p, p), SpellMastery.dmg(p, 5f));
        }
        for (int y = 1; y <= 7; y++) {
            final int yy = y;
            Tasks.later(y, () -> {
                if (TempBlocks.place(level, base.above(yy), block.part(yy, 7), 200 - yy)) {
                    Vec3 at = Vec3.atBottomCenterOf(base.above(yy));
                    level.sendParticles(part, at.x, at.y + 0.2, at.z, 8, 0.35, 0.15, 0.35, 0.05);
                    if (yy % 2 == 1) level.playSound(null, at.x, at.y, at.z, snd, SoundSource.PLAYERS, 0.7f, 0.8f + yy * 0.08f);
                }
            });
        }
        particles(level, part, Vec3.atCenterOf(base.above()), 30, 0.8);
        sound(level, Vec3.atCenterOf(base), snd, 1.5f);
        return true;
    }

    /** Cupula de radio 3 alrededor del jugador. */
    private static void dome(ServerPlayer p, ServerLevel level, BlockState state, int ticks, SoundEvent snd) {
        BlockPos c = p.blockPosition();
        int r = 3;
        for (BlockPos pos : BlockPos.betweenClosed(c.offset(-r, -1, -r), c.offset(r, r, r))) {
            double d = Math.sqrt(pos.distSqr(c));
            if (d >= r - 0.5 && d < r + 0.5) TempBlocks.place(level, pos.immutable(), state, ticks);
        }
        sound(level, p.position(), snd, 1.5f);
    }

    /** Ola: empuja y moja todo lo que tengas adelante. */
    private static void wave(ServerPlayer p, ServerLevel level) {
        Vec3 look = p.getLookAngle();
        // apaga lo encendido a lo largo de la ola
        Vec3 flat = new Vec3(look.x, 0, look.z).normalize();
        for (int i = 2; i <= 8; i += 3) douse(level, p.position().add(flat.scale(i)), 3, 3);
        douse(level, p.position(), 2, 2);
        for (LivingEntity e : around(level, p.position(), 8, p)) {
            Vec3 to = e.position().subtract(p.position()).normalize();
            if (to.dot(look) < 0.3) continue;
            e.push(look.x * 2.0, 0.4, look.z * 2.0);
            e.hurtMarked = true;
            e.clearFire();
            e.hurt(p.damageSources().indirectMagic(p, p), SpellMastery.dmg(p, 4f));
        }
        for (int i = 1; i <= 8; i++) {
            Vec3 at = p.position().add(look.x * i, 0.5, look.z * i);
            level.sendParticles(ParticleTypes.SPLASH, at.x, at.y, at.z, 20, 1.2, 0.4, 1.2, 0.1);
            level.sendParticles(ParticleTypes.FALLING_WATER, at.x, at.y + 1, at.z, 10, 1.2, 0.4, 1.2, 0);
        }
        sound(level, p.position(), SoundEvents.PLAYER_SPLASH_HIGH_SPEED, 1.5f);
    }

    /**
     * Campo Minado: se forma una bolita de fuego en la mano, el mago la lanza y salen las minas hacia el cielo, suben
     * por encima del punto marcado, caen en abanico y quedan armadas en el suelo: estallan cuando alguien las pisa o
     * cuando se les acaba el tiempo.
     */
    private static void minefield(ServerPlayer p, ServerLevel level) {
        Vec3 c = targetPoint(p, 40);
        int charge = 12;
        // 1) bolita de fuego en la mano (sigue la mano mientras se carga)
        Tasks.repeat(0, 1, charge, t -> {
            Vec3 hand = hand(p);
            float s = 0.04f + t * 0.012f;
            level.sendParticles(ParticleTypes.FLAME, hand.x, hand.y, hand.z, 3, s, s, s, 0.005);
            if (t % 3 == 0) level.sendParticles(ParticleTypes.SMALL_FLAME, hand.x, hand.y, hand.z, 2, s * 1.5, s * 1.5, s * 1.5, 0.01);
            if (t == 0) level.playSound(null, hand.x, hand.y, hand.z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.5f, 1.6f);
            return p.isAlive();
        });
        int n = 8;
        Tasks.later(charge, () -> {
            p.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
            Vec3 from = hand(p);
            level.playSound(null, from.x, from.y, from.z, SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1f, 1.3f);
            for (int i = 0; i < n; i++) {
                double ox = (p.getRandom().nextDouble() - 0.5) * 12, oz = (p.getRandom().nextDouble() - 0.5) * 12;
                Vec3 land = ground(level, c.add(ox, 0, oz));
                Vec3 apex = c.add(ox * 0.35, 14 + p.getRandom().nextDouble() * 3, oz * 0.35);
                int up = 18 + i, down = 12;
                final int idx = i;
                // 2) sube desde la mano hasta arriba del objetivo y 3) cae a su lugar
                Tasks.repeat(i, 1, up + down, t -> {
                    Vec3 at;
                    if (t < up) {
                        double u = (t + 1) / (double) up, k = 1 - u;
                        Vec3 ctrl = from.add(apex).scale(0.5).add(0, 10, 0);
                        at = from.scale(k * k).add(ctrl.scale(2 * k * u)).add(apex.scale(u * u));
                    } else {
                        double u = (t - up + 1) / (double) down;
                        at = apex.add(land.subtract(apex).scale(u * u));
                    }
                    level.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 4, 0.08, 0.08, 0.08, 0.01);
                    level.sendParticles(ParticleTypes.SMOKE, at.x, at.y, at.z, 1, 0.05, 0.05, 0.05, 0.0);
                    if (t == up) level.playSound(null, at.x, at.y, at.z, SoundEvents.FIREWORK_ROCKET_TWINKLE_FAR, SoundSource.PLAYERS, 0.4f, 1.6f);
                    if (t == up + down - 1) arm(p, level, land, 40 + idx * 6);
                    return true;
                });
            }
        });
    }

    /** Mina armada en el suelo: humea y brilla; estalla si alguien la pisa o al acabarse el tiempo. */
    private static void arm(ServerPlayer p, ServerLevel level, Vec3 at, int fuse) {
        level.playSound(null, at.x, at.y, at.z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.5f, 0.8f);
        boolean[] done = {false};
        float power = 2f + 0.25f * (SpellMastery.castLevel(p) / 5);
        Tasks.repeat(0, 2, fuse, t -> {
            if (done[0]) return false;
            level.sendParticles(ParticleTypes.SMALL_FLAME, at.x, at.y + 0.1, at.z, 2, 0.12, 0.02, 0.12, 0.0);
            if (t % 6 == 0) level.sendParticles(ParticleTypes.SMOKE, at.x, at.y + 0.2, at.z, 2, 0.1, 0.05, 0.1, 0.01);
            boolean stepped = !around(level, at, 1.3, p).isEmpty();
            if (stepped || t >= fuse - 2) {
                done[0] = true;
                level.explode(p, at.x, at.y, at.z, power, Level.ExplosionInteraction.MOB);
                return false;
            }
            return true;
        });
    }

    /** Mano derecha del jugador (adelante y un poco abajo de los ojos). */
    private static Vec3 hand(ServerPlayer p) {
        Vec3 look = p.getLookAngle();
        Vec3 right = new Vec3(-look.z, 0, look.x).normalize();
        if (p.getMainArm() == net.minecraft.world.entity.HumanoidArm.LEFT) right = right.scale(-1);
        return p.getEyePosition().add(look.scale(0.6)).add(right.scale(0.35)).add(0, -0.45, 0);
    }

    /** Baja el punto hasta el primer bloque solido (busca hasta 10 abajo y 4 arriba). */
    private static Vec3 ground(ServerLevel level, Vec3 v) {
        BlockPos b = BlockPos.containing(v.x, v.y + 4, v.z);
        for (int i = 0; i < 14; i++) {
            if (!level.getBlockState(b.below()).getCollisionShape(level, b.below()).isEmpty() && level.getBlockState(b).getCollisionShape(level, b).isEmpty())
                return new Vec3(v.x, b.getY(), v.z);
            b = b.below();
        }
        return v;
    }

    /**
     * Inundacion apaga todo lo encendido en su alcance: fuego (comun, de alma, sobre netherrack), fogatas, velas y
     * tortas con vela. Los portales no se tocan.
     */
    private static void douse(ServerLevel level, Vec3 c, int r, int h) {
        BlockPos base = BlockPos.containing(c);
        boolean any = false;
        for (BlockPos pos : BlockPos.betweenClosed(base.offset(-r, -2, -r), base.offset(r, h, r))) {
            BlockState st = level.getBlockState(pos);
            if (st.getBlock() instanceof net.minecraft.world.level.block.BaseFireBlock) {
                level.removeBlock(pos, false);
                any = true;
            } else if (st.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT)
                    && st.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT)
                    && (st.getBlock() instanceof net.minecraft.world.level.block.CampfireBlock
                        || st.getBlock() instanceof net.minecraft.world.level.block.AbstractCandleBlock)) {
                if (st.getBlock() instanceof net.minecraft.world.level.block.CampfireBlock)
                    net.minecraft.world.level.block.CampfireBlock.dowse(null, level, pos.immutable(), st);
                level.setBlock(pos, st.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT, false), 11);
                any = true;
            }
        }
        if (any) level.playSound(null, c.x, c.y, c.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1f, 1f);
    }

    /** Lanza de tierra: una fila de pinchos de piedra sale del suelo hacia adelante. */
    private static void earthLance(ServerPlayer p, ServerLevel level) {
        Vec3 look = new Vec3(p.getLookAngle().x, 0, p.getLookAngle().z).normalize();
        for (int i = 1; i <= 8; i++) {
            final int step = i;
            Tasks.later(i * 2, () -> {
                Vec3 at = p.position().add(look.scale(step));
                net.zennin.mushoku.entity.SpellFxEntity.spawn(level, "earth_spike", at, step * 47f, 1.6f, 50, null);
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()),
                        at.x, at.y + 0.5, at.z, 25, 0.4, 0.6, 0.4, 0.1);
                for (LivingEntity e : around(level, at, 1.4, p)) {
                    e.hurt(p.damageSources().indirectMagic(p, p), SpellMastery.dmg(p, 6f));
                    e.push(0, 0.7, 0);
                    e.hurtMarked = true;
                }
            });
        }
        sound(level, p.position(), SoundEvents.POINTED_DRIPSTONE_PLACE, 1.5f);
    }
}
