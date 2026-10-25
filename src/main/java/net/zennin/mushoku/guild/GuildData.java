package net.zennin.mushoku.guild;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Rango del gremio (1 = F ... 6 = A, 7 = S), XP de rango, trabajo activo (0 = ninguno) y su progreso. */
public record GuildData(int rank, int xp, int job, int progress) {
    public static final GuildData DEFAULT = new GuildData(1, 0, 0, 0);
    public static final Codec<GuildData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("rank", 1).forGetter(GuildData::rank),
            Codec.INT.optionalFieldOf("xp", 0).forGetter(GuildData::xp),
            Codec.INT.optionalFieldOf("job", 0).forGetter(GuildData::job),
            Codec.INT.optionalFieldOf("progress", 0).forGetter(GuildData::progress)
    ).apply(i, GuildData::new));

    /** XP para subir desde cada rango (igual que el original). */
    public static int xpToRankUp(int rank) {
        return switch (rank) { case 1 -> 100; case 2 -> 250; case 3 -> 2500; case 4 -> 12500; case 5 -> 75000; case 6 -> 375000; default -> -1; };
    }

    public static String rankLetter(int rank) {
        return switch (rank) { case 1 -> "F"; case 2 -> "E"; case 3 -> "D"; case 4 -> "C"; case 5 -> "B"; case 6 -> "A"; default -> "S"; };
    }

    public GuildData withJob(int j, int p) { return new GuildData(rank, xp, j, p); }

    public GuildData withXp(int x) { return new GuildData(rank, x, job, progress); }

    public GuildData withRank(int r) { return new GuildData(r, xp, job, progress); }
}
