package net.zennin.mushoku.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.zennin.mushoku.data.ModAttachments;
import net.zennin.mushoku.magic.Element;
import net.zennin.mushoku.magic.PlayerClasses;
import net.zennin.mushoku.network.ModNetworking;

/**
 * Comandos de prueba (requieren permisos de operador):
 * /mushoku mana <n>, /mushoku puntos <n> (espadachin), /mushoku puntosarquero <n>, /mushoku maxmana <n>, /mushoku elemento <agua|fuego|tierra|viento> <1-6>, /mushoku clase
 */
public final class MushokuCommands {
    /** Autocompletar con TAB los nombres de los hechizos. */
    private static final com.mojang.brigadier.suggestion.SuggestionProvider<net.minecraft.commands.CommandSourceStack> SPELLS =
            (c, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                    java.util.Arrays.stream(net.zennin.mushoku.magic.Spell.values()).map(sp -> sp.id), b);

    private MushokuCommands() {}

    public static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                Commands.literal("mushoku").requires(s -> s.hasPermission(2))
                        .then(Commands.literal("mana").then(Commands.argument("valor", DoubleArgumentType.doubleArg(0)).executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            var d = ModAttachments.get(p);
                            ModAttachments.set(p, d.withMana(DoubleArgumentType.getDouble(c, "valor")));
                            ModNetworking.syncMana(p);
                            return 1;
                        })))
                        .then(Commands.literal("maxmana").then(Commands.argument("valor", DoubleArgumentType.doubleArg(1)).executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            double v = DoubleArgumentType.getDouble(c, "valor");
                            var d = ModAttachments.get(p);
                            ModAttachments.set(p, new net.zennin.mushoku.data.ManaData(v, v, d.playerClass()));
                            ModNetworking.syncMana(p);
                            return 1;
                        })))
                        .then(Commands.literal("elemento").then(Commands.argument("elemento", StringArgumentType.word())
                                .suggests((c, b) -> { for (String s : new String[]{"agua", "fuego", "tierra", "viento"}) b.suggest(s); return b.buildFuture(); })
                                .then(Commands.argument("nivel", IntegerArgumentType.integer(1, 6)).executes(c -> {
                                    ServerPlayer p = c.getSource().getPlayerOrException();
                                    Element e = switch (StringArgumentType.getString(c, "elemento")) {
                                        case "agua" -> Element.WATER;
                                        case "fuego" -> Element.FIRE;
                                        case "tierra" -> Element.EARTH;
                                        case "viento" -> Element.WIND;
                                        default -> null;
                                    };
                                    if (e == null) {
                                        c.getSource().sendFailure(Component.literal("Elemento invalido"));
                                        return 0;
                                    }
                                    ModAttachments.setMagic(p, ModAttachments.magic(p).withLevel(e, IntegerArgumentType.getInteger(c, "nivel")));
                                    ModNetworking.syncMagic(p);
                                    return 1;
                                }))))
                        .then(Commands.literal("puntos").then(Commands.argument("valor", IntegerArgumentType.integer(0)).executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            ModAttachments.setSword(p, ModAttachments.sword(p).withPoints(IntegerArgumentType.getInteger(c, "valor")));
                            net.zennin.mushoku.sword.SwordSystem.sync(p);
                            return 1;
                        })))
                        .then(Commands.literal("puntosarquero").then(Commands.argument("valor", IntegerArgumentType.integer(0)).executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            ModAttachments.setArcher(p, ModAttachments.archer(p).withPoints(IntegerArgumentType.getInteger(c, "valor")));
                            net.zennin.mushoku.archer.ArcherSystem.sync(p);
                            c.getSource().sendSuccess(() -> Component.literal("Puntos de arquero: " + IntegerArgumentType.getInteger(c, "valor")), false);
                            return 1;
                        })))
                        .then(Commands.literal("raza").then(Commands.argument("raza", StringArgumentType.word())
                                .suggests((c, b) -> { for (var r : net.zennin.mushoku.race.Race.values()) b.suggest(r.key); return b.buildFuture(); })
                                .executes(c -> {
                                    net.zennin.mushoku.race.RaceSystem.choose(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "raza"), true);
                                    return 1;
                                })))
                        .then(Commands.literal("laplace").executes(c -> {
                            net.zennin.mushoku.race.RaceSystem.grantLaplace(c.getSource().getPlayerOrException());
                            return 1;
                        }))
                        .then(Commands.literal("hitogami")
                                .then(Commands.literal("sueno").executes(c -> {
                                    net.zennin.mushoku.hitogami.HitogamiSystem.dream(c.getSource().getPlayerOrException());
                                    return 1;
                                }))
                                .then(Commands.literal("reiniciar").executes(c -> {
                                    // Borra raza, clase y rasgo y vuelve a empezar con Hitogami (para probar la creacion de personaje)
                                    ServerPlayer p = c.getSource().getPlayerOrException();
                                    ModAttachments.setRace(p, ModAttachments.race(p).withRace(""));
                                    ModAttachments.set(p, ModAttachments.get(p).withClass(0));
                                    net.zennin.mushoku.sword.SwordSystem.reset(p);
                                    ModNetworking.syncMana(p);
                                    net.zennin.mushoku.race.RaceSystem.refresh(p);
                                    net.zennin.mushoku.race.RaceSystem.sync(p);
                                    net.zennin.mushoku.hitogami.HitogamiSystem.setAppearance(p, net.zennin.mushoku.race.Appearance.NONE);
                                    ModAttachments.setHitogami(p, net.zennin.mushoku.hitogami.HitogamiData.DEFAULT);
                                    net.zennin.mushoku.hitogami.HitogamiSystem.beginIntro(p);
                                    return 1;
                                })))
                        .then(Commands.literal("clase").executes(c -> {
                            PlayerClasses.reset(c.getSource().getPlayerOrException());
                            return 1;
                        }))
                        // lanzar un hechizo por su nombre (pruebas): /mushoku lanzar quagmire
                        .then(Commands.literal("lanzar").then(Commands.argument("hechizo", StringArgumentType.word()).suggests(SPELLS).executes(c -> {
                            net.zennin.mushoku.magic.Spell sp = net.zennin.mushoku.magic.Spell.byId(StringArgumentType.getString(c, "hechizo"));
                            if (sp == null) { c.getSource().sendFailure(Component.literal("Hechizo desconocido")); return 0; }
                            return net.zennin.mushoku.magic.SpellEffects.cast(sp, c.getSource().getPlayerOrException()) ? 1 : 0;
                        })))
                        // aprender u olvidar hechizos (pruebas): /mushoku aprender fireball | /mushoku aprender todos
                        .then(Commands.literal("aprender").then(Commands.argument("hechizo", StringArgumentType.word())
                                .suggests((c, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(java.util.stream.Stream.concat(
                                        java.util.stream.Stream.of("todos"), java.util.Arrays.stream(net.zennin.mushoku.magic.Spell.values()).map(sp -> sp.id)), b))
                                .executes(c -> {
                            var pl = c.getSource().getPlayerOrException();
                            String id = StringArgumentType.getString(c, "hechizo");
                            if (id.equals("todos")) {
                                net.zennin.mushoku.data.ModAttachments.setKnownSpells(pl, java.util.Arrays.stream(net.zennin.mushoku.magic.Spell.values()).map(sp -> sp.id).toList());
                                net.zennin.mushoku.magic.SpellMastery.sync(pl);
                                c.getSource().sendSuccess(() -> Component.literal("Todos los hechizos aprendidos"), false);
                                return 1;
                            }
                            net.zennin.mushoku.magic.Spell sp = net.zennin.mushoku.magic.Spell.byId(id);
                            if (sp == null) { c.getSource().sendFailure(Component.literal("Hechizo desconocido")); return 0; }
                            return net.zennin.mushoku.magic.SpellKnowledge.learn(pl, sp) ? 1 : 0;
                        })))
                        .then(Commands.literal("olvidar").then(Commands.argument("hechizo", StringArgumentType.word()).suggests(SPELLS).executes(c -> {
                            net.zennin.mushoku.magic.Spell sp = net.zennin.mushoku.magic.Spell.byId(StringArgumentType.getString(c, "hechizo"));
                            if (sp == null) { c.getSource().sendFailure(Component.literal("Hechizo desconocido")); return 0; }
                            net.zennin.mushoku.magic.SpellKnowledge.forget(c.getSource().getPlayerOrException(), sp);
                            return 1;
                        })))
                        // fijar el nivel de un hechizo (pruebas): /mushoku maestria nuclear_strike 20 (pide el mana maximo del nivel)
                        .then(Commands.literal("maestria").then(Commands.argument("hechizo", StringArgumentType.word()).suggests(SPELLS)
                                .then(Commands.argument("nivel", IntegerArgumentType.integer(1, 20)).executes(c -> {
                            net.zennin.mushoku.magic.Spell sp = net.zennin.mushoku.magic.Spell.byId(StringArgumentType.getString(c, "hechizo"));
                            if (sp == null) { c.getSource().sendFailure(Component.literal("Hechizo desconocido")); return 0; }
                            var pl = c.getSource().getPlayerOrException();
                            int lvl = IntegerArgumentType.getInteger(c, "nivel");
                            java.util.Map<String, Integer> m = new java.util.HashMap<>(net.zennin.mushoku.data.ModAttachments.spellXp(pl));
                            m.put(sp.id, net.zennin.mushoku.magic.SpellMastery.usesFor(sp, lvl));
                            net.zennin.mushoku.data.ModAttachments.setSpellXp(pl, m);
                            net.zennin.mushoku.magic.SpellKnowledge.learn(pl, sp);
                            int gate = net.zennin.mushoku.magic.SpellMastery.gate(sp, lvl);
                            var md = net.zennin.mushoku.data.ModAttachments.get(pl);
                            if (md.maxMana() < gate) net.zennin.mushoku.data.ModAttachments.set(pl, md.withMaxMana(gate));
                            net.zennin.mushoku.network.ModNetworking.syncMana(pl);
                            net.zennin.mushoku.magic.SpellMastery.sync(pl);
                            c.getSource().sendSuccess(() -> Component.literal(sp.id + " -> nivel " + lvl), false);
                            return 1;
                        }))))));
    }
}
