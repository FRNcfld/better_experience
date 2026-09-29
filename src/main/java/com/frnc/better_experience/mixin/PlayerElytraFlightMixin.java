package com.frnc.better_experience.mixin;

import com.frnc.better_experience.elytraflight.ElytraFlightHandler;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 鞘翅飞行开关的拦截点: 关闭时不允许起飞。
 *
 * 原版 {@code Player#tryToStartFallFlying} 是鞘翅起飞的唯一公共入口 ——
 *   - 客户端: LocalPlayer.aiStep 在按下跳跃时调用它, 返回 true 才发出
 *     ServerboundPlayerCommandPacket(START_FALL_FLYING);
 *   - 服务端: ServerGamePacketListenerImpl 收到该包后同样调用它。
 * 且 LocalPlayer / ServerPlayer 都未覆写该方法, 故这一个注入即可同时覆盖两侧:
 * 关闭时客户端发不出起飞包, 服务端也不会进入滑翔 (换客户端无法绕过)。
 *
 * 注: 目标为原版类, 方法名需经 better_experience.refmap.json 重映射到 SRG, 因此此处
 * 不可加 remap = false (与仓库内针对第三方模组的 mixin 相反)。
 */
@Mixin(Player.class)
public abstract class PlayerElytraFlightMixin
{
    @Inject(method = "tryToStartFallFlying", at = @At("HEAD"), cancellable = true)
    private void betterExperience$blockElytraFlight(CallbackInfoReturnable<Boolean> cir)
    {
        Player self = (Player) (Object) this;
        if (!ElytraFlightHandler.isFlightAllowed(self))
        {
            cir.setReturnValue(false);
        }
    }
}
