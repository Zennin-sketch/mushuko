package net.zennin.mushoku.archer;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.zennin.mushoku.item.MushokuBowItem;
import net.zennin.mushoku.magic.Tasks;
import net.zennin.mushoku.network.ModNetworking;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Arcos del mod al estilo Unturned: con R se carga la flecha (animacion de sacarla del carcaj y colocarla) y recien
 * ahi se puede tensar manteniendo el clic. Al disparar queda descargado.
 */
public final class BowSystem {
    /** Ticks que tarda en cargar (lo que dura la animacion bow_load). */
    public static final int LOAD_TICKS = 16;
    private static final String KEY = "remushoku_loaded";
    private static final Map<UUID, Long> LOADING = new HashMap<>();

    private BowSystem() {}

    public static void init() {}

    public static boolean isLoaded(ItemStack bow) {
        CustomData d = bow.get(DataComponents.CUSTOM_DATA);
        return d != null && d.copyTag().getBoolean(KEY);
    }

    public static void setLoaded(ItemStack bow, boolean loaded) {
        CustomData.update(DataComponents.CUSTOM_DATA, bow, tag -> {
            if (loaded) tag.putBoolean(KEY, true);
            else tag.remove(KEY);
        });
    }

    /** Esta cargando (para no tensar a mitad de la animacion). */
    public static boolean busy(ServerPlayer p) {
        Long until = LOADING.get(p.getUUID());
        return until != null && until > p.server.getTickCount();
    }

    public static void reload(ServerPlayer p) {
        ItemStack bow = p.getMainHandItem();
        if (!(bow.getItem() instanceof MushokuBowItem) || isLoaded(bow) || busy(p) || p.isUsingItem()) return;
        boolean creative = p.getAbilities().instabuild;
        if (!creative && p.getProjectile(bow).isEmpty()) {
            p.displayClientMessage(net.minecraft.network.chat.Component.translatable("msg.remushoku.no_arrows")
                    .withStyle(net.minecraft.ChatFormatting.RED), true);
            return;
        }
        long now = p.server.getTickCount();
        LOADING.put(p.getUUID(), now + LOAD_TICKS);
        ModNetworking.playAnim(p, "bow_load");
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.CROSSBOW_LOADING_START.value(), SoundSource.PLAYERS, 0.6f, 1.3f);
        Tasks.later(LOAD_TICKS - 3, () -> {
            ItemStack now2 = p.getMainHandItem();
            if (now2 != bow || !p.isAlive()) { LOADING.remove(p.getUUID()); return; }
            setLoaded(bow, true);
            p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.CROSSBOW_LOADING_END.value(), SoundSource.PLAYERS, 0.7f, 1.5f);
        });
    }

    @SuppressWarnings("unused")
    private static CompoundTag unused() { return null; }
}
