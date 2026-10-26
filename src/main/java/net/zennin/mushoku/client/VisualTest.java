package net.zennin.mushoku.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.zennin.mushoku.MushokuMod;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Prueba visual automatica (solo para desarrollo, se activa con -Dmushoku.visualtest=ruta/escenario.json).
 * Crea un mundo, pone un "maniqui" (otro jugador) delante de la camara y sigue los pasos del escenario:
 * comandos, poses, animaciones, equipo, caminar y capturas de pantalla. Al terminar cierra el juego.
 * Sirve para ver en GitHub Actions como se ven animaciones y armaduras sin abrir el juego a mano.
 */
public final class VisualTest {
    public static final String PROP = "mushoku.visualtest";
    /** Skin que usan todos los jugadores durante la prueba (con capa externa para probar 3D Skin Layers). */
    public static ResourceLocation skin;

    private static JsonArray steps;
    private static int index, wait, ticksInWorld, walkTicks, dummyYaw;
    private static boolean created;
    private static RemotePlayer dummy;

    private VisualTest() {}

    public static boolean active() { return System.getProperty(PROP) != null; }

    public static void init() {
        if (!active()) return;
        try {
            JsonObject root = JsonParser.parseString(Files.readString(Path.of(System.getProperty(PROP)))).getAsJsonObject();
            steps = root.getAsJsonArray("steps");
            if (root.has("skin")) skin = ResourceLocation.parse(root.get("skin").getAsString());
        } catch (Exception e) {
            MushokuMod.LOG.error("[visualtest] no se pudo leer el escenario", e);
            return;
        }
        ClientTickEvents.END_CLIENT_TICK.register(VisualTest::tick);
    }

    private static boolean keepScreen;

    private static void log(String s) { MushokuMod.LOG.info("[visualtest] " + s); }

