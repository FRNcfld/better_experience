package com.frnc.better_experience.mixin;

import com.frnc.better_experience.Config;
import com.frnc.better_experience.goldenapple.EnchantedGoldenAppleFoods;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 按配置 {@code enchantedGoldenAppleBuffEnabled} 决定附魔金苹果是否使用强化后的食物属性。
 *
 * <p><strong>为什么在这里判断,而不是在 {@link FoodsMixin} 里读配置:</strong>
 * <ol>
 *   <li>{@code Items.ENCHANTED_GOLDEN_APPLE} 在 {@code Items.<clinit>} 构造时就把
 *       {@code FoodProperties} <em>按引用</em>拷进了自己的 private 字段,所以事后改
 *       {@code Foods.ENCHANTED_GOLDEN_APPLE} 不会影响已经建好的物品——必须在"取用"时判断。</li>
 *   <li>{@code Foods.<clinit>} 与配置加载的先后顺序取决于 Forge 内部调度
 *       (vanilla bootstrap 是通过 {@code BackgroundWaiter.runAndTick} 跑的),不该依赖。</li>
 * </ol>
 * 在取用时刻判断,配置必然已加载;附带好处是改了配置无需重启即可生效。
 *
 * <p><strong>调用链</strong>(已在字节码中逐段核对):
 * {@code LivingEntity.addEatEffect} → {@code ItemStack.getFoodProperties(LivingEntity)}
 * (Forge 默认方法) → {@code Item.getFoodProperties(ItemStack, LivingEntity)}
 * (Forge 默认方法) → {@code Item.getFoodProperties()} ← 本注入点。
 */
@Mixin(Item.class)
public abstract class ItemMixin
{
    @Inject(method = "getFoodProperties", at = @At("HEAD"), cancellable = true)
    private void betterExperience$useVanillaEnchantedGoldenAppleWhenDisabled(CallbackInfoReturnable<FoodProperties> cir)
    {
        if (Config.enchantedGoldenAppleBuffEnabled) return;
        // 只对附魔金苹果生效,其余物品一律交回原版逻辑
        if ((Object) this != Items.ENCHANTED_GOLDEN_APPLE) return;
        if (EnchantedGoldenAppleFoods.vanilla != null)
        {
            cir.setReturnValue(EnchantedGoldenAppleFoods.vanilla);
        }
    }
}
