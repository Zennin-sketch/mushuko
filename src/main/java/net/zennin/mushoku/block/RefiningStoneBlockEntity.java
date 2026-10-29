package net.zennin.mushoku.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.zennin.mushoku.registry.ModBlocks;

/** Piedra apoyada en el piso para refinarla: la piedra, cuanto polvo de refinado lleva y cuanto suma. */
public class RefiningStoneBlockEntity extends BlockEntity {
    private ItemStack stone = ItemStack.EMPTY;
    private int dust;
    private float bonus;
    /** Tirada en curso (la ruleta esta girando): no se puede tocar. */
    public boolean rolling;

    public RefiningStoneBlockEntity(BlockPos pos, BlockState state) { super(ModBlocks.REFINING_STONE_BE, pos, state); }

    public ItemStack stone() { return stone; }
    public int dust() { return dust; }
    public float bonus() { return bonus; }

    public void setStone(ItemStack s) { stone = s.copyWithCount(1); dust = 0; bonus = 0; sync(); }
    public void addDust(float b) { dust++; bonus += b; sync(); }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.saveAdditional(tag, reg);
        if (!stone.isEmpty()) tag.put("Stone", stone.save(reg));
        tag.putInt("Dust", dust);
        tag.putFloat("Bonus", bonus);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.loadAdditional(tag, reg);
        stone = tag.contains("Stone") ? ItemStack.parseOptional(reg, tag.getCompound("Stone")) : ItemStack.EMPTY;
        dust = tag.getInt("Dust");
        bonus = tag.getFloat("Bonus");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider reg) { return saveWithoutMetadata(reg); }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
