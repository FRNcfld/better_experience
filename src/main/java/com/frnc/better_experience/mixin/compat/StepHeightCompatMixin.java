package com.frnc.better_experience.mixin.compat;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.extensions.IForgeEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * 神化 (ApothicAttributes / attributeslib) 的玩家上坡高度兼容。
 *
 * <p><strong>问题</strong>: attributeslib 的 {@code IForgeEntityMixin#getStepHeight} 用 {@code @Overwrite}
 * 接管了 Forge 的 {@link IForgeEntity#getStepHeight()}，但它的<strong>玩家分支漏了加上原版基准</strong> ——
 * 玩家直接 {@code return getAttributeValue(STEP_HEIGHT_ADDITION)}，而 Forge 原本是
 * {@code max(0, maxUpStep + 属性值)}。它自己的非玩家分支倒是写对了。
 *
 * <p>而 {@code forge:step_height_addition} 默认值是 <strong>0</strong>，于是装了这个前置之后:
 * <ul>
 *   <li>没穿相关装备的玩家上坡高度直接变成 <strong>0</strong>（连半砖都上不去）；</li>
 *   <li>本模组上坡辅助写进去的 {@code 0.65}（Forge 的<em>增量</em>语义，目标 1.25 减去基准 0.6）
 *       被当成绝对值，有效高度只有 0.65，<strong>上不去一格方块</strong>，表现就是「上坡辅助不生效」。</li>
 * </ul>
 *
 * <p><strong>做法</strong>: 用同一个 {@code @Overwrite} 再接管一次，并把 priority 抬到 attributeslib 之上
 * （它没声明 priority，取默认 1000），由本模组最后生效 —— 于是公式回到 Forge 的
 * {@code max(0, 原版基准 + 属性值)}。属性的「增量」语义因此保持不变: 神化的词缀 / 宝石 / 本模组的上坡辅助
 * 都是按增量加值的，照原样继续工作；没穿装备时恢复成原版的 0.6，能正常上台阶和半砖。
 *
 * <p>玩家之所以不需要单独一个分支: {@code Player} 本来就是 {@code LivingEntity}，
 * Forge 原实现（也就是下面这段）判一次 {@code LivingEntity} 就把玩家一并覆盖了 ——
 * attributeslib 的问题正是它在 {@code LivingEntity} 分支<em>之前</em>多插了一个玩家分支并提前返回。
 *
 * <p><strong>为什么是 {@code @Overwrite} 而不是 {@code @Inject}</strong>: {@code IForgeEntity} 是接口，
 * Mixin 对接口目标用的是 {@code MixinApplicatorInterface}，它一旦在目标方法上解析出任何注入注解
 * （{@code @Inject} / {@code @Redirect} / {@code @ModifyArg} …）就直接抛
 * {@code InvalidInterfaceMixinException}（"is not supported on interface mixin method"），
 * 且它的 {@code applyInjections} 是空实现。接口目标上<strong>只能</strong>走 {@code @Overwrite} 这类
 * "special method"。
 *
 * <p><strong>方法名不能改</strong>: {@code @Overwrite} 是按<strong>方法名 + 描述符</strong>去目标类里找方法
 * （不像 {@code @Inject} 那样可以在注解里写选择器），所以这里必须原样叫 {@code getStepHeight}，
 * 加了 {@code betterExperience$} 前缀会直接报 "Overwrite target was not located in target class"。
 *
 * <p><strong>优先级冲突</strong>: 同一个方法上的多个 {@code @Overwrite}，Mixin 只保留 priority 最大的那个，
 * 其余记一条 {@code Method overwrite conflict ... Skipping method.} 的警告后跳过，不会抛异常。
 * 本模组取 1500，高于 attributeslib 的默认 1000。
 *
 * <p><strong>目标不是原版类</strong>（{@code IForgeEntity} 由 Forge 提供，{@code getStepHeight} 不在 SRG 表里），
 * 所以这里<strong>必须</strong>加 {@code remap = false}。方法体里用到的 {@code Entity#maxUpStep()}、
 * {@code AttributeInstance#getValue()} 是<strong>原版</strong>方法，仍由 better_experience.refmap.json
 * 重映射，不受这个开关影响。
 *
 * <p><strong>另外两个不能照抄的细节</strong>: {@code IForgeEntity#self()} 是 Forge 的
 * <em>private 接口方法</em>，接口 mixin 里调不到，只能 {@code ((Entity) this)} 强转；
 * 又因为 mixin 不许继承自己的目标，也只能靠强转拿到 {@code Entity}。
 *
 * <p><strong>移除条件</strong>: 若将来 attributeslib 修好了玩家分支（把它改成
 * {@code max(0, 基准 + 属性值)}），本 mixin 可以整个删掉；留着也无副作用 —— 两边公式一致，
 * 而且没装 attributeslib 时它就是 Forge 原实现的一个等价复刻。
 */
@Mixin(value = IForgeEntity.class, priority = 1500)
public interface StepHeightCompatMixin
{
    /**
     * 与 attributeslib 同签名再接管一次，靠更高的 priority 胜出。
     *
     * <p>方法体等同于 Forge 的原始实现，只是补回了 attributeslib 玩家分支漏掉的那一项。
     * 写成 {@code default} 是为了和 attributeslib 的写法保持一致（接口方法本来就是 public，
     * 这里也不打算依赖 Mixin 的可见性自动对齐）。
     */
    @Overwrite(remap = false)
    default float getStepHeight()
    {
        // 原版基准: Player 构造时 setMaxUpStep(0.6F)
        float vanillaStep = ((Entity) this).maxUpStep();

        if (this instanceof LivingEntity living)
        {
            AttributeInstance stepHeightAttribute = living.getAttribute(ForgeMod.STEP_HEIGHT_ADDITION.get());
            if (stepHeightAttribute != null)
            {
                return (float) Math.max(0.0D, vanillaStep + stepHeightAttribute.getValue());
            }
        }

        return vanillaStep;
    }
}
