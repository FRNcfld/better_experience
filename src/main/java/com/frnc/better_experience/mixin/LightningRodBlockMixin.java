package com.frnc.better_experience.mixin;

import com.frnc.better_experience.oceanblessing.OceanBlessingRules;

import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 海洋之祝: 让"引雷打避雷针"这一路也免除雷雨天要求。
 *
 * <p><strong>引雷其实有两个落雷点, 缺一个都会表现为"部分场景不生效":</strong>
 * <ul>
 *   <li>{@code ThrownTrident#onHitEntity} —— <em>命中生物</em>时在生物脚下落雷, 见 {@link ThrownTridentMixin};</li>
 *   <li>{@link LightningRodBlock#onProjectileHit} —— <em>投掷物命中避雷针方块</em>时在避雷针处落雷。
 *       这一路不经过 {@code ThrownTrident} 的方法, 而是由方块侧的 {@code BlockState#onProjectileHit} 分发,
 *       所以只改投掷物那边会漏掉它 (雷雨天能打避雷针, 晴天带海洋之祝却打不了, 就是这个原因)。</li>
 * </ul>
 *
 * <p><strong>做法:</strong> 不重写原版那段落雷逻辑, 只把它条件里的"是否雷雨"补上 ——
 * {@code HEAD} 处先记下"本次命中是不是一把带海洋之祝的三叉戟", 再把
 * {@code Level#isThundering()} 的返回值改成"雷雨天 <em>或</em> 刚才记下的条件"。
 * 于是落雷位置、{@code canSeeSky} 判定、引雷附魔判定、音效统统仍由原版执行。
 *
 * <p>两处注入靠"同一次方法调用内 HEAD 一定先于 redirect"配合, 这是确定的执行顺序 (HEAD 在方法最前面),
 * 不需要额外同步; 该字段每次进入方法都会先置回 false, 所以也不会残留上一次的状态。
 * {@code onProjectileHit} 内只有这一处 {@code isThundering()} 调用 (已核字节码),
 * 另一个在 {@code animateTick} 里, 那是天气氛围粒子, 与本附魔无关, 故不动。
 *
 * <p>目标为原版类, 方法名需经 better_experience.refmap.json 重映射到 SRG, 因此不可加 remap = false。
 */
@Mixin(LightningRodBlock.class)
public abstract class LightningRodBlockMixin
{
    /** 本次 {@code onProjectileHit} 的投掷物是否为"带海洋之祝的三叉戟"; 每次进入方法都会重算 */
    private boolean betterExperience$blessedTrident;

    @Inject(method = "onProjectileHit", at = @At("HEAD"))
    private void betterExperience$inspectProjectile(Level level, BlockState state, BlockHitResult hit,
            Projectile projectile, CallbackInfo ci)
    {
        this.betterExperience$blessedTrident = projectile instanceof ThrownTrident trident
                && OceanBlessingRules.liftsChannelingWeatherRestriction(
                        ((ThrownTridentAccessor) trident).betterExperience$tridentItem());
    }

    @Redirect(method = "onProjectileHit", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;isThundering()Z"))
    private boolean betterExperience$thunderingOrBlessed(Level level)
    {
        // 交回原版时这里就是原值; 只在晴天 + 带海洋之祝时补成 true
        return level.isThundering() || this.betterExperience$blessedTrident;
    }
}
