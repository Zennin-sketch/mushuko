package net.zennin.mushoku.addon;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.zennin.mushoku.MushokuMod;
import net.zennin.mushoku.data.ManaData;
import net.zennin.mushoku.data.ModAttachments;
import net.zennin.mushoku.network.ModNetworking;
import net.zennin.mushoku.registry.ModItems;

import java.util.List;
import java.util.Map;

/** Objetos del addon: piedras de mana, armaduras magicas, Lanza Superd, Hoja de Piedra, libro de intercambio y la cura. */
public final class AddonItems {
    private AddonItems() {}

    // ---------- Maldicion Superd (efecto) ----------
    public static final Holder<MobEffect> SUPERD_CURSE = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, MushokuMod.id("superd_curse"),
            new MobEffect(MobEffectCategory.HARMFUL, 0x2E5A3A) {});

    // ---------- Piedras de mana: recuperan mana (usos limitados) ----------
    public static final Item INTERMEDIATE_MANA_STONE = reg("intermediate_mana_stone", new ManaStone(200, 16));
    public static final Item ADVANCED_MANA_STONE = reg("advanced_mana_stone", new ManaStone(500, 24));
    public static final Item SAINT_MANA_STONE = reg("saint_mana_stone", new ManaStone(1500, 32));
    public static final Item KING_MANA_STONE = reg("king_mana_stone", new ManaStone(4000, 48));

    // ---------- Armas ----------
    public static final Item STONE_BLADE = reg("stone_blade", new SwordItem(Tiers.IRON,
            new Item.Properties().attributes(SwordItem.createAttributes(Tiers.IRON, 4, -2.0f))) {
        @Override
        public void appendHoverText(ItemStack s, TooltipContext c, List<Component> l, TooltipFlag f) { tip(l, "tooltip.remushoku.stone_blade"); }
    });
    public static final Item SUPERD_SPEAR = reg("superd_spear", new SuperdSpearItem(new Item.Properties().durability(3500)
            .attributes(ItemAttributeModifiers.builder()
                    .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(net.minecraft.resources.ResourceLocation.withDefaultNamespace("base_attack_damage"), 9, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                    .add(Attributes.ATTACK_SPEED, new AttributeModifier(net.minecraft.resources.ResourceLocation.withDefaultNamespace("base_attack_speed"), -2.4, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                    .build())));

    // ---------- Armaduras magicas ----------
    private static Holder<ArmorMaterial> material(String name, int defense) {
        return Registry.registerForHolder(BuiltInRegistries.ARMOR_MATERIAL, MushokuMod.id(name), new ArmorMaterial(
                Map.of(ArmorItem.Type.HELMET, defense, ArmorItem.Type.CHESTPLATE, defense, ArmorItem.Type.LEGGINGS, defense,
                        ArmorItem.Type.BOOTS, defense, ArmorItem.Type.BODY, defense),
                15, SoundEvents.ARMOR_EQUIP_LEATHER, () -> Ingredient.of(ModItems.INTERMEDIATE_STONE),
                List.of(new ArmorMaterial.Layer(MushokuMod.id(name))), 0f, 0f));
    }

    public static final Item CURSE_SUPPRESSING_HELMET = reg("curse_suppressing_helmet",
            new TipArmor(material("curse_helmet", 3), ArmorItem.Type.HELMET, 40, "tooltip.remushoku.curse_helmet"));
    public static final Item MAGICAL_SUNGLASSES = reg("magical_sunglasses",
            new TipArmor(material("sunglasses", 1), ArmorItem.Type.HELMET, 20, "tooltip.remushoku.sunglasses"));
    public static final Item ENCHANTED_SHOES = reg("enchanted_shoes",
            new TipArmor(material("enchanted_shoes", 2), ArmorItem.Type.BOOTS, 20, "tooltip.remushoku.enchanted_shoes"));
    public static final Item FLAME_TRAPPING_CLOAK = reg("flame_trapping_cloak",
            new TipArmor(material("flame_cloak", 3), ArmorItem.Type.CHESTPLATE, 20, "tooltip.remushoku.flame_cloak"));

    // ---------- Otros ----------
    public static final Item TRADING_BOOK = reg("trading_book", new Item(new Item.Properties().stacksTo(1)) {
        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            if (player instanceof ServerPlayer sp) net.zennin.mushoku.guild.GuildSystem.open(sp, "trading");
            return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
        }

        @Override
        public void appendHoverText(ItemStack s, TooltipContext c, List<Component> l, TooltipFlag f) { tip(l, "tooltip.remushoku.trading_book"); }
    });
    public static final Item SUPERD_CURSE_CURE = reg("superd_curse_cure", new Item(new Item.Properties().stacksTo(16)
            .food(new FoodProperties.Builder().nutrition(4).saturationModifier(1.2f).alwaysEdible().build())) {
        @Override
        public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity e) {
            if (!level.isClientSide && e.hasEffect(SUPERD_CURSE)) {
                e.removeEffect(SUPERD_CURSE);
                if (e instanceof Player p) p.displayClientMessage(Component.translatable("msg.remushoku.curse_cured").withStyle(ChatFormatting.GREEN), false);
            }
            return super.finishUsingItem(stack, level, e);
        }

        @Override
        public void appendHoverText(ItemStack s, TooltipContext c, List<Component> l, TooltipFlag f) { tip(l, "tooltip.remushoku.curse_cure"); }
    });

    private static Item reg(String name, Item item) {
        Registry.register(BuiltInRegistries.ITEM, MushokuMod.id(name), item);
        ModItems.ALL.add(item);
        return item;
    }

    static void tip(List<Component> l, String key) { l.add(Component.translatable(key).withStyle(ChatFormatting.GRAY)); }

    public static void init() {}

    /** Piedra de mana: clic derecho recupera mana. */
    public static class ManaStone extends Item {
        private final int mana;

        public ManaStone(int mana, int uses) {
            super(new Item.Properties().durability(uses));
            this.mana = mana;
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack s = player.getItemInHand(hand);
            if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(s);
            ManaData m = ModAttachments.get(sp);
            if (m.mana() >= m.maxMana()) {
                sp.displayClientMessage(Component.translatable("msg.remushoku.mana_full").withStyle(ChatFormatting.GRAY), true);
                return InteractionResultHolder.fail(s);
            }
            ModAttachments.set(sp, m.withMana(m.mana() + mana));
            ModNetworking.syncMana(sp);
            s.hurtAndBreak(1, sp, LivingEntity.getSlotForHand(hand));
            sp.getCooldowns().addCooldown(this, 20);
            level.playSound(null, sp.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1f, 1.4f);
            return InteractionResultHolder.consume(s);
        }

        @Override
        public void appendHoverText(ItemStack s, TooltipContext c, List<Component> l, TooltipFlag f) {
            l.add(Component.translatable("tooltip.remushoku.mana_stone", mana).withStyle(ChatFormatting.AQUA));
        }
    }

    public static class TipArmor extends ArmorItem {
        private final String tip;

        public TipArmor(Holder<ArmorMaterial> m, Type t, int durabilityMult, String tip) {
            super(m, t, new Item.Properties().durability(t.getDurability(durabilityMult)));
            this.tip = tip;
        }

        @Override
        public void appendHoverText(ItemStack s, TooltipContext c, List<Component> l, TooltipFlag f) { tip(l, tip); }
    }
}
