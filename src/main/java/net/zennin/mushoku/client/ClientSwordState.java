package net.zennin.mushoku.client;

import net.zennin.mushoku.data.SwordData;

/** Copia en el cliente de los datos de espadachin. version cambia con cada actualizacion (para refrescar el menu). */
public final class ClientSwordState {
    public static SwordData data = SwordData.DEFAULT;
    public static int version = 0;

    private ClientSwordState() {}

    public static void set(SwordData d) {
        data = d;
        version++;
    }
}
