package net.zennin.mushoku.companion;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.zennin.mushoku.data.ManaData;
import net.zennin.mushoku.data.ModAttachments;
import net.zennin.mushoku.data.SwordData;
import net.zennin.mushoku.network.CompanionStatePayload;
import net.zennin.mushoku.registry.ModItems;
import net.zennin.mushoku.sword.SwordSystem;

import java.util.Map;

/**
 * Interacciones con los personajes, al estilo de Minecraft Comes Alive:
 *  Hablar: saludar, charlar, chiste, cumplido, coquetear*, contar una historia.
 *  Interactuar: abrazo, beso*, regalo (el objeto de tu mano).
 *  Ordenes (con 40 corazones o casados): seguir, quedarse, andar libre, fijar casa, volver a casa.
 *  Especial: accion propia de cada personaje; casamiento* con Anillo de Bodas (100 corazones) y divorcio.
 * (*) solo con personajes adultos.
 */
public final class Interactions {
    public static final String MARRIED_TAG = "remushoku_married";
    private static final Map<String, Float> BASE = Map.of("greet", 0.9f, "chat", 0.7f, "joke", 0.6f, "compliment", 0.65f,
            "story", 0.65f, "flirt", 0.45f, "hug", 0.7f, "kiss", 0.75f);
    private static final Map<String, Integer> WIN = Map.of("greet", 1, "chat", 3, "joke", 4, "compliment", 4, "story", 4, "flirt", 6, "hug", 6, "kiss", 8);
    private static final Map<String, Integer> LOSE = Map.of("greet", 0, "chat", -2, "joke", -3, "compliment", -2, "story", -2, "flirt", -6, "hug", -5, "kiss", -8);
    /** Cantidad de variantes de frase por tema (las claves de idioma terminan en .1, .2). */
    private static final Map<String, Integer> VARIANTS = Map.ofEntries(
            Map.entry("greet.ok", 2), Map.entry("greet.bad", 2), Map.entry("chat.ok", 2), Map.entry("chat.bad", 2),
            Map.entry("joke.ok", 2), Map.entry("joke.bad", 2), Map.entry("compliment.ok", 2), Map.entry("compliment.bad", 2),
            Map.entry("story.ok", 2), Map.entry("story.bad", 2), Map.entry("flirt.ok", 2), Map.entry("flirt.bad", 2),
            Map.entry("hug.ok", 1), Map.entry("hug.bad", 1), Map.entry("kiss.ok", 1), Map.entry("kiss.bad", 1));

    private Interactions() {}

    public static void open(CompanionEntity c, ServerPlayer p) { sync(c, p, ""); }

    static void sync(CompanionEntity c, ServerPlayer p, String line) {
        Relation r = c.relation(p.getUUID());
        boolean married = p.getUUID().equals(c.spouse());
        ServerPlayNetworking.send(p, new CompanionStatePayload(new CompanionState(c.getId(), r.hearts, c.mood(), c.mode().ordinal(),
                married, c.spouse() != null && !married, c.kind.romance, c.home() != null, c.kind.id, line)));
    }

    /** Dice una frase (en el chat del jugador) y devuelve la clave usada. */
    public static String say(CompanionEntity c, ServerPlayer p, String topic) {
        int n = VARIANTS.getOrDefault(topic, 1);
        String key = "talk.remushoku." + c.kind.id + "." + topic + "." + (1 + c.getRandom().nextInt(n));
        p.sendSystemMessage(Component.literal("<").append(c.getDisplayName()).append("> ").withStyle(ChatFormatting.GOLD)
                .append(Component.translatable(key, p.getDisplayName()).withStyle(ChatFormatting.WHITE)));
        return key;
    }

    private static void particles(CompanionEntity c, ParticleOptions type, int n) {
        ((ServerLevel) c.level()).sendParticles(type, c.getX(), c.getY() + c.getBbHeight() + 0.3, c.getZ(), n, 0.3, 0.2, 0.3, 0.02);
    }

