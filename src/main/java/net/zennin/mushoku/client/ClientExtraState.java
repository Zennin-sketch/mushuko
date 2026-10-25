package net.zennin.mushoku.client;

import net.zennin.mushoku.data.ExtraData;

public final class ClientExtraState {
    public static ExtraData data = ExtraData.DEFAULT;
    public static int version = 0;

    private ClientExtraState() {}

    public static void set(ExtraData d) { data = d; version++; }
}
