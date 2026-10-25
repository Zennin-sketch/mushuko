package net.zennin.mushoku.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.zennin.mushoku.MushokuMod;

/** Cliente -> servidor: acciones del gremio y compras/ventas en tiendas. */
public record GuildActionPayload(int action, int arg, String shop) implements CustomPacketPayload {
    public static final int ACCEPT = 0, SUBMIT = 1, COMPLETE = 2, ABANDON = 3, RANK_UP = 4, BUY = 5, SELL = 6, EYE_PICK = 7, RACE_PICK = 8, RACE_TOGGLE = 9, RACE_SKILL = 10, PARTY = 11, COMBINE = 12, TRADE_BOOK = 13;
    public static final Type<GuildActionPayload> TYPE = new Type<>(MushokuMod.id("guild_action"));
    public static final StreamCodec<ByteBuf, GuildActionPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, GuildActionPayload::action,
            ByteBufCodecs.VAR_INT, GuildActionPayload::arg,
            ByteBufCodecs.STRING_UTF8, GuildActionPayload::shop,
            GuildActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
