package net.zennin.mushoku.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.zennin.mushoku.MushokuMod;

/** Mensajes del refinado de piedras (menu rúnico). */
public final class RefinePayloads {
    public static final int ADD_DUST = 0, ROLL = 1;

    private RefinePayloads() {}

    /** Servidor -> cliente: abrir/actualizar el menu. */
    public record Open(BlockPos pos, int tier, int dust, float chance, int mana, int playerMana) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(MushokuMod.id("refine_open"));
        public static final StreamCodec<ByteBuf, Open> CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Open::pos, ByteBufCodecs.VAR_INT, Open::tier, ByteBufCodecs.VAR_INT, Open::dust,
                ByteBufCodecs.FLOAT, Open::chance, ByteBufCodecs.VAR_INT, Open::mana, ByteBufCodecs.VAR_INT, Open::playerMana, Open::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Servidor -> cliente: empieza la tirada (ya se sabe si sale bien; la ruleta cae en esa parte). */
    public record Spin(BlockPos pos, boolean success, float chance) implements CustomPacketPayload {
        public static final Type<Spin> TYPE = new Type<>(MushokuMod.id("refine_spin"));
        public static final StreamCodec<ByteBuf, Spin> CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Spin::pos, ByteBufCodecs.BOOL, Spin::success, ByteBufCodecs.FLOAT, Spin::chance, Spin::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Servidor -> cliente: resultado final. */
    public record Result(BlockPos pos, boolean success) implements CustomPacketPayload {
        public static final Type<Result> TYPE = new Type<>(MushokuMod.id("refine_result"));
        public static final StreamCodec<ByteBuf, Result> CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Result::pos, ByteBufCodecs.BOOL, Result::success, Result::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Cliente -> servidor: agregar polvo o tirar. */
    public record Action(BlockPos pos, int action) implements CustomPacketPayload {
        public static final Type<Action> TYPE = new Type<>(MushokuMod.id("refine_action"));
        public static final StreamCodec<ByteBuf, Action> CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Action::pos, ByteBufCodecs.VAR_INT, Action::action, Action::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
}
