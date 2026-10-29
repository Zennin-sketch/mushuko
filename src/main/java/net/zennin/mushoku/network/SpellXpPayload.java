package net.zennin.mushoku.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.zennin.mushoku.MushokuMod;

import java.util.HashMap;
import java.util.Map;

/** Servidor -> cliente: lanzamientos de cada hechizo (para el nivel que muestra el libro). */
public record SpellXpPayload(Map<String, Integer> uses) implements CustomPacketPayload {
    public static final Type<SpellXpPayload> TYPE = new Type<>(MushokuMod.id("spell_xp"));
    public static final StreamCodec<ByteBuf, SpellXpPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT), p -> new HashMap<>(p.uses()),
            SpellXpPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
