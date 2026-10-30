package net.zennin.mushoku.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.zennin.mushoku.MushokuMod;

import java.util.HashMap;
import java.util.Map;

/** Servidor -> cliente: lanzamientos de cada hechizo y los hechizos que conoce (para el libro y la mesa). */
public record SpellXpPayload(Map<String, Integer> uses, java.util.List<String> known) implements CustomPacketPayload {
    public static final Type<SpellXpPayload> TYPE = new Type<>(MushokuMod.id("spell_xp"));
    public static final StreamCodec<ByteBuf, SpellXpPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT), p -> new HashMap<>(p.uses()),
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), SpellXpPayload::known,
            SpellXpPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
