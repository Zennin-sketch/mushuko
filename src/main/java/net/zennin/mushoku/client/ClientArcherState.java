package net.zennin.mushoku.client;

import net.zennin.mushoku.data.ArcherData;

/** Copia en el cliente de los datos del arquero. */
public final class ClientArcherState {
    public static ArcherData data = ArcherData.DEFAULT;
    public static int version = 0;

    private ClientArcherState() {}

    public static void set(ArcherData d) {
        data = d;
        version++;
    }
}