    public static void handle(ServerPlayer p, int entityId, String action) {
        Entity e = p.level().getEntity(entityId);
        if (!(e instanceof CompanionEntity c) || c.distanceToSqr(p) > 100 || !c.isAlive()) return;
        Relation r = c.relation(p.getUUID());
        boolean married = p.getUUID().equals(c.spouse());
        String line;
        switch (action) {
            case "greet", "chat", "joke", "compliment", "story", "flirt", "hug", "kiss" -> line = talk(c, p, r, action, married);
            case "gift" -> line = gift(c, p, r);
            case "follow", "stay", "free", "sethome", "gohome" -> line = command(c, p, r, action, married);
            case "special" -> line = special(c, p, r);
            case "marry" -> line = marry(c, p, r);
            case "divorce" -> {
                if (!married) return;
                c.setSpouse(null);
                p.removeTag(MARRIED_TAG);
                r.addHearts(-50);
                c.addMood(-8);
                line = say(c, p, "divorce");
            }
            default -> { return; }
        }
        sync(c, p, line);
    }

    private static String talk(CompanionEntity c, ServerPlayer p, Relation r, String type, boolean married) {
        boolean romantic = type.equals("flirt") || type.equals("kiss");
        if (romantic && !c.kind.romance) return "";
        float chance = BASE.get(type) * c.kind.affinity.getOrDefault(type, 1f) + r.hearts / 300f + c.mood() / 40f - r.fatigue(type) * 0.15f;
        if (married) chance += 0.2f;
        if (type.equals("flirt") && r.hearts < 20 && !married) chance *= 0.3f;
        chance = Math.max(0.05f, Math.min(0.95f, chance));
        // Abrazos y besos necesitan confianza: con pocos corazones casi siempre te rechazan
        if ((type.equals("hug") && r.hearts < 30 || type.equals("kiss") && r.hearts < 80) && !married) chance = 0.02f;
        boolean ok = c.getRandom().nextFloat() < chance;
        r.tire(type);
        r.addHearts(ok ? WIN.get(type) : LOSE.get(type));
        c.addMood(ok ? 1 : -1);
        if (ok) {
            particles(c, type.equals("kiss") || type.equals("flirt") ? ParticleTypes.HEART : ParticleTypes.HAPPY_VILLAGER, type.equals("kiss") ? 6 : 3);
            c.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.6f, 1.3f);
        } else {
            particles(c, ParticleTypes.ANGRY_VILLAGER, 2);
        }
        return say(c, p, type + (ok ? ".ok" : ".bad"));
    }

    private static String gift(CompanionEntity c, ServerPlayer p, Relation r) {
        ItemStack s = p.getMainHandItem();
        if (s.isEmpty()) {
            p.displayClientMessage(Component.translatable("msg.remushoku.gift_empty").withStyle(ChatFormatting.GRAY), true);
            return "";
        }
        if (s.is(ModItems.WEDDING_RING)) return marry(c, p, r);
        String topic;
        int tired = r.fatigue("gift");
        if (c.kind.likes.test(s)) {
            topic = "gift_like";
            r.addHearts(Math.max(2, 14 - tired * 3));
            c.addMood(3);
            particles(c, ParticleTypes.HEART, 4);
        } else if (c.kind.dislikes.test(s)) {
            topic = "gift_bad";
            r.addHearts(-8);
            c.addMood(-3);
            particles(c, ParticleTypes.ANGRY_VILLAGER, 3);
        } else {
            topic = "gift_ok";
            boolean valuable = s.is(Items.DIAMOND) || s.is(Items.EMERALD) || s.is(Items.GOLD_INGOT);
            r.addHearts(Math.max(1, (valuable ? 8 : 4) - tired * 2));
            c.addMood(1);
            particles(c, ParticleTypes.HAPPY_VILLAGER, 3);
        }
        r.tire("gift");
        if (!p.getAbilities().instabuild) s.shrink(1);
        return say(c, p, topic);
    }

    private static String command(CompanionEntity c, ServerPlayer p, Relation r, String cmd, boolean married) {
        if (r.hearts < 40 && !married) return say(c, p, "refuse");
        switch (cmd) {
            case "follow" -> c.setMode(CompanionEntity.Mode.FOLLOW, p);
            case "stay" -> c.setMode(CompanionEntity.Mode.STAY, p);
            case "free" -> c.setMode(CompanionEntity.Mode.FREE, p);
            case "sethome" -> c.setHome(c.blockPosition());
            case "gohome" -> {
                if (c.home() == null) return say(c, p, "nohome");
                c.setMode(CompanionEntity.Mode.FREE, p);
                c.getNavigation().moveTo(c.home().getX() + 0.5, c.home().getY(), c.home().getZ() + 0.5, 1.0);
            }
            default -> {}
        }
        return say(c, p, cmd);
    }

    private static String marry(CompanionEntity c, ServerPlayer p, Relation r) {
        if (!c.kind.romance) return "";
        ItemStack s = p.getMainHandItem();
        if (!s.is(ModItems.WEDDING_RING)) {
            p.displayClientMessage(Component.translatable("msg.remushoku.need_ring").withStyle(ChatFormatting.GRAY), true);
            return "";
        }
        if (p.getUUID().equals(c.spouse())) return say(c, p, "marry_already");
        if (c.spouse() != null || p.getTags().contains(MARRIED_TAG) || r.hearts < 100) {
            r.addHearts(-5);
            c.addMood(-2);
            return say(c, p, "marry_no");
        }
        c.setSpouse(p.getUUID());
        p.addTag(MARRIED_TAG);
        if (!p.getAbilities().instabuild) s.shrink(1);
        r.addHearts(20);
        c.addMood(10);
        particles(c, ParticleTypes.HEART, 20);
        c.level().playSound(null, c.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 1f, 1.2f);
        p.server.getPlayerList().broadcastSystemMessage(Component.translatable("msg.remushoku.wedding", p.getDisplayName(), c.getDisplayName())
                .withStyle(ChatFormatting.LIGHT_PURPLE), false);
        return say(c, p, "marry_yes");
    }

    /** Accion especial de cada personaje (una vez por dia del juego). */
    private static String special(CompanionEntity c, ServerPlayer p, Relation r) {
        long day = c.level().getDayTime() / 24000L;
        int need = switch (c.kind) { case KISHIRIKA -> 100; case SYLPHIETTE, SYLPHIETTE_YOUNG -> 30; default -> 50; };
        if (r.hearts < need) return say(c, p, "special_no");
        if (c.kind == CompanionKind.KISHIRIKA) {
            if (r.gotEye) return say(c, p, "special_done");
            r.gotEye = true;
            give(p, new ItemStack(ModItems.DEMON_EYE));
            particles(c, ParticleTypes.WITCH, 20);
            return say(c, p, "special");
        }
        if (r.lastSpecialDay == day) return say(c, p, "special_done");
        r.lastSpecialDay = day;
        switch (c.kind) {
            case ROXY, RUDEUS -> {
                ManaData m = ModAttachments.get(p);
                ModAttachments.set(p, m.withMaxMana(m.maxMana() + 10));
                net.zennin.mushoku.network.ModNetworking.syncMana(p);
                particles(c, ParticleTypes.ENCHANT, 30);
            }
            case ERIS, GHISLAINE, RUIJERD -> {
                SwordData d = ModAttachments.sword(p);
                ModAttachments.setSword(p, d.withPoints(d.points() + 5));
                SwordSystem.sync(p);
                particles(c, ParticleTypes.SWEEP_ATTACK, 4);
            }
            case SYLPHIETTE -> {
                p.heal(p.getMaxHealth());
                p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
                particles(c, ParticleTypes.HEART, 6);
            }
            case SYLPHIETTE_YOUNG -> give(p, new ItemStack(c.getRandom().nextBoolean() ? Items.POPPY : Items.DANDELION));
            default -> {}
        }
        return say(c, p, "special");
    }

    private static void give(ServerPlayer p, ItemStack s) {
        if (!p.getInventory().add(s)) p.drop(s, false);
    }
}