    private static void tick(Minecraft mc) {
        if (!created && mc.screen instanceof TitleScreen) {
            created = true;
            log("creando mundo");
            LevelSettings settings = new LevelSettings("visualtest", GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
                    new GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel("visualtest", settings, new WorldOptions(20261026L, false, false),
                    WorldPresets::createNormalWorldDimensions, mc.screen);
            return;
        }
        if (mc.player == null || mc.level == null) return;
        if (mc.screen != null && !keepScreen) mc.setScreen(null);
        ticksInWorld++;
        if (ticksInWorld < 60) return;
        keepDummy(mc);
        if (wait > 0) { wait--; return; }
        if (steps == null || index >= steps.size()) {
            log("fin");
            mc.stop();
            return;
        }
        JsonObject s = steps.get(index++).getAsJsonObject();
        String op = s.get("op").getAsString();
        log("paso " + index + ": " + s);
        try {
            run(mc, op, s);
        } catch (Exception e) {
            MushokuMod.LOG.error("[visualtest] fallo el paso " + s, e);
        }
    }

    private static void run(Minecraft mc, String op, JsonObject s) throws Exception {
        switch (op) {
            case "cmd" -> mc.player.connection.sendCommand(s.get("v").getAsString());
            case "wait" -> wait = s.get("t").getAsInt();
            case "setup" -> {
                // Plataforma plana, de dia, camara fija en primera persona sin mano ni interfaz
                mc.options.hideGui = true;
                mc.options.bobView().set(false);
                mc.gameRenderer.setRenderHand(false);
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                mc.options.fov().set(s.has("fov") ? s.get("fov").getAsInt() : 50);
                for (String c : new String[]{"gamerule doDaylightCycle false", "time set 6000", "weather clear",
                        "gamerule doWeatherCycle false", "tp @s 0 120 0 0 0",
                        "fill -8 119 -6 8 119 12 minecraft:smooth_stone", "fill -8 120 -6 8 126 12 minecraft:air"})
                    mc.player.connection.sendCommand(c);
                mc.player.getAbilities().flying = true;
                wait = 20;
            }
            case "dummy" -> {
                if (dummy == null) {
                    dummy = new RemotePlayer(mc.level, new GameProfile(UUID.nameUUIDFromBytes("vt".getBytes()), "Maniqui"));
                    mc.level.addEntity(dummy);
                    // todas las capas de la skin visibles (chaqueta, mangas, pantalon, sombrero)
                    dummy.getEntityData().set(net.zennin.mushoku.mixin.PlayerAccessor.remushoku$customisation(), (byte) 0x7F);
                }
                dummyYaw = s.has("yaw") ? s.get("yaw").getAsInt() : 180;
                double dist = s.has("dist") ? s.get("dist").getAsDouble() : 3.2;
                dummy.setPos(0.5, 120, 0.5 + dist);
                if (s.has("pitch")) dummy.setXRot(s.get("pitch").getAsFloat());
                wait = 2;
            }
            case "cam" -> {
                // camara: alto de los ojos y mirada (pitch); 'h' sube/baja la camara
                double h = s.has("h") ? s.get("h").getAsDouble() : 0;
                float pitch = s.has("pitch") ? s.get("pitch").getAsFloat() : 0;
                mc.player.connection.sendCommand(String.format(java.util.Locale.ROOT, "tp @s 0.5 %.2f 0.5 0 %.1f", 120 + h, pitch));
                mc.player.getAbilities().flying = true;
                wait = 3;
            }
            case "equip" -> {
                EquipmentSlot slot = EquipmentSlot.byName(s.get("slot").getAsString());
                String id = s.get("v").getAsString();
                dummy.setItemSlot(slot, id.isEmpty() ? ItemStack.EMPTY
                        : new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id))));
            }
            case "anim" -> {
                Class<?> c = Class.forName("net.zennin.mushokucombat.client.CombatAnimations");
                c.getMethod("play", AbstractClientPlayer.class, String.class, int.class).invoke(null, dummy, s.get("v").getAsString(), 0);
            }
            case "ready" -> ClientReadySpell.set(dummy.getId(), s.get("v").getAsString());
            case "walk" -> walkTicks = s.get("t").getAsInt();
            case "use" -> {
                // usar el item de la mano (para arcos): mantiene 'useTicks' ticks
                ((net.zennin.mushoku.mixin.LivingEntityAccessor) dummy).remushoku$setFlag(2, false);
                ((net.zennin.mushoku.mixin.LivingEntityAccessor) dummy).remushoku$setFlag(1, true);
            }
            case "stopuse" -> ((net.zennin.mushoku.mixin.LivingEntityAccessor) dummy).remushoku$setFlag(1, false);
            case "shot" -> {
                String name = s.get("v").getAsString();
                Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), m -> log("captura " + name));
            }
            case "oneshot" -> Class.forName("net.zennin.mushokucombat.client.BowAnimClient")
                    .getMethod("oneShot", AbstractClientPlayer.class, String.class).invoke(null, dummy, s.get("v").getAsString());
            case "loadbow" -> net.zennin.mushoku.archer.BowSystem.setLoaded(dummy.getMainHandItem(), !s.has("v") || s.get("v").getAsBoolean());
            case "quiver" -> {
                // carcaj en la ranura del maniqui (y del jugador si 'me'), con n flechas de un tipo
                ItemStack q = new ItemStack(net.zennin.mushoku.registry.ModItems.QUIVER);
                int n = s.has("n") ? s.get("n").getAsInt() : 0;
                java.util.List<ItemStack> list = new java.util.ArrayList<>();
                if (n > 0) list.add(new ItemStack(net.minecraft.world.item.Items.ARROW, Math.min(64, n)));
                if (s.has("spectral")) list.add(new ItemStack(net.minecraft.world.item.Items.SPECTRAL_ARROW, s.get("spectral").getAsInt()));
                if (!list.isEmpty()) q.set(net.minecraft.core.component.DataComponents.BUNDLE_CONTENTS, new net.minecraft.world.item.component.BundleContents(list));
                if (s.has("me")) {
                    var sp = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                    ItemStack qq = q.copy();
                    mc.getSingleplayerServer().execute(() -> { net.zennin.mushoku.data.ModAttachments.setQuiver(sp, qq); net.zennin.mushoku.archer.QuiverSystem.broadcast(sp); });
                } else net.zennin.mushoku.data.ModAttachments.setQuiver(dummy, q);
            }
            case "fp" -> {
                boolean on = s.get("v").getAsBoolean();
                mc.gameRenderer.setRenderHand(on);
                mc.options.hideGui = !on; // la mano en primera persona no se dibuja con la interfaz oculta
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                if (s.has("fov")) mc.options.fov().set(s.get("fov").getAsInt());
            }
            case "reload" -> net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
                    new net.zennin.mushoku.network.ActionPayload(net.zennin.mushoku.network.ActionPayload.BOW_RELOAD));
            case "fpuse" -> {
                mc.gameMode.useItem(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND);
                mc.options.keyUse.setDown(true);
            }
            case "fprelease" -> mc.options.keyUse.setDown(false);
            case "inv" -> {
                keepScreen = s.get("v").getAsBoolean();
                mc.options.hideGui = !keepScreen;
                if (keepScreen) { mc.options.guiScale().set(4); mc.resizeDisplay(); }
                if (keepScreen) mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
                else mc.setScreen(null);
            }
            default -> log("paso desconocido " + op);
        }
    }

    /** El maniqui no se mueve solo: cada tick se le fija la rotacion y, si camina, la animacion de las piernas. */
    private static void keepDummy(Minecraft mc) {
        if (dummy == null) return;
        dummy.setYRot(dummyYaw);
        dummy.yRotO = dummyYaw;
        dummy.yBodyRot = dummyYaw;
        dummy.yBodyRotO = dummyYaw;
        dummy.yHeadRot = dummyYaw;
        dummy.yHeadRotO = dummyYaw;
        if (walkTicks > 0) {
            walkTicks--;
            dummy.walkAnimation.update(0.55f, 1.0f);
        }
    }
}
