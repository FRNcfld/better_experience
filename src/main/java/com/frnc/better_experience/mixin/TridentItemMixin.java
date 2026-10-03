package com.frnc.better_experience.mixin;

import com.frnc.better_experience.oceanblessing.OceanBlessingRules;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.TridentItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 海洋之祝: 取消激流"只能在水中或雨中生效"的限制。
 *
 * <p>整个原版源码里对激流的降水判定只有两处, 都在 {@link TridentItem} 内, 且各自只调用一次
 * {@code Player#isInWaterOrRain} (已在字节码中逐处核对过调用点与 owner):
 * <ul>
 *   <li>{@code use} —— {@code getRiptide(stack) > 0 && !player.isInWaterOrRain()} 直接 fail,
 *       压根不允许开始蓄力;</li>
 *   <li>{@code releaseUsing} —— {@code if (j <= 0 || player.isInWaterOrRain())} 这道守卫包住整段,
 *       不满足则既不投掷也不冲刺。</li>
 * </ul>
 * 两处都只要让这个布尔判定返回 true 就能放行, 所以用 {@code @Redirect} 而不重写方法体 ——
 * 重写等于把原版逻辑抄一份 (含 {@code releaseUsing} 里四十来行的激流冲刺与音效选取), 容易和上游脱节。
 *
 * <p><strong>解开守卫后陆地激流是完整的</strong>, 不需要再动别处: 冲刺的推进力是 {@code releaseUsing}
 * 守卫内 {@code player.push(...)} 无条件给的, 而 {@code Player} 里读 {@code isAutoSpinAttack()} 的地方
 * 只影响姿势判定 (SPIN_ATTACK), 不要求水。
 *
 * <p><strong>目标必须按字节码里的 owner 写</strong>: 该调用的 owner 是
 * {@code net/minecraft/world/entity/player/Player} (接收者的静态类型), 尽管方法实际声明在 {@code Entity} 上。
 *
 * <p>目标为原版类, 方法名需经 better_experience.refmap.json 重映射到 SRG, 因此不可加 remap = false。
 */
@Mixin(TridentItem.class)
public abstract class TridentItemMixin
{
    /** {@code use}: 带海洋之祝时, 不在水中 / 雨中也能开始蓄力激流 */
    @Redirect(method = "use", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;isInWaterOrRain()Z"))
    private boolean betterExperience$allowRiptideOutOfWaterOnUse(Player player)
    {
        return player.isInWaterOrRain() || OceanBlessingRules.liftsRiptideWaterRestriction(player);
    }

    /** {@code releaseUsing}: 带海洋之祝时, 不在水中 / 雨中也能放出激流冲刺 */
    @Redirect(method = "releaseUsing", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;isInWaterOrRain()Z"))
    private boolean betterExperience$allowRiptideOutOfWaterOnRelease(Player player)
    {
        return player.isInWaterOrRain() || OceanBlessingRules.liftsRiptideWaterRestriction(player);
    }
}
