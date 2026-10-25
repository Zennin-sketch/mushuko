package net.zennin.mushoku.client;

import net.zennin.mushoku.race.RaceData;

public final class ClientRaceState {
    public static RaceData data = RaceData.DEFAULT;
    public static int version = 0;

    private ClientRaceState() {}

    public static void set(RaceData d) { data = d; version++; }
}
