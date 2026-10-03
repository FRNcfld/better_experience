package com.frnc.better_experience.stepassist;

import com.frnc.better_experience.BetterExperience;
import com.frnc.better_experience.BetterExperienceServerConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 上坡辅助的核心: 每个客户端 tick 末尾按当前模式改写本地玩家的
 * {@code forge:step_height_addition} 属性 (来源: Accessible Step 的 {@code StepHeightHandler})。
 *
 * <p><strong>为什么用属性而不是 {@code Entity#maxUpStep}</strong>: Forge 的有效上坡高度是
 * {@code maxUpStep + forge:step_height_addition}, 见 {@code IForgeEntity#getStepHeight()},
 * 而 {@code Entity#move} 走的正是这个 {@code getStepHeight()}。所以属性值要写成
 * <em>目标高度 - 原版基准 0.6</em>。
 *
 * <p><strong>为什么纯客户端就够</strong>: 1.20.1 的玩家移动是客户端权威, 位置由客户端算出后
 * 发给服务端; 来源模组也没有任何自定义数据包。代价是上坡高度过高时服务端可能不接受该位移,
 * 所以配置注释里保留了来源模组的多人游戏警告。
 *
 * <p>模式是<strong>会话级</strong>的: 初值取配置项 {@code stepAssistMode}, 游戏内用按键循环
 * (默认不绑定按键, 需在「控制」中自行设置), 不落盘。与仓库内二段跳 / 鞘翅飞行的开关行为一致。
 *
 * <p>{@link StepAssistMode#OFF} 这一档就是本功能的游戏内开关; 配置项 {@code stepAssistEnabled}
 * 则是功能级总开关, 关掉时按键无效、上坡高度恒为原版。
 */
@Mod.EventBusSubscriber(modid = BetterExperience.MOD_ID, value = Dist.CLIENT)
public class StepAssistHandler
{
    /** 原版玩家基准上坡高度 (Player 构造时 {@code setMaxUpStep(0.6F)}) */
    private static final double VANILLA_STEP_HEIGHT = 0.6D;

    private static StepAssistMode mode = BetterExperienceServerConfig.stepAssistMode;

    public static StepAssistMode getMode()
    {
        return mode;
    }

    /** 循环到下一个模式 (由按键触发) */
    public static void cycleMode()
    {
        mode = mode.next();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        // 功能级总开关关闭: 只还原原版上坡高度, 并且完全不碰原版「自动跳跃」选项——
        // 否则会在玩家没要求的情况下把他自己的自动跳跃设置改掉
        if (!BetterExperienceServerConfig.stepAssistEnabled)
        {
            applyStepHeight(player, VANILLA_STEP_HEIGHT);
            return;
        }

        // 三种模式互斥: AUTO_JUMP 借用原版自动跳跃选项, 其余模式把它关掉。
        // OptionInstance#set 内部有 Objects.equals 短路, 值没变时既不会重复回调也不会写 options.txt。
        mc.options.autoJump().set(mode == StepAssistMode.AUTO_JUMP);

        applyStepHeight(player, targetStepHeight(player, mc));
    }

    /** 按当前模式与玩家动作状态算出目标上坡高度 */
    private static double targetStepHeight(LocalPlayer player, Minecraft mc)
    {
        if (mode != StepAssistMode.STEP) return VANILLA_STEP_HEIGHT;

        if (player.isShiftKeyDown()) return BetterExperienceServerConfig.stepAssistSneakHeight;

        // 疾跑键按住但尚未真正进入疾跑 (例如刚起步、或顶着墙) 也按疾跑算;
        // 且必须有前进输入, 否则站着按疾跑键不该抬高上坡高度
        boolean sprinting = player.isSprinting() || mc.options.keySprint.isDown();
        if (sprinting && player.input.hasForwardImpulse()) return BetterExperienceServerConfig.stepAssistSprintHeight;

        return BetterExperienceServerConfig.stepAssistStepHeight;
    }

    /**
     * 把目标高度写进 {@code forge:step_height_addition}。
     *
     * <p>Forge 会把它与 {@code maxUpStep} 相加 (并夹到非负), 所以这里减去 0.6 的基准值。
     * {@code AttributeInstance#setBaseValue} 自带相等短路, 每 tick 调用不会产生额外开销。
     */
    private static void applyStepHeight(LocalPlayer player, double targetStepHeight)
    {
        AttributeInstance stepHeight = player.getAttribute(ForgeMod.STEP_HEIGHT_ADDITION.get());
        if (stepHeight == null) return;

        stepHeight.setBaseValue(targetStepHeight - VANILLA_STEP_HEIGHT);
    }
}
