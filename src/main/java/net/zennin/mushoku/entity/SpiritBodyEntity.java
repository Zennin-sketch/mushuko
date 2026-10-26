package net.zennin.mushoku.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.zennin.mushoku.extra.DemonEyes;

import java.util.Optional;
import java.util.UUID;

/**
 * El cuerpo que queda quieto donde estabas mientras usas el ojo de Clarividencia (modo espectador).
 * Se ve con tu skin y tu equipo. Si algo lo golpea, volves a tu cuerpo y recibis ese dano.
 */
public class SpiritBodyEntity extends PathfinderMob {
    private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(SpiritBodyEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    public SpiritBodyEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setNoAi(true);
    }

    public static AttributeSupplier.Builder attributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 20).add(Attributes.MOVEMENT_SPEED, 0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(OWNER, Optional.empty());
    }

    public Optional<UUID> owner() { return entityData.get(OWNER); }

    public void setOwner(UUID id) { entityData.set(OWNER, Optional.of(id)); }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || isInvulnerableTo(source)) return false;
        ServerPlayer p = owner().map(id -> level().getServer() == null ? null : level().getServer().getPlayerList().getPlayer(id)).orElse(null);
        if (p == null) { discard(); return false; }
        DemonEyes.bodyHit(p, source, amount);
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        // sin dueno (se desconecto o ya volvio) el cuerpo desaparece
        if (!level().isClientSide && tickCount % 20 == 0 && !DemonEyes.hasBody(this)) discard();
    }

    @Override
    public boolean isPushable() { return false; }

    @Override
    protected void doPush(Entity entity) {}

    @Override
    public boolean removeWhenFarAway(double dist) { return false; }

    @Override
    public boolean shouldBeSaved() { return false; }
}
