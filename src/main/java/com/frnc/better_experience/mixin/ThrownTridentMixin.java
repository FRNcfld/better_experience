package com.frnc.better_experience.mixin;

import com.frnc.better_experience.oceanblessing.OceanBlessingRules;

import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 海洋之祝: 取消引雷"只能在雷雨天生效"的限制 —— <strong>命中生物</strong>这一条落雷路径。
 *
 * <p>落雷由<strong>投掷出去</strong>的三叉戟触发 (原地旋转攻击不落雷), 判定集中在
 * {@code ThrownTrident#onHitEntity}。整个方法里 {@code Level#isThundering} 只被调用一次,
 * 且它是落雷分支三个条件中的第二个:
 * {@code level instanceof ServerLevel && level.isThundering() && this.isChanneling()}。
 * 因此把这一处的返回值改成"雷雨天 <em>或</em> 这枚三叉戟带海洋之祝"即可 ——
 * {@code isChanneling()} 仍会照常要求真的附了引雷, 不会凭海洋之祝就落雷。
 *
 * <p><strong>注意这只是两条落雷路径之一。</strong>另一条是"投掷物命中避雷针方块", 它在方块侧
 * {@code LightningRodBlock#onProjectileHit} 里, 由 {@link LightningRodBlockMixin} 处理 ——
 * 两处都改才等于"引雷+海洋之祝 等同于 引雷+雷雨天"。
 *
 * <p>物品栈经由 {@link ThrownTridentAccessor} 读取 (原版 {@code getPickupItem()} 是 protected,
 * 方块侧那条路径拿不到), 两侧共用同一个访问器。
 *
 * <p>目标为原版类, 方法名需经 better_experience.refmap.json 重映射到 SRG, 因此不可加 remap = false。
 */
@Mixin(ThrownTrident.class)
public abstract class ThrownTridentMixin implements ThrownTridentAccessor
{
    /** {@code onHitEntity}: 带海洋之祝时, 不在雷雨天也能落雷 */
    @Redirect(method = "onHitEntity", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;isThundering()Z"))
    private boolean betterExperience$allowChannelingInAnyWeather(Level level)
    {
        return level.isThundering()
                || OceanBlessingRules.liftsChannelingWeatherRestriction(this.betterExperience$tridentItem());
    }
}
