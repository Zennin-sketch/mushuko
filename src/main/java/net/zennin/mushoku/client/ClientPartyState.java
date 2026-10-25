package net.zennin.mushoku.client;

import net.zennin.mushoku.network.PartySyncPayload;

import java.util.List;

public final class ClientPartyState {
    public static String leader = "", invite = "";
    public static List<String> members = List.of();
    public static int version = 0;

    private ClientPartyState() {}

    public static void set(PartySyncPayload p) {
        leader = p.leader();
        members = p.members();
        invite = p.invite();
        version++;
    }
}
