package net.zennin.mushoku.refine;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.zennin.mushoku.block.RefiningStoneBlockEntity;
import net.zennin.mushoku.data.ManaData;
import net.zennin.mushoku.data.ModAttachments;
import net.zennin.mushoku.item.ElementBookItem;
import net.zennin.mushoku.item.SpellData;
import net.zennin.mushoku.magic.Tasks;
import net.zennin.mushoku.network.ModNetworking;
import net.zennin.mushoku.network.RefinePayloads;
import net.zennin.mushoku.registry.ModBlocks;
import net.zennin.mushoku.registry.ModItems;

import java.util.HashSet;
import java.util.Set;

/**
 * Refinado de piedras: la piedra apoyada en el piso (RefiningStoneBlock) se imbuye con el hechizo Refinamiento, que
 * abre el menu rúnico. Cuesta maná segun el nivel al que va; puede salir bien (sube un nivel) o romperse.
 * El Polvo de refinado suma probabilidad (al azar dentro de un rango segun el nivel), hasta 5 y con tope de 80%.
 */
public final class Refining {
    private Refining() {}

    /** Por nivel destino (2 Intermedia ... 6 Imperial). */
    public static final int[] MANA = {0, 0, 100, 300, 800, 2000, 5000};
    public static final float[] BASE = {0, 0, 0.50f, 0.40f, 0.30f, 0.20f, 0.12f};
    public static final float[] DUST_MIN = {0, 0, 0.05f, 0.04f, 0.035f, 0.03f, 0.025f};
    public static final float[] DUST_MAX = {0, 0, 0.10f, 0.08f, 0.07f, 0.06f, 0.05f};
    public static final int MAX_DUST = 5;
    public static final float CAP = 0.80f;
    /** Lo que gira la ruleta antes de ver el resultado. */
    public static final int SPIN_TICKS = 56;

    private static final Set<Long> CONSUMED = new HashSet<>();

    public static boolean consumed(Level level, BlockPos pos) { return CONSUMED.remove(pos.asLong()); }

    public static float chance(RefiningStoneBlockEntity be) {
        int target = ElementBookItem.stoneLevel(be.stone()) + 1;
        if (target < 2 || target > 6) return 0;
        return Math.min(CAP, BASE[target] + be.bonus());
    }

