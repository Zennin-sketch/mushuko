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

    /** Hechizo en cola, modo sin elemento y niveles aprendidos por elemento. */
    @SuppressWarnings("UnstableApiUsage")
    public static final AttachmentType<MagicData> MAGIC = AttachmentRegistry.<MagicData>builder()
            .persistent(MagicData.CODEC)
            .copyOnDeath()
            .initializer(() -> MagicData.DEFAULT)
            .buildAndRegister(MushokuMod.id("magic"));

    /** Estilo, puntos de habilidad, Touki y habilidades del espadachin. */
    @SuppressWarnings("UnstableApiUsage")
    public static final AttachmentType<SwordData> SWORD = AttachmentRegistry.<SwordData>builder()
            .persistent(SwordData.CODEC)
            .copyOnDeath()
            .initializer(() -> SwordData.DEFAULT)
            .buildAndRegister(MushokuMod.id("sword"));

    /** Rango del gremio y trabajo activo. */
    @SuppressWarnings("UnstableApiUsage")
    public static final AttachmentType<net.zennin.mushoku.guild.GuildData> GUILD = AttachmentRegistry.<net.zennin.mushoku.guild.GuildData>builder()
            .persistent(net.zennin.mushoku.guild.GuildData.CODEC)
            .copyOnDeath()
            .initializer(() -> net.zennin.mushoku.guild.GuildData.DEFAULT)
            .buildAndRegister(MushokuMod.id("guild"));

    /** Ojos demoniacos, conjuro silencioso y estados de la Fase 6. */
    @SuppressWarnings("UnstableApiUsage")
    public static final AttachmentType<ExtraData> EXTRA = AttachmentRegistry.<ExtraData>builder()
            .persistent(ExtraData.CODEC)
            .copyOnDeath()
            .initializer(() -> ExtraData.DEFAULT)
            .buildAndRegister(MushokuMod.id("extra"));

    /** Raza y habilidades del addon. */
    @SuppressWarnings("UnstableApiUsage")
    public static final AttachmentType<net.zennin.mushoku.race.RaceData> RACE = AttachmentRegistry.<net.zennin.mushoku.race.RaceData>builder()
            .persistent(net.zennin.mushoku.race.RaceData.CODEC)
            .copyOnDeath()
            .initializer(() -> net.zennin.mushoku.race.RaceData.DEFAULT)
            .buildAndRegister(MushokuMod.id("race"));

    /** Rasgo visible de la raza (orejas, cuernos, gema) elegido al crear el personaje. */
    @SuppressWarnings("UnstableApiUsage")
    public static final AttachmentType<net.zennin.mushoku.race.Appearance> APPEARANCE = AttachmentRegistry.<net.zennin.mushoku.race.Appearance>builder()
            .persistent(net.zennin.mushoku.race.Appearance.CODEC)
            .copyOnDeath()
            .initializer(() -> net.zennin.mushoku.race.Appearance.NONE)
            .buildAndRegister(MushokuMod.id("appearance"));

    /** Personaje creado con Hitogami y estado del sueno (para volver junto a la cama). */
    @SuppressWarnings("UnstableApiUsage")
    public static final AttachmentType<net.zennin.mushoku.hitogami.HitogamiData> HITOGAMI = AttachmentRegistry.<net.zennin.mushoku.hitogami.HitogamiData>builder()
            .persistent(net.zennin.mushoku.hitogami.HitogamiData.CODEC)
            .copyOnDeath()
            .initializer(() -> net.zennin.mushoku.hitogami.HitogamiData.DEFAULT)
            .buildAndRegister(MushokuMod.id("hitogami"));

    @SuppressWarnings("UnstableApiUsage")
    public static net.zennin.mushoku.race.Appearance appearance(Player player) { return player.getAttachedOrCreate(APPEARANCE); }

    @SuppressWarnings("UnstableApiUsage")
    public static void setAppearance(Player player, net.zennin.mushoku.race.Appearance a) { player.setAttached(APPEARANCE, a); }

    @SuppressWarnings("UnstableApiUsage")
    public static net.zennin.mushoku.hitogami.HitogamiData hitogami(Player player) { return player.getAttachedOrCreate(HITOGAMI); }

    @SuppressWarnings("UnstableApiUsage")
    public static void setHitogami(Player player, net.zennin.mushoku.hitogami.HitogamiData d) { player.setAttached(HITOGAMI, d); }

    /** Escuela, puntos y habilidades del arquero. */
    @SuppressWarnings("UnstableApiUsage")
    public static final AttachmentType<ArcherData> ARCHER = AttachmentRegistry.<ArcherData>builder()
            .persistent(ArcherData.CODEC)
            .copyOnDeath()
            .initializer(() -> ArcherData.DEFAULT)
            .buildAndRegister(MushokuMod.id("archer"));

    @SuppressWarnings("UnstableApiUsage")
    public static ArcherData archer(Player player) { return player.getAttachedOrCreate(ARCHER); }

    @SuppressWarnings("UnstableApiUsage")
    public static void setArcher(Player player, ArcherData d) { player.setAttached(ARCHER, d); }

    private ModAttachments() {}

    @SuppressWarnings("UnstableApiUsage")
    public static net.zennin.mushoku.race.RaceData race(Player player) {
        return player.getAttachedOrCreate(RACE);
    }

    @SuppressWarnings("UnstableApiUsage")
    public static void setRace(Player player, net.zennin.mushoku.race.RaceData data) {
        player.setAttached(RACE, data);
    }

    @SuppressWarnings("UnstableApiUsage")
    public static ExtraData extra(Player player) {
        return player.getAttachedOrCreate(EXTRA);
    }

    @SuppressWarnings("UnstableApiUsage")
    public static void setExtra(Player player, ExtraData data) {
        player.setAttached(EXTRA, data);
    }

    @SuppressWarnings("UnstableApiUsage")
    public static net.zennin.mushoku.guild.GuildData guild(Player player) {
        return player.getAttachedOrCreate(GUILD);
    }

    @SuppressWarnings("UnstableApiUsage")
    public static void setGuild(Player player, net.zennin.mushoku.guild.GuildData data) {
        player.setAttached(GUILD, data);
    }

    @SuppressWarnings("UnstableApiUsage")
    public static SwordData sword(Player player) {
        return player.getAttachedOrCreate(SWORD);
    }

    @SuppressWarnings("UnstableApiUsage")
    public static void setSword(Player player, SwordData data) {
        player.setAttached(SWORD, data);
    }

    @SuppressWarnings("UnstableApiUsage")
    public static MagicData magic(Player player) {
        return player.getAttachedOrCreate(MAGIC);
    }

    @SuppressWarnings("UnstableApiUsage")
    public static void setMagic(Player player, MagicData data) {
        player.setAttached(MAGIC, data);
    }

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
