package net.zennin.mushoku.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.zennin.mushoku.network.SuperdSyncPayload;

/** Lado del cliente de los Superd: tiempos de espera de las habilidades y el tercer ojo (joya roja de la frente). */
public final class SuperdClient {
    /** Tick del cliente en que cada habilidad vuelve a estar lista. */
    private static final long[] READY = new long[4];
    private static long now;
    /** Tercer ojo encendido: se ven los seres vivos a traves de las paredes (sin mana). */
    public static boolean eye;

    private SuperdClient() {}

    public static boolean isSuperd() { return "superd".equals(ClientRaceState.data.race()); }

    public static void receive(SuperdSyncPayload p) {
        int[] left = {p.lunge(), p.whirl(), p.thr(), p.fury()};
        for (int i = 0; i < 4; i++) READY[i] = now + left[i];
    }

    /** Segundos que faltan (0 = lista). */
    public static int secondsLeft(int skill) { return (int) Math.max(0, (READY[skill] - now + 19) / 20); }

    public static void tick(Minecraft mc) {
        now++;
        if (eye && (mc.player == null || !isSuperd())) eye = false;
    }

    public static void toggleEye(Minecraft mc) {
        eye = !eye;
        mc.getSoundManager().play(SimpleSoundInstance.forUI(eye ? SoundEvents.AMETHYST_BLOCK_RESONATE : SoundEvents.AMETHYST_BLOCK_STEP, eye ? 1.4f : 0.8f, 0.8f));
    }
}
