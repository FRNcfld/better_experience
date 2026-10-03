package com.frnc.better_experience.mixin;

import com.frnc.better_experience.BetterExperienceServerConfig;
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
 *
 * <p><strong>客户端与服务端配置不一致时会怎样</strong>(已对整个原版源码核实):
 * 本注入读的是<strong>本侧</strong>的配置, 所以两侧的 {@code enchantedGoldenAppleBuffEnabled}
 * 不一致时, 两侧拿到的 {@code FoodProperties} 确实不同。但<strong>原版下这既不可观测、也没有行为差异</strong>:
 * <ul>
 *   <li>客户端能读到该返回值的地方只有三处, 且读的都是两套属性<strong>完全相同</strong>的字段:
 *       {@code LivingEntity#shouldTriggerItemUseEffects} 与 {@code Item#getUseDuration} 读
 *       {@code isFastFood()} (强化版与原版皆为 false), {@code Item#use} 读 {@code canAlwaysEat()}
 *       (两者皆为 true)。两套属性真正的差异 (各效果的时长与等级、营养与饱和度) 都不在这几个字段里;</li>
 *   <li>那条效果的施加点在 {@code LivingEntity#addEatEffect}, 其 {@code !pLevel.isClientSide}
 *       判断保证效果只在服务端施加;</li>
 *   <li>饥饿值与饱和度的改动只经过 {@code completeUsingItem()}, 而它被
 *       {@code !this.level().isClientSide} 挡在服务端; 客户端的 {@code FoodData} 本来就靠血量包同步;</li>
 *   <li>原版 1.20.1 <strong>不显示</strong>食物属性, 物品提示与该返回值无关。</li>
 * </ul>
 *
 * <p>唯一的例外是<strong>第三方食物信息模组</strong>(AppleSkin 之类): 它们在客户端读
 * {@code ItemStack#getFoodProperties} 来画饥饿值预览与效果提示, 配置不一致时显示的是客户端那份值,
 * 而实际生效的是服务端那份。属显示层面, 不影响实际效果。
 *
 * <p>之所以<strong>不按逻辑侧分支</strong>来彻底消除这个差异: {@code Item#getUseDuration} 调用时传入的
 * entity 是 {@code null} ({@code pStack.getFoodProperties(null)}), 在那里拿不到可靠的逻辑侧。
 */
@Mixin(Item.class)
public abstract class ItemMixin
{
    @Inject(method = "getFoodProperties", at = @At("HEAD"), cancellable = true)
    private void betterExperience$useVanillaEnchantedGoldenAppleWhenDisabled(CallbackInfoReturnable<FoodProperties> cir)
    {
        if (BetterExperienceServerConfig.enchantedGoldenAppleBuffEnabled) return;
        // 只对附魔金苹果生效,其余物品一律交回原版逻辑
        if ((Object) this != Items.ENCHANTED_GOLDEN_APPLE) return;
        if (EnchantedGoldenAppleFoods.vanilla != null)
        {
            cir.setReturnValue(EnchantedGoldenAppleFoods.vanilla);
        }
    }
}
