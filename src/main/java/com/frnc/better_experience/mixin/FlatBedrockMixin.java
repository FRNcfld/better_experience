package com.frnc.better_experience.mixin;

import com.frnc.better_experience.BetterExperienceServerConfig;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 平坦基岩 (来源: Flat Bedrock 的 {@code FlatBedrockMixin})。
 *
 * <p>1.19.2 起基岩层由<strong>数据驱动</strong>的垂直渐变条件生成。原版 noise_settings 里写的是
 * {@code sequence[0]} 的 {@code if_true} 节点:
 *
 * <pre>
 * {"type": "minecraft:vertical_gradient", "random_name": "minecraft:bedrock_floor",
 *  "true_at_and_below": {"above_bottom": 0}, "false_at_and_above": {"above_bottom": 5}}
 * </pre>
 *
 * 即「y 低于底部 0 格为真、高于底部 5 格为假」, 中间是随机的锯齿状基岩。下界天花板同理, 用的是
 * {@code random_name} 为 {@code minecraft:bedrock_roof}、锚点为 {@code below_top} 的另一条规则。
 *
 * <p>把 {@code falseAtAndAbove} 改成 {@code aboveBottom(1)}, 条件就只在最底层为真 → 基岩压成 1 格;
 * 天花板则把 {@code trueAtAndBelow} 改成 {@code belowTop(1)}, 只在最顶那一层为真。
 *
 * <p><strong>为什么用 Mixin 而不是数据包</strong>: 规则确实在 JSON 里, 但整份 noise_settings 有
 * 100KB 以上, 覆盖它要复制 6 份巨型 JSON, 且会和其它世界生成模组冲突。改这里一处即可同时覆盖
 * 主世界、下界、以及所有复用同款 {@code random_name} 的模组维度 (原 mod 为此额外写了
 * BOP / TerraBlender 兼容 Mixin, 本方案不需要)。
 *
 * <p>目标是原版类, 因此<strong>不可加 {@code remap = false}</strong>: 字段 {@code randomName} 与两个
 * 方法名都要靠 better_experience.refmap.json 重映射到 SRG (与仓库内针对第三方模组的 mixin 相反)。
 *
 * <p>该 mixin 放在配置的 {@code mixins} (双端) 列表里——世界生成在专用服务端也要压平。
 */
@Mixin(targets = "net.minecraft.world.level.levelgen.SurfaceRules$VerticalGradientConditionSource")
public abstract class FlatBedrockMixin
{
    /** 主世界地板 / 下界地板的规则名 */
    private static final ResourceLocation BEDROCK_FLOOR =
            ResourceLocation.fromNamespaceAndPath("minecraft", "bedrock_floor");

    /** 下界天花板的规则名 */
    private static final ResourceLocation BEDROCK_ROOF =
            ResourceLocation.fromNamespaceAndPath("minecraft", "bedrock_roof");

    /** 目标 record 的第一个分量: 用来区分这条规则是地板还是天花板 */
    @Shadow
    @Final
    private ResourceLocation randomName;

    /** 地板: 原版 falseAtAndAbove = aboveBottom(5) → 改成 aboveBottom(1), 只留最底层 */
    @Inject(method = "falseAtAndAbove", at = @At("HEAD"), cancellable = true)
    private void betterExperience$flattenBedrockFloor(CallbackInfoReturnable<VerticalAnchor> cir)
    {
        if (!BetterExperienceServerConfig.flatBedrockEnabled) return;

        if (BEDROCK_FLOOR.equals(this.randomName))
        {
            cir.setReturnValue(VerticalAnchor.aboveBottom(1));
        }
    }

    /** 天花板: 原版 trueAtAndBelow = belowTop(5) → 改成 belowTop(1), 只留最顶层 */
    @Inject(method = "trueAtAndBelow", at = @At("HEAD"), cancellable = true)
    private void betterExperience$flattenBedrockRoof(CallbackInfoReturnable<VerticalAnchor> cir)
    {
        if (!BetterExperienceServerConfig.flatBedrockEnabled) return;

        if (BEDROCK_ROOF.equals(this.randomName))
        {
            cir.setReturnValue(VerticalAnchor.belowTop(1));
        }
    }
}
