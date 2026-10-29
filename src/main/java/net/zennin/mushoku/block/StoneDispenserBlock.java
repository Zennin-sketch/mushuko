package net.zennin.mushoku.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.zennin.mushoku.item.ElementBookItem;
import net.zennin.mushoku.item.SpellData;
import net.zennin.mushoku.magic.Spell;

/**
 * Dispensador de Piedras: como un dispensador, pero solo sirve para cargar circulos magicos. Con un pulso (tiza azul
 * activada o redstone) busca el circulo al que apunta (hasta 4 bloques, o el que esta en el piso de ese bloque) y le
 * suelta una piedra del rango del hechizo inscrito. Asi se arman mecanismos que recargan y disparan circulos en serie.
 */
public class StoneDispenserBlock extends DispenserBlock {
    public static final MapCodec<StoneDispenserBlock> CODEC = simpleCodec(StoneDispenserBlock::new);

    public StoneDispenserBlock(Properties props) { super(props); }

    @Override
    public MapCodec<? extends DispenserBlock> codec() { return CODEC; }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new StoneDispenserBlockEntity(pos, state); }

    @Override
    protected void dispenseFrom(ServerLevel level, BlockState state, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof StoneDispenserBlockEntity be)) return;
        Direction facing = state.getValue(FACING);
        BlockPos circle = findCircle(level, pos, facing);
        if (circle == null || !(level.getBlockEntity(circle) instanceof MagicCircleBlockEntity cbe) || cbe.spell() == null || cbe.armed()) {
            level.levelEvent(1001, pos, 0); // click de dispensador vacio
            return;
        }
        Spell spell = cbe.spell();
        for (int i = 0; i < be.getContainerSize(); i++) {
            ItemStack st = be.getItem(i);
            if (st.isEmpty() || SpellData.get(st) != null || ElementBookItem.stoneLevel(st) != spell.level.tier) continue;
            ItemStack shown = st.copyWithCount(1);
            st.shrink(1);
            be.setChanged();
            MagicCircleBlock.arm(level, circle, level.getBlockState(circle), cbe);
            // la piedra sale volando al circulo
            Vec3 from = Vec3.atCenterOf(pos).add(Vec3.atLowerCornerOf(facing.getNormal()).scale(0.6));
            Vec3 to = Vec3.atCenterOf(circle).add(0, -0.35, 0);
            for (int k = 0; k <= 8; k++) {
                Vec3 at = from.lerp(to, k / 8.0);
                level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, shown), at.x, at.y, at.z, 2, 0.04, 0.04, 0.04, 0.0);
                level.sendParticles(ParticleTypes.ENCHANT, at.x, at.y, at.z, 1, 0.05, 0.05, 0.05, 0.2);
            }
            level.playSound(null, pos, SoundEvents.DISPENSER_LAUNCH, SoundSource.BLOCKS, 0.8f, 1.3f);
            level.playSound(null, circle, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1f, 1.2f);
            return;
        }
        level.levelEvent(1001, pos, 0);
    }

    /** El circulo al que apunta: en linea recta hasta 4 bloques, o apoyado en el piso de esa linea. */
    private static BlockPos findCircle(ServerLevel level, BlockPos pos, Direction facing) {
        for (int d = 1; d <= 4; d++) {
            BlockPos p = pos.relative(facing, d);
            if (level.getBlockState(p).getBlock() instanceof MagicCircleBlock) return p;
            if (facing.getAxis().isHorizontal() && level.getBlockState(p.below()).getBlock() instanceof MagicCircleBlock) return p.below();
            if (!level.getBlockState(p).getCollisionShape(level, p).isEmpty()) return null;
        }
        return null;
    }
}