    /** Hechizo Refinamiento: la piedra que miras (hasta 6 bloques) abre su menu. */
    public static boolean cast(ServerPlayer p) {
        Vec3 eye = p.getEyePosition();
        BlockHitResult hit = p.level().clip(new ClipContext(eye, eye.add(p.getLookAngle().scale(6)), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, p));
        if (hit.getType() != HitResult.Type.BLOCK || !(p.level().getBlockEntity(hit.getBlockPos()) instanceof RefiningStoneBlockEntity be)) {
            p.displayClientMessage(Component.translatable("msg.remushoku.refine_look").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        int tier = ElementBookItem.stoneLevel(be.stone());
        if (tier >= 6) {
            p.displayClientMessage(Component.translatable("msg.remushoku.stone_max").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        ((ServerLevel) p.level()).sendParticles(ParticleTypes.ENCHANT, hit.getBlockPos().getX() + 0.5, hit.getBlockPos().getY() + 0.6,
                hit.getBlockPos().getZ() + 0.5, 40, 0.3, 0.3, 0.3, 0.6);
        p.level().playSound(null, hit.getBlockPos(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1f, 0.8f);
        open(p, hit.getBlockPos(), be);
        return true;
    }

    public static void open(ServerPlayer p, BlockPos pos, RefiningStoneBlockEntity be) {
        int tier = ElementBookItem.stoneLevel(be.stone());
        ManaData mana = ModAttachments.get(p);
        ModNetworking.send(p, new RefinePayloads.Open(pos, tier, be.dust(), chance(be), MANA[Math.min(6, tier + 1)],
                p.getAbilities().instabuild ? Integer.MAX_VALUE : (int) mana.mana()));
    }

    public static void action(ServerPlayer p, BlockPos pos, int action) {
        if (p.distanceToSqr(Vec3.atCenterOf(pos)) > 10 * 10) return;
        if (!(p.level().getBlockEntity(pos) instanceof RefiningStoneBlockEntity be) || be.rolling) return;
        if (action < 0) { open(p, pos, be); return; } // pedir los datos de nuevo
        int tier = ElementBookItem.stoneLevel(be.stone());
        if (tier < 1 || tier >= 6) return;
        int target = tier + 1;
        ServerLevel level = p.serverLevel();
        boolean creative = p.getAbilities().instabuild;
        if (action == RefinePayloads.ADD_DUST) {
            if (be.dust() >= MAX_DUST) return;
            if (!creative && !take(p)) {
                p.displayClientMessage(Component.translatable("msg.remushoku.refine_no_dust").withStyle(ChatFormatting.RED), true);
                return;
            }
            float b = DUST_MIN[target] + p.getRandom().nextFloat() * (DUST_MAX[target] - DUST_MIN[target]);
            be.addDust(b);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.2f + be.dust() * 0.1f);
            level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8, 0.2, 0.2, 0.2, 0.02);
            open(p, pos, be);
        } else if (action == RefinePayloads.ROLL) {
            ManaData mana = ModAttachments.get(p);
            int cost = MANA[target];
            if (!creative && mana.mana() < cost) {
                p.displayClientMessage(Component.translatable("msg.remushoku.no_mana").withStyle(ChatFormatting.RED), true);
                return;
            }
            if (!creative) { ModAttachments.set(p, mana.withMana(mana.mana() - cost)); ModNetworking.syncMana(p); }
            boolean ok = p.getRandom().nextFloat() < chance(be);
            be.rolling = true;
            ModNetworking.send(p, new RefinePayloads.Spin(pos, ok, chance(be)));
            level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8f, 1.6f);
            Tasks.later(SPIN_TICKS, () -> finish(level, pos, ok));
        }
    }

    private static boolean take(ServerPlayer p) {
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack st = inv.getItem(i);
            if (st.is(ModItems.REFINING_DUST)) { st.shrink(1); return true; }
        }
        return false;
    }

    private static void finish(ServerLevel level, BlockPos pos, boolean ok) {
        if (!(level.getBlockEntity(pos) instanceof RefiningStoneBlockEntity be)) return;
        be.rolling = false;
        double x = pos.getX() + 0.5, y = pos.getY() + 0.4, z = pos.getZ() + 0.5;
        if (ok) {
            int tier = ElementBookItem.stoneLevel(be.stone());
            ItemStack up = new ItemStack(SpellData.stoneFor(tier + 1));
            be.setStone(up);
            level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.4f);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.2f, 1f);
            level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, x, y, z, 40, 0.3, 0.4, 0.3, 0.3);
            level.sendParticles(ParticleTypes.END_ROD, x, y, z, 30, 0.2, 0.6, 0.2, 0.08);
        } else {
            CONSUMED.add(pos.asLong());
            level.removeBlock(pos, false);
            level.playSound(null, pos, SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.2f, 0.8f);
            level.playSound(null, pos, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.2f, 1f);
            level.sendParticles(new net.minecraft.core.particles.ItemParticleOption(ParticleTypes.ITEM, be.stone()), x, y, z, 30, 0.2, 0.2, 0.2, 0.12);
            level.sendParticles(ParticleTypes.SMOKE, x, y, z, 20, 0.2, 0.2, 0.2, 0.02);
        }
        for (ServerPlayer sp : level.players()) {
            if (sp.distanceToSqr(x, y, z) < 16 * 16) ModNetworking.send(sp, new RefinePayloads.Result(pos, ok));
        }
    }

    /** Shift + clic derecho con una piedra (sin hechizo) sobre la cara de arriba de un bloque: la apoya ahi. */
    public static boolean place(Level level, BlockPos clicked, net.minecraft.core.Direction face, ServerPlayer p, ItemStack stack) {
        if (face != net.minecraft.core.Direction.UP || ElementBookItem.stoneLevel(stack) == 0 || SpellData.get(stack) != null) return false;
        BlockPos pos = clicked.above();
        if (!level.getBlockState(pos).canBeReplaced()) return false;
        var state = ModBlocks.REFINING_STONE.defaultBlockState();
        if (!state.canSurvive(level, pos)) return false;
        level.setBlock(pos, state, 3);
        if (level.getBlockEntity(pos) instanceof RefiningStoneBlockEntity be) be.setStone(stack);
        if (!p.getAbilities().instabuild) stack.shrink(1);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_PLACE, SoundSource.BLOCKS, 1f, 1f);
        return true;
    }
}
