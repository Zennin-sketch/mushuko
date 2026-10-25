package net.zennin.mushoku.race;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.zennin.mushoku.MushokuMod;
import net.zennin.mushoku.data.ModAttachments;
import net.zennin.mushoku.entity.MagicProjectile;
import net.zennin.mushoku.magic.SpellCasting;
import net.zennin.mushoku.network.RaceSyncPayload;

/** Razas y habilidades del addon: stats, afinidad magica, pasivas, Esquivar, Piel de Mana y Factor Laplace. */
public final class RaceSystem {
    private static final ResourceLocation HP = MushokuMod.id("race_health"), SPEED = MushokuMod.id("race_speed"),
            ARMOR = MushokuMod.id("race_armor"), DMG = MushokuMod.id("race_damage"), ATK_SPEED = MushokuMod.id("race_attack_speed"),
            SCALE = MushokuMod.id("race_scale");
    private static final ThreadLocal<Boolean> REENTRY = ThreadLocal.withInitial(() -> false);

    private RaceSystem() {}

    public static RaceData data(Player p) { return ModAttachments.race(p); }

    public static void sync(ServerPlayer p) { ServerPlayNetworking.send(p, new RaceSyncPayload(data(p))); }

    /** Elegir raza (una sola vez; un admin puede cambiarla con /mushoku raza). */
    public static void choose(ServerPlayer p, String key, boolean force) {
        Race r = Race.byKey(key);
        RaceData d = data(p);
        if (r == null || (!d.race().isEmpty() && !force)) return;
        ModAttachments.setRace(p, d.withRace(r.key));
        refresh(p);
        p.setHealth(p.getMaxHealth());
        if (r == Race.SUPERD) {
            ItemStack spear = new ItemStack(net.zennin.mushoku.addon.AddonItems.SUPERD_SPEAR);
            if (!p.getInventory().add(spear)) p.drop(spear, false);
        }
        p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1f, 0.8f);
        p.displayClientMessage(Component.translatable("msg.remushoku.race_chosen", Component.translatable("race.remushoku." + r.key))
                .withStyle(ChatFormatting.GOLD), false);
        sync(p);
    }

    public static void toggle(ServerPlayer p, String what) {
        RaceData d = data(p);
        if (what.equals("dodge") && d.dodge()) d = d.withDodgeOn(!d.dodgeOn());
        else if (what.equals("mana_skin")) d = d.withManaSkinOn(!d.manaSkinOn());
        else return;
        ModAttachments.setRace(p, d);
        sync(p);
    }

    public static void grantDodge(ServerPlayer p) {
        ModAttachments.setRace(p, data(p).withDodge(true));
        p.level().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1f, 1f);
        p.displayClientMessage(Component.translatable("msg.remushoku.dodge_get").withStyle(ChatFormatting.AQUA), false);
        sync(p);
    }

    public static void grantLaplace(ServerPlayer p) {
        if (data(p).laplace()) return;
        ModAttachments.setRace(p, data(p).withLaplace(true));
        p.displayClientMessage(Component.translatable("msg.remushoku.laplace_get").withStyle(ChatFormatting.DARK_PURPLE), false);
        sync(p);
    }

    /** Multiplicador del daño de tus hechizos segun tu raza y el tipo de proyectil. */
    public static float affinity(Player p, int kind) {
        Race r = data(p).raceOrNull();
        if (r == null) return 1f;
        return (float) switch (kind) {
            case MagicProjectile.FIRE -> r.fire;
            case MagicProjectile.ROCK -> r.earth;
            case MagicProjectile.WIND -> r.wind;
            default -> r.water;
        };
    }

    public static void refresh(ServerPlayer p) {
        Race r = data(p).raceOrNull();
        set(p, Attributes.MAX_HEALTH, HP, r == null ? 0 : r.health - 20, AttributeModifier.Operation.ADD_VALUE);
        set(p, Attributes.MOVEMENT_SPEED, SPEED, r == null ? 0 : r.speed, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        set(p, Attributes.ARMOR, ARMOR, r == null ? 0 : r.armor, AttributeModifier.Operation.ADD_VALUE);
        set(p, Attributes.ATTACK_DAMAGE, DMG, r == null ? 0 : r.damage, AttributeModifier.Operation.ADD_VALUE);
        set(p, Attributes.ATTACK_SPEED, ATK_SPEED, r == null ? 0 : r.attackSpeed, AttributeModifier.Operation.ADD_VALUE);
        set(p, Attributes.SCALE, SCALE, r == null ? 0 : r.scale - 1, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }

    private static void set(ServerPlayer p, Holder<Attribute> attr, ResourceLocation id, double amount, AttributeModifier.Operation op) {
        AttributeInstance inst = p.getAttribute(attr);
        if (inst == null) return;
        AttributeModifier cur = inst.getModifier(id);
        if (amount == 0) { if (cur != null) inst.removeModifier(id); return; }
        if (cur != null && cur.amount() == amount) return;
        inst.addOrUpdateTransientModifier(new AttributeModifier(id, amount, op));
    }

    /** Tecla R: habilidad de raza. Elfo: bendicion del bosque (regeneracion a ti y aliados cercanos, 40 s de espera). */
    public static void raceSkill(ServerPlayer p) {
        RaceData d = data(p);
        Race r = d.raceOrNull();
        if (r != Race.ELF) {
            p.displayClientMessage(Component.translatable("msg.remushoku.race_no_skill").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        long now = p.serverLevel().getGameTime();
        if (d.skillCooldownUntil() > now) {
            p.displayClientMessage(Component.translatable("msg.remushoku.race_cooldown", (d.skillCooldownUntil() - now + 19) / 20).withStyle(ChatFormatting.GRAY), true);
            return;
        }
        for (LivingEntity e : p.serverLevel().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(5),
                e -> e == p || e instanceof Player || e instanceof net.zennin.mushoku.companion.CompanionEntity || e instanceof net.minecraft.world.entity.animal.Animal))
            e.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
        p.serverLevel().sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX(), p.getY() + 1, p.getZ(), 40, 2.5, 1, 2.5, 0);
        p.level().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.4f);
        ModAttachments.setRace(p, d.withCooldown(now + 800));
    }

    public static void init() {
        ServerPlayConnectionEvents.JOIN.register((h, s, server) -> { refresh(h.getPlayer()); sync(h.getPlayer()); });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 20 != 0) return;
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                refresh(p);
                // Los elfos ven en la oscuridad
                if (data(p).raceOrNull() == Race.ELF && server.getTickCount() % 100 == 0)
                    p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, true, false, true));
            }
        });
        // Enanos: 20% de sacar el doble al picar menas
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, be) -> {
            if (!(player instanceof ServerPlayer sp) || data(sp).raceOrNull() != Race.DWARF || !state.is(ConventionalBlockTags.ORES)) return;
            if (sp.getRandom().nextFloat() >= 0.2f) return;
            for (ItemStack drop : Block.getDrops(state, (ServerLevel) level, pos, be, sp, sp.getMainHandItem()))
                Block.popResource(level, pos, drop);
            ((ServerLevel) level).sendParticles(ParticleTypes.WAX_ON, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8, 0.3, 0.3, 0.3, 0);
        });
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(RaceSystem::allowDamage);
    }

    private static boolean allowDamage(LivingEntity victim, DamageSource source, float amount) {
        if (REENTRY.get() || !(victim instanceof ServerPlayer p) || source.getEntity() == null) return true;
        RaceData d = data(p);
        // Esquivar: 10% de evitar el golpe
        if (d.dodge() && d.dodgeOn() && p.getRandom().nextFloat() < 0.10f) {
            p.level().playSound(null, p.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_1.value(), SoundSource.PLAYERS, 0.5f, 1.5f);
            p.displayClientMessage(Component.translatable("msg.remushoku.dodged").withStyle(ChatFormatting.AQUA), true);
            return false;
        }
        // Piel de Mana (magos): 10 / 15 / 20 / 25% menos daño segun el mana maximo
        int lvl = RaceData.manaSkinLevel(ModAttachments.get(p).maxMana());
        if (lvl > 0 && d.manaSkinOn() && ModAttachments.get(p).playerClass() == SpellCasting.CLASS_MAGE) {
            float mult = 1f - (0.05f + 0.05f * lvl);
            REENTRY.set(true);
            try { p.hurt(source, amount * mult); } finally { REENTRY.set(false); }
            return false;
        }
        return true;
    }
}
