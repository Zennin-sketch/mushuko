package net.zennin.mushoku.data;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.world.entity.player.Player;
import net.zennin.mushoku.MushokuMod;

public final class ModAttachments {
    /** Mana del jugador: se guarda en el mundo y se conserva al morir. */
    @SuppressWarnings("UnstableApiUsage")
    public static final AttachmentType<ManaData> MANA = AttachmentRegistry.<ManaData>builder()
            .persistent(ManaData.CODEC)
            .copyOnDeath()
            .initializer(() -> ManaData.DEFAULT)
            .buildAndRegister(MushokuMod.id("mana"));

    private ModAttachments() {}

    @SuppressWarnings("UnstableApiUsage")
    public static ManaData get(Player player) {
        return player.getAttachedOrCreate(MANA);
    }

    @SuppressWarnings("UnstableApiUsage")
    public static void set(Player player, ManaData data) {
        player.setAttached(MANA, data);
    }

    public static void init() {}
}
