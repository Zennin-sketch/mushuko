package net.zennin.mushoku.client;

/** Copia en el cliente de los datos que manda el servidor. */
public final class ClientManaState {
    public static double mana = 100;
    public static double maxMana = 100;
    public static int playerClass = 0;
    public static String queued = "";
    public static boolean noElement = false;
    public static int water = 1, fire = 1, earth = 1, wind = 1;

    private ClientManaState() {}
}
