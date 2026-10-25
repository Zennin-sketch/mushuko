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
 * /mushoku mana <n>, /mushoku maxmana <n>, /mushoku elemento <agua|fuego|tierra|viento> <1-6>, /mushoku clase
 */
public final class MushokuCommands {
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
                        .then(Commands.literal("clase").executes(c -> {
                            PlayerClasses.reset(c.getSource().getPlayerOrException());
                            return 1;
                        }))));
    }
}
