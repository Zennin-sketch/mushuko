package net.zennin.mushoku.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.zennin.mushoku.data.MagicData;
import net.zennin.mushoku.data.ManaData;
import net.zennin.mushoku.data.ModAttachments;
import net.zennin.mushoku.item.ElementBookItem;
import net.zennin.mushoku.item.SpellData;
import net.zennin.mushoku.magic.CircleTraps;
import net.zennin.mushoku.magic.Spell;
import net.zennin.mushoku.network.ModNetworking;
import net.zennin.mushoku.registry.ModItems;

/**
 * Circulo magico. Usos:
 *  - Mano vacia con un hechizo formado (sin lanzar): lo inscribe.
 *  - Piedra del mismo nivel que el hechizo: activa la trampa (se lanza al pisarlo).
 *  - Agachado + piedra del mismo nivel: la imbuye con el hechizo (para baculos).
 *  - Papel o pergamino en blanco: pergamino de hechizo (cuesta el mana del hechizo).
 *  - Circulo vacio + 4 piedras iguales: las mejora a 1 piedra del nivel siguiente.
 *  - Agachado con mano vacia (dueño): borra el hechizo.
 */
public class MagicCircleBlock extends BaseEntityBlock {
    public static final MapCodec<MagicCircleBlock> CODEC = simpleCodec(MagicCircleBlock::new);
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 1, 15);

    public MagicCircleBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(POWERED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(POWERED); }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) { return SHAPE; }

    @Override
    protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new MagicCircleBlockEntity(pos, state); }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return canSupportRigidBlock(level, below) || canSupportCenter(level, below, Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (dir == Direction.DOWN && !canSurvive(state, level, pos)) return Blocks.AIR.defaultBlockState();
        return super.updateShape(state, dir, neighbor, level, pos, neighborPos);
    }

    // ---------- Interaccion ----------

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        int tier = ElementBookItem.stoneLevel(stack);
        boolean paper = stack.is(Items.PAPER) || (stack.is(ModItems.SCROLL) && SpellData.get(stack) == null);
        if (tier == 0 && !paper) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp) || !(level.getBlockEntity(pos) instanceof MagicCircleBlockEntity be))
            return ItemInteractionResult.CONSUME;
        Spell spell = be.spell();

        if (spell == null) {
            if (paper) msg(sp, "msg.remushoku.circle_empty", ChatFormatting.GRAY);
            else upgradeStones(sp, stack, tier, level, pos);
            return ItemInteractionResult.CONSUME;
        }
        if (SpellData.get(stack) != null) { // piedra ya imbuida
            msg(sp, "msg.remushoku.stone_already_imbued", ChatFormatting.GRAY);
            return ItemInteractionResult.CONSUME;
        }
        if (paper) {
            makeScroll(sp, stack, spell, level, pos);
            return ItemInteractionResult.CONSUME;
        }
        if (tier != spell.level.tier) {
            sp.displayClientMessage(Component.translatable("msg.remushoku.stone_wrong_level",
                    Component.translatable(SpellData.stoneFor(spell.level.tier).getDescriptionId())).withStyle(ChatFormatting.RED), true);
            return ItemInteractionResult.CONSUME;
        }
        if (player.isShiftKeyDown()) {
            ItemStack imbued = SpellData.with(new ItemStack(stack.getItem()), spell);
            consume(sp, stack, 1);
            give(sp, imbued);
            level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1f, 1.2f);
            sp.displayClientMessage(Component.translatable("msg.remushoku.stone_imbued",
                    Component.translatable(spell.translationKey())).withStyle(ChatFormatting.AQUA), true);
        } else {
            if (be.armed()) {
                msg(sp, "msg.remushoku.circle_already_armed", ChatFormatting.GRAY);
                return ItemInteractionResult.CONSUME;
            }
            consume(sp, stack, 1);
            be.setArmed(true);
            level.setBlock(pos, state.setValue(POWERED, true), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.8f, 1.5f);
            msg(sp, "msg.remushoku.circle_armed", ChatFormatting.LIGHT_PURPLE);
        }
        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp) || !(level.getBlockEntity(pos) instanceof MagicCircleBlockEntity be))
            return InteractionResult.CONSUME;
        Spell spell = be.spell();
        if (spell != null && player.isShiftKeyDown()) {
            if (!be.isOwner(sp.getUUID())) {
                msg(sp, "msg.remushoku.not_owner", ChatFormatting.RED);
                return InteractionResult.CONSUME;
            }
            be.setSpell(null);
            level.setBlock(pos, state.setValue(POWERED, false), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.SAND_BREAK, SoundSource.BLOCKS, 1f, 1f);
            msg(sp, "msg.remushoku.circle_erased", ChatFormatting.GRAY);
            return InteractionResult.CONSUME;
        }
        MagicData m = ModAttachments.magic(sp);
        Spell queued = m.queued().isEmpty() ? null : Spell.byId(m.queued());
        if (queued != null) {
            if (be.owner() == null) be.setOwner(sp.getUUID());
            be.setSpell(queued);
            level.setBlock(pos, state.setValue(POWERED, false), Block.UPDATE_ALL);
            ModAttachments.setMagic(sp, m.withQueued(""));
            ModNetworking.syncMagic(sp);
            level.playSound(null, pos, SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 0.8f, 1.3f);
            ((ServerLevel) level).sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,
                    pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, 40, 0.5, 0.2, 0.5, 0.6);
            sp.displayClientMessage(Component.translatable("msg.remushoku.circle_inscribed",
                    Component.translatable(queued.translationKey())).withStyle(ChatFormatting.AQUA), true);
        } else if (spell != null) {
            sp.displayClientMessage(Component.translatable(be.armed() ? "msg.remushoku.circle_info_armed" : "msg.remushoku.circle_info",
                    Component.translatable(spell.translationKey()),
                    Component.translatable("level.remushoku." + spell.level.tier)).withStyle(ChatFormatting.AQUA), true);
        } else {
            msg(sp, "msg.remushoku.circle_empty", ChatFormatting.GRAY);
        }
        return InteractionResult.CONSUME;
    }

    // ---------- Trampa ----------

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || !state.getValue(POWERED) || !(entity instanceof LivingEntity target) || !target.isAlive()) return;
        if (!(level.getBlockEntity(pos) instanceof MagicCircleBlockEntity be) || !be.armed()) return;
        Spell spell = be.spell();
        if (spell == null) return;
        boolean friendly = CircleTraps.isFriendly(spell);
        boolean isOwner = be.owner() != null && be.owner().equals(target.getUUID());
        if (isOwner && !friendly) return; // al dueño no le afecta su propia trampa
        be.setArmed(false);
        level.setBlock(pos, state.setValue(POWERED, false), Block.UPDATE_ALL);
        Player owner = be.owner() == null ? null : level.getPlayerByUUID(be.owner());
        CircleTraps.trigger((ServerLevel) level, pos, spell, owner, target);
    }

    // ---------- Linea de tiza azul ----------

    /** Una linea de tiza azul con senal pegada al circulo lo activa, como si alguien lo pisara. */
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
        super.neighborChanged(state, level, pos, block, from, moving);
        if (level.isClientSide || !(level.getBlockEntity(pos) instanceof MagicCircleBlockEntity be)) return;
        boolean powered = linePowered(level, pos);
        boolean rising = powered && !be.linePowered;
        be.linePowered = powered;
        if (!rising || !state.getValue(POWERED) || !be.armed()) return;
        Spell spell = be.spell();
        if (spell == null) return;
        // Se activa como si alguien lo pisara: le afecta a todo lo que este sobre el circulo o pegado (hasta 1.5
        // bloques); si no hay nadie, el hechizo se lanza igual en el circulo (se ve, suena y explota si es de fuego).
        boolean friendly = CircleTraps.isFriendly(spell);
        net.minecraft.world.phys.Vec3 c = net.minecraft.world.phys.Vec3.atBottomCenterOf(pos);
        java.util.List<LivingEntity> hit = new java.util.ArrayList<>();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(1.5, 1, 1.5))) {
            if (!e.isAlive() || e.isSpectator()) continue;
            boolean isOwner = be.owner() != null && be.owner().equals(e.getUUID());
            if (isOwner && !friendly) continue;
            hit.add(e);
        }
        hit.sort(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(c)));
        be.setArmed(false);
        level.setBlock(pos, state.setValue(POWERED, false), Block.UPDATE_ALL);
        Player owner = be.owner() == null ? null : level.getPlayerByUUID(be.owner());
        if (hit.isEmpty()) CircleTraps.trigger((ServerLevel) level, pos, spell, owner, null);
        else CircleTraps.trigger((ServerLevel) level, pos, spell, owner, hit.get(0));
        for (int k = 1; k < hit.size(); k++) CircleTraps.applyOnly((ServerLevel) level, pos, spell, owner, hit.get(k));
    }

    /** Hay una linea de tiza azul con senal tocando el circulo (al lado, o subiendo/bajando un bloque). */
    private static boolean linePowered(Level level, BlockPos pos) {
        for (Direction d : Direction.Plane.HORIZONTAL) {
            for (BlockPos q : new BlockPos[]{pos.relative(d), pos.relative(d).above(), pos.relative(d).below()}) {
                BlockState n = level.getBlockState(q);
                if (n.getBlock() instanceof ChalkLineBlock && n.getValue(ChalkLineBlock.CHARGED) && n.getValue(ChalkLineBlock.POWER) > 0) return true;
            }
        }
        return false;
    }

    // ---------- Ayudas ----------

    private static void upgradeStones(ServerPlayer sp, ItemStack stack, int tier, Level level, BlockPos pos) {
        if (SpellData.get(stack) != null) { msg(sp, "msg.remushoku.stone_already_imbued", ChatFormatting.GRAY); return; }
        if (tier >= 6) { msg(sp, "msg.remushoku.stone_max", ChatFormatting.GRAY); return; }
        if (stack.getCount() < 4 && !sp.getAbilities().instabuild) { msg(sp, "msg.remushoku.stone_need_four", ChatFormatting.RED); return; }
        consume(sp, stack, 4);
        ItemStack up = new ItemStack(SpellData.stoneFor(tier + 1));
        give(sp, up);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2f, 0.8f);
        ((ServerLevel) level).sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, 30, 0.4, 0.4, 0.4, 0.05);
        sp.displayClientMessage(Component.translatable("msg.remushoku.stone_upgraded",
                Component.translatable(up.getItem().getDescriptionId())).withStyle(ChatFormatting.GOLD), true);
    }

    private static void makeScroll(ServerPlayer sp, ItemStack paper, Spell spell, Level level, BlockPos pos) {
        ManaData mana = ModAttachments.get(sp);
        if (!sp.getAbilities().instabuild) {
            if (mana.mana() < spell.level.cost) { msg(sp, "msg.remushoku.no_mana", ChatFormatting.RED); return; }
            ModAttachments.set(sp, mana.withMana(mana.mana() - spell.level.cost));
            ModNetworking.syncMana(sp);
        }
        consume(sp, paper, 1);
        give(sp, SpellData.with(new ItemStack(ModItems.SCROLL), spell));
        level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1f, 1f);
        sp.displayClientMessage(Component.translatable("msg.remushoku.scroll_made",
                Component.translatable(spell.translationKey())).withStyle(ChatFormatting.AQUA), true);
    }

    static void consume(Player p, ItemStack stack, int n) {
        if (!p.getAbilities().instabuild) stack.shrink(n);
    }

    static void give(Player p, ItemStack stack) {
        if (!p.getInventory().add(stack)) p.drop(stack, false);
    }

    private static void msg(ServerPlayer sp, String key, ChatFormatting color) {
        sp.displayClientMessage(Component.translatable(key).withStyle(color), true);
    }
}
