package net.zennin.mushoku.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.zennin.mushoku.MushokuMod;

/** Servidor -> cliente: abrir el menu del gremio ("guild") o una tienda (id de la tienda). */
public record OpenScreenPayload(String screen) implements CustomPacketPayload {
    public static final Type<OpenScreenPayload> TYPE = new Type<>(MushokuMod.id("open_screen"));
    public static final StreamCodec<ByteBuf, OpenScreenPayload> CODEC = ByteBufCodecs.STRING_UTF8.map(OpenScreenPayload::new, OpenScreenPayload::screen);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
