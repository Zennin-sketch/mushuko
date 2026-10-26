package net.zennin.mushoku.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.zennin.mushoku.magic.Spell;
import net.zennin.mushoku.registry.ModBlocks;

import java.util.UUID;

/** Guarda el hechizo inscrito en el circulo, su dueño y si la trampa esta activa. */
public class MagicCircleBlockEntity extends BlockEntity {
    private String spell = "";
    private UUID owner;
    private boolean armed;
    /** Venia senal de una linea de tiza azul (para activarse solo al encenderse). */
    public boolean linePowered;

    public MagicCircleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.MAGIC_CIRCLE_BE, pos, state);
    }

    public Spell spell() { return spell.isEmpty() ? null : Spell.byId(spell); }

    public void setSpell(Spell s) { spell = s == null ? "" : s.id; if (s == null) armed = false; setChanged(); }

    public UUID owner() { return owner; }

    public void setOwner(UUID id) { owner = id; setChanged(); }

    public boolean isOwner(UUID id) { return owner == null || owner.equals(id); }

    public boolean armed() { return armed; }

    public void setArmed(boolean v) { armed = v; setChanged(); }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("Spell", spell);
        tag.putBoolean("Armed", armed);
        if (owner != null) tag.putUUID("Owner", owner);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        spell = tag.getString("Spell");
        armed = tag.getBoolean("Armed");
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
    }
}
