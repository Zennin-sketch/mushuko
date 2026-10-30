package net.zennin.mushoku.menu;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.zennin.mushoku.data.ManaData;
import net.zennin.mushoku.data.ModAttachments;
import net.zennin.mushoku.network.ModNetworking;
import net.zennin.mushoku.registry.ModBlocks;

/** Menu de la Mesa de Investigacion: tres huecos de materiales y la piedra que sale (cuesta mana al sacarla). */
public class ResearchMenu extends AbstractContainerMenu {
    public static final int IN_X = 30, IN_Y = 34, OUT_X = 124, OUT_Y = 34;
    private final Container input = new SimpleContainer(3) {
        @Override
        public void setChanged() {
            super.setChanged();
            ResearchMenu.this.slotsChanged(this);
        }
    };
    private final ResultContainer output = new ResultContainer();
    private final ContainerLevelAccess access;
    private final Player player;
    /** [0] = mana que cuesta lo que esta armado (0 = nada). */
    private final ContainerData data = new SimpleContainerData(1);

    public ResearchMenu(int id, Inventory inv) { this(id, inv, ContainerLevelAccess.NULL); }

    public ResearchMenu(int id, Inventory inv, ContainerLevelAccess access) {
        super(ModMenus.RESEARCH, id);
        this.access = access;
        this.player = inv.player;
        for (int i = 0; i < 3; i++) addSlot(new Slot(input, i, IN_X + i * 22, IN_Y));
        addSlot(new Slot(output, 0, OUT_X, OUT_Y) {
            @Override public boolean mayPlace(ItemStack s) { return false; }

            @Override
            public boolean mayPickup(Player p) {
                ResearchRecipes.Recipe r = ResearchRecipes.match(input);
                if (r == null) return false;
                if (p.getAbilities().instabuild) return true;
                double mana = p.level().isClientSide ? net.zennin.mushoku.client.ClientManaState.mana : ModAttachments.get(p).mana();
                return mana >= r.mana();
            }

            @Override
            public void onTake(Player p, ItemStack stack) {
                ResearchRecipes.Recipe r = ResearchRecipes.match(input);
                if (r != null) {
                    if (p instanceof ServerPlayer sp && !sp.getAbilities().instabuild) {
                        ManaData m = ModAttachments.get(sp);
                        ModAttachments.set(sp, m.withMana(m.mana() - r.mana()));
                        ModNetworking.syncMana(sp);
                    }
                    ResearchRecipes.consume(r, input);
                    access.execute((level, pos) -> level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1f, 1.2f));
                }
                super.onTake(p, stack);
            }
        });
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c + r * 9 + 9, 8 + c * 18, 84 + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, 8 + c * 18, 142));
        addDataSlots(data);
    }

    public boolean engraving() { return ResearchRecipes.engraving(input); }

    // ------------------------------------------------------------------ investigar hechizos (ruleta)

    /** Id especial del EngravePayload que pide investigar en vez de grabar. */
    public static final String RESEARCH = "?research";
    /** Mana que cuesta cada intento. */
    public static final int RESEARCH_COST = 150;

    /** Probabilidad de exito: 3 % y +1.5 % por nivel de la piedra de mana que se ofrezca en los huecos. */
    public static float researchChance(int stoneTier) { return 0.03f + 0.015f * stoneTier; }

    /** Nivel de la piedra de mana que hay en los huecos (0 = ninguna). */
    public int offeredStone() {
        int best = 0;
        for (int i = 0; i < input.getContainerSize(); i++) {
            ItemStack st = input.getItem(i);
            for (int t = 1; t <= 6; t++) if (st.is(net.zennin.mushoku.item.SpellData.stoneFor(t))) best = Math.max(best, t);
        }
        return best;
    }

    /**
     * Investiga un hechizo: gasta un pergamino en blanco, un uso de la Tinta y Pluma, mana y la piedra ofrecida. Casi
     * siempre sale mal; si sale bien da un Pergamino de hechizo de uno que no conozca (los de nivel alto son rarisimos).
     */
    public void research(ServerPlayer p) {
        if (!engraving()) return;
        boolean creative = p.getAbilities().instabuild;
        ManaData m = ModAttachments.get(p);
        if (!creative && m.mana() < RESEARCH_COST) {
            p.displayClientMessage(net.minecraft.network.chat.Component.translatable("msg.remushoku.no_mana").withStyle(net.minecraft.ChatFormatting.RED), true);
            return;
        }
        java.util.List<net.zennin.mushoku.magic.Spell> unknown = new java.util.ArrayList<>();
        for (var s : net.zennin.mushoku.magic.Spell.values())
            if (s.implemented && !net.zennin.mushoku.magic.SpellKnowledge.knows(p, s)) unknown.add(s);
        if (unknown.isEmpty()) {
            p.displayClientMessage(net.minecraft.network.chat.Component.translatable("msg.remushoku.research_all").withStyle(net.minecraft.ChatFormatting.GOLD), true);
            return;
        }
        int stone = offeredStone();
        // gasta todo
        boolean scroll = false, gem = false;
        for (int i = 0; i < input.getContainerSize(); i++) {
            ItemStack st = input.getItem(i);
            if (!scroll && st.is(net.zennin.mushoku.registry.ModItems.SCROLL) && net.zennin.mushoku.item.SpellData.get(st) == null) { st.shrink(1); scroll = true; }
            else if (!gem && stone > 0 && st.is(net.zennin.mushoku.item.SpellData.stoneFor(stone))) { st.shrink(1); gem = true; }
            else if (st.is(net.zennin.mushoku.registry.ModItems.INKAND_QUILL) && !creative) {
                st.hurtAndBreak(1, (net.minecraft.server.level.ServerLevel) p.level(), p, it -> {});
            }
        }
        input.setChanged();
        if (!creative) { ModAttachments.set(p, m.withMana(m.mana() - RESEARCH_COST)); ModNetworking.syncMana(p); }
        // tirada
        var rnd = p.getRandom();
        net.zennin.mushoku.magic.Spell won = null;
        if (rnd.nextFloat() < researchChance(stone)) {
            double total = 0;
            double[] w = new double[unknown.size()];
            for (int i = 0; i < w.length; i++) {
                int t = unknown.get(i).level.tier;
                w[i] = switch (t) { case 1 -> 40; case 2 -> 22; case 3 -> 10; case 4 -> 3; case 5 -> 0.8; default -> 0.2; };
                if (t <= stone + 1) w[i] *= 2; // la piedra empuja hacia hechizos de su nivel
                total += w[i];
            }
            double r = rnd.nextDouble() * total;
            for (int i = 0; i < w.length; i++) { r -= w[i]; if (r <= 0) { won = unknown.get(i); break; } }
            if (won == null) won = unknown.get(unknown.size() - 1);
            ItemStack out = net.zennin.mushoku.item.SpellLessonScrollItem.of(won);
            if (!p.getInventory().add(out)) p.drop(out, false);
        }
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(p, new net.zennin.mushoku.network.ResearchRollPayload(won == null ? "" : won.id));
        access.execute((level, pos) -> level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1f, 0.7f));
        // el resultado se anuncia cuando termina la ruleta (2.5 s)
        final net.zennin.mushoku.magic.Spell result = won;
        net.zennin.mushoku.magic.Tasks.later(50, () -> {
            if (result != null) {
                p.displayClientMessage(net.minecraft.network.chat.Component.translatable("msg.remushoku.research_win",
                        net.minecraft.network.chat.Component.translatable(result.translationKey()).withStyle(st -> st.withColor(net.zennin.mushoku.magic.SpellKnowledge.color(result.level))))
                        .withStyle(net.minecraft.ChatFormatting.GOLD), false);
                p.level().playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.8f, 1f);
            } else {
                p.displayClientMessage(net.minecraft.network.chat.Component.translatable("msg.remushoku.research_fail").withStyle(net.minecraft.ChatFormatting.GRAY), true);
                p.level().playSound(null, p.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.6f, 1.2f);
            }
        });
    }

    /** El jugador sabe este hechizo (nivel del elemento suficiente; los combinados no se graban). */
    public static boolean knows(Player p, net.zennin.mushoku.magic.Spell s) {
        if (s.combined() || !s.implemented) return false;
        if (!net.zennin.mushoku.magic.SpellKnowledge.knows(p, s)) return false;
        if (p.getAbilities().instabuild) return true;
        int lvl = p.level().isClientSide ? clientLevel(s.element) : ModAttachments.magic(p).level(s.element);
        return lvl >= s.level.tier;
    }

    private static int clientLevel(net.zennin.mushoku.magic.Element e) {
        int w = net.zennin.mushoku.client.ClientManaState.water, f = net.zennin.mushoku.client.ClientManaState.fire,
                ea = net.zennin.mushoku.client.ClientManaState.earth, wi = net.zennin.mushoku.client.ClientManaState.wind;
        return new net.zennin.mushoku.data.MagicData("", false, w, f, ea, wi).level(e);
    }

    /** Graba el hechizo en un pergamino en blanco (la pluma no se gasta; el pergamino si, y el mana). */
    public void engrave(ServerPlayer p, net.zennin.mushoku.magic.Spell s) {
        if (s == null || !engraving() || !knows(p, s)) return;
        int cost = ResearchRecipes.engraveCost(s);
        ManaData m = ModAttachments.get(p);
        if (!p.getAbilities().instabuild && m.mana() < cost) {
            p.displayClientMessage(net.minecraft.network.chat.Component.translatable("msg.remushoku.no_mana").withStyle(net.minecraft.ChatFormatting.RED), true);
            return;
        }
        for (int i = 0; i < input.getContainerSize(); i++) {
            ItemStack st = input.getItem(i);
            if (st.is(net.zennin.mushoku.registry.ModItems.SCROLL) && net.zennin.mushoku.item.SpellData.get(st) == null) { st.shrink(1); break; }
        }
        // la tinta y pluma se gasta: 8 grabados y se termina
        if (!p.getAbilities().instabuild)
            for (int i = 0; i < input.getContainerSize(); i++) {
                ItemStack st = input.getItem(i);
                if (st.is(net.zennin.mushoku.registry.ModItems.INKAND_QUILL)) {
                    st.hurtAndBreak(1, (net.minecraft.server.level.ServerLevel) p.level(), p, it -> {});
                    if (st.getDamageValue() >= st.getMaxDamage()) input.setItem(i, ItemStack.EMPTY);
                    break;
                }
            }
        input.setChanged();
        if (!p.getAbilities().instabuild) { ModAttachments.set(p, m.withMana(m.mana() - cost)); ModNetworking.syncMana(p); }
        ItemStack out = net.zennin.mushoku.item.SpellData.with(new ItemStack(net.zennin.mushoku.registry.ModItems.SCROLL), s);
        if (!p.getInventory().add(out)) p.drop(out, false);
        access.execute((level, pos) -> {
            level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1f, 1f);
            level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.7f, 1.4f);
        });
        p.displayClientMessage(net.minecraft.network.chat.Component.translatable("msg.remushoku.engraved",
                net.minecraft.network.chat.Component.translatable(s.translationKey())).withStyle(net.minecraft.ChatFormatting.AQUA), true);
    }

    /** Mana que pide la investigacion armada (0 = ninguna). */
    public int manaCost() { return data.get(0); }

    @Override
    public void slotsChanged(Container c) {
        if (c != input) return;
        ResearchRecipes.Recipe r = ResearchRecipes.match(input);
        output.setItem(0, r == null ? ItemStack.EMPTY : ResearchRecipes.result(r));
        data.set(0, r == null ? 0 : r.mana());
        broadcastChanges();
    }

    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack st = slot.getItem(), copy = st.copy();
        if (index == 3) {
            if (!slot.mayPickup(p)) return ItemStack.EMPTY;
            if (!moveItemStackTo(st, 4, 40, true)) return ItemStack.EMPTY;
            slot.onTake(p, copy);
            return copy;
        }
        if (index < 3) {
            if (!moveItemStackTo(st, 4, 40, true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(st, 0, 3, false)) return ItemStack.EMPTY;
        if (st.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public void removed(Player p) {
        super.removed(p);
        access.execute((level, pos) -> clearContainer(p, input));
    }

    @Override
    public boolean stillValid(Player p) { return stillValid(access, p, ModBlocks.RESEARCH_TABLE); }
}
