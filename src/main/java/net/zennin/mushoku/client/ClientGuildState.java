package net.zennin.mushoku.client;

import net.zennin.mushoku.guild.GuildData;

public final class ClientGuildState {
    public static GuildData data = GuildData.DEFAULT;
    public static int version = 0;

    private ClientGuildState() {}

    public static void set(GuildData d) { data = d; version++; }
}
