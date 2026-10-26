package net.zennin.mushoku.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.zennin.mushoku.archer.BowSystem;
import net.zennin.mushoku.item.MushokuBowItem;
import net.zennin.mushoku.network.ActionPayload;
import org.lwjgl.glfw.GLFW;

/**
 * Arcos del mod: R carga la flecha (si el arco esta descargado) y, ya cargado, mantener el clic izquierdo tensa como
 * el clic derecho de un arco normal (que tambien sigue funcionando). Se corre antes de que Minecraft lea las teclas.
 */
public final class BowClient {
    private static boolean reloadPrev, attackDraw;

    private BowClient() {}

    public static boolean holdingBow(Player p) {
        return p != null && p.getMainHandItem().getItem() instanceof MushokuBowItem;
    }

    public static void init() {
        ClientTickEvents.START_CLIENT_TICK.register(BowClient::tick);
    }

    private static boolean physicallyDown(KeyMapping k) {
        InputConstants.Key key = KeyBindingHelper.getBoundKeyOf(k);
        long win = Minecraft.getInstance().getWindow().getWindow();
        if (key.getType() == InputConstants.Type.MOUSE) return GLFW.glfwGetMouseButton(win, key.getValue()) == GLFW.GLFW_PRESS;
        if (key.getType() == InputConstants.Type.KEYSYM && key.getValue() != InputConstants.UNKNOWN.getValue())
            return InputConstants.isKeyDown(win, key.getValue());
        return k.isDown();
    }

    private static void tick(Minecraft mc) {
        LocalPlayer p = mc.player;
        if (p == null || mc.gameMode == null) { attackDraw = false; return; }
        boolean bow = holdingBow(p);
        // R: cargar
        boolean reload = ModKeys.isDown(ModKeys.BOW_RELOAD);
        while (ModKeys.BOW_RELOAD.consumeClick()) {}
        Long started = net.zennin.mushoku.client.render.BowRender.LOAD_START.get(p.getId());
        boolean busy = started != null && mc.level != null && mc.level.getGameTime() - started < BowSystem.LOAD_TICKS;
        if (reload && !reloadPrev && mc.screen == null && bow && !busy && !BowSystem.isLoaded(p.getMainHandItem()) && !p.isUsingItem()) {
            ClientPlayNetworking.send(new ActionPayload(ActionPayload.BOW_RELOAD));
            boolean hasArrows = p.getAbilities().instabuild || !p.getProjectile(p.getMainHandItem()).isEmpty();
            if (mc.level != null && hasArrows)
                net.zennin.mushoku.client.render.BowRender.LOAD_START.put(p.getId(), mc.level.getGameTime());
        }
        reloadPrev = reload;
        // clic izquierdo mantenido: tensar (con la flecha cargada); no golpea ni rompe bloques
        boolean attack = bow && mc.screen == null && mc.mouseHandler.isMouseGrabbed() && physicallyDown(mc.options.keyAttack);
        if (attack && (BowSystem.isLoaded(p.getMainHandItem()) || attackDraw)) {
            while (mc.options.keyAttack.consumeClick()) {}
            mc.options.keyAttack.setDown(false);
            if (!attackDraw && !p.isUsingItem()) {
                mc.gameMode.useItem(p, InteractionHand.MAIN_HAND);
                attackDraw = true;
            }
            if (attackDraw) mc.options.keyUse.setDown(true);
        } else if (attackDraw) {
            attackDraw = false;
            if (!physicallyDown(mc.options.keyUse)) mc.options.keyUse.setDown(false);
        }
    }
}
