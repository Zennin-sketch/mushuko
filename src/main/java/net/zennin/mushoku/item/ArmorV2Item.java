package net.zennin.mushoku.item;

import net.minecraft.ChatFormatting;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.function.Consumer;

/**
 * Armadura rediseñada (v2) con modelo 3D de GeckoLib (tools/armor/armor_v2.py). Hace lo mismo que la original
 * (ver ArmorV2.is) y solo se consigue en creativo, para comparar con la version vieja.
 */
public class ArmorV2Item extends ArmorItem implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public final String model;
    public final boolean glows;
    private final String tip;

    public ArmorV2Item(Holder<ArmorMaterial> material, Type type, Properties props, String model, boolean glows, String tip) {
        super(material, type, props);
        this.model = model;
        this.glows = glows;
        this.tip = tip;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private Object renderer;

            @Override
            public <T extends LivingEntity> HumanoidModel<?> getGeoArmorRenderer(T entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<T> original) {
                if (renderer == null) renderer = net.zennin.mushoku.client.ArmorV2Renderer.create(model, glows);
                return (HumanoidModel<?>) renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {}

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    @Override
    public void appendHoverText(ItemStack s, TooltipContext c, List<Component> lines, TooltipFlag f) {
        if (tip != null) lines.add(Component.translatable(tip).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.remushoku.v2_test").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));
    }
}
