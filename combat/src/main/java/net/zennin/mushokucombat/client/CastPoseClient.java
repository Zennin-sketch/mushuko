package net.zennin.mushokucombat.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.zennin.mushoku.client.CastingClient;
import net.zennin.mushoku.client.ClientManaState;
import net.zennin.mushoku.client.ClientSwordState;
import net.zennin.mushoku.client.ModKeys;
import net.zennin.mushokucombat.net.PoseStatePayload;

/**
 * Pose de conjuro (inspirada en Rudeus): mientras el modo conjuro esta activo el personaje extiende el brazo hacia
 * adelante con el baston; al lanzar (Tab) hace el empuje. Los demas jugadores la ven gracias al servidor.
 * Mas adelante cada hechizo puede tener su propia animacion: se elige aca segun el hechizo preparado.
 */
public final class CastPoseClient {
    private static boolean posed = false, shootPrev = false;

    private CastPoseClient() {}

    static void tick(Minecraft mc) {
        LocalPlayer p = mc.player;
        if (p == null) {
            posed = false;
            return;
        }
        boolean mage = net.zennin.mushoku.magic.SpellCasting.isMageClass(ClientManaState.playerClass)
                || (ClientManaState.playerClass == 2 && ClientSwordState.data.knows("ng_magic"));
        boolean shoot = mc.screen == null && ModKeys.isDown(ModKeys.SHOOT);
        if (mage && shoot && !shootPrev && (CastingClient.casting || posed || !ClientManaState.queued.isEmpty())) {
            CombatAnimations.play(p, "cast_release", 2);
            send(PoseStatePayload.CAST_RELEASE);
            posed = false;
        } else if (mage && CastingClient.casting && !posed) {
            ClientSheath.ensureDrawn(p);
            CombatAnimations.play(p, "cast_pose", 4);
            send(PoseStatePayload.CAST_POSE);
            posed = true;
        } else if (posed && !CastingClient.casting) {
            CombatAnimations.play(p, "", 5);
            send(PoseStatePayload.NONE);
            posed = false;
        }
        shootPrev = shoot;
    }

    private static void send(byte state) {
        if (ClientPlayNetworking.canSend(PoseStatePayload.TYPE)) ClientPlayNetworking.send(new PoseStatePayload(state));
    }
}
