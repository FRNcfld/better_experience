package com.frnc.better_experience.doublejump.client;

import com.frnc.better_experience.BetterExperience;
import com.frnc.better_experience.doublejump.network.DoubleJumpNetwork;
import com.frnc.better_experience.doublejump.network.DoubleJumpPacket;
import com.frnc.better_experience.doublejump.network.ToggleDoubleJumpPacket;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BetterExperience.MOD_ID, value = Dist.CLIENT)
public class KeyInputHandler
{
    /** 客户端本地的二段跳开关 (由服务端经 DoubleJumpStatePacket 同步) */
    private static boolean doubleJumpEnabled = true;

    /** 本次腾空是否已用过二段跳 (落地重置), 防止连按叠加 */
    private static boolean usedDoubleJump = false;

    public static boolean isDoubleJumpEnabled()
    {
        return doubleJumpEnabled;
    }

    public static void setDoubleJumpEnabled(boolean enabled)
    {
        doubleJumpEnabled = enabled;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // 落地时重置二段跳次数
        if (mc.player.onGround())
        {
            usedDoubleJump = false;
        }

        if (mc.screen != null) return;

        // 切换二段跳开关 (默认 J 键), 服务端会回发聊天消息确认状态
        if (ClientHandler.TOGGLE_KEY.consumeClick())
        {
            DoubleJumpNetwork.CHANNEL.sendToServer(new ToggleDoubleJumpPacket());
        }
    }

    /**
     * 二段跳: 在空中再次按下跳跃键时, 在客户端直接施加垂直速度。
     * 玩家移动为客户端权威, 服务端设置 motion 会被客户端移动包覆盖, 故必须在此应用;
     * 随后通过 DoubleJumpPacket 通知服务端记录状态/重置摔落距离。
     */
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event)
    {
        if (event.getAction() != InputConstants.PRESS) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        // 判断是否为跳跃键 (尊重按键重绑定)
        if (mc.options.keyJump.getKey().getValue() != event.getKey()) return;

        // 仅在空中触发; 落地后的普通跳跃由原版处理; 飞行/鞘翅/水中/骑乘时不触发
        if (mc.player.onGround() || mc.player.isInWater() || mc.player.isPassenger()
                || mc.player.getAbilities().flying || mc.player.isFallFlying()) return;
        if (!doubleJumpEnabled || usedDoubleJump) return;

        usedDoubleJump = true;

        LocalPlayer player = mc.player;
        Vec3 motion = player.getDeltaMovement();
        player.setDeltaMovement(motion.x, getJumpPower(player), motion.z);
        player.fallDistance = 0;

        DoubleJumpNetwork.CHANNEL.sendToServer(new DoubleJumpPacket());
    }

    /** 复刻 LivingEntity.getJumpPower(): 基础 0.42, 受跳跃提升效果加成 (amplifier + 1 倍) */
    private static float getJumpPower(Player player)
    {
        MobEffectInstance jumpBoost = player.getEffect(MobEffects.JUMP);
        if (jumpBoost != null)
        {
            return (float) (0.42F * (double) (jumpBoost.getAmplifier() + 1));
        }
        return 0.42F;
    }
}
