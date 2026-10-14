package com.frnc.better_experience.gamma.client;

import com.frnc.better_experience.BetterExperience;
import com.frnc.better_experience.gamma.ExtendedGamma;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 亮度 (伽马) 扩展的客户端输入:
 * <ul>
 *   <li>按一下「切换」热键 —— 开 / 关扩展是否生效, 动作栏回显;</li>
 *   <li><strong>按住</strong>「调整」热键再滚轮 —— 改亮度值 (0–1000%), 动作栏回显。</li>
 * </ul>
 * 两个热键都默认不绑定, 需在「控制」中自行设置。
 */
@Mod.EventBusSubscriber(modid = BetterExperience.MOD_ID, value = Dist.CLIENT)
public class KeyInputHandler
{
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        if (!ClientHandler.TOGGLE_KEY.consumeClick()) return;

        boolean enabled = ExtendedGamma.toggle();

        // 从开到关时回显"现在实际生效的亮度": 只报一句"已关闭"的话, 玩家看着仍然很亮的画面
        // 会以为关失败了 —— 实际生效的是回退后的基准值, 而这个值通常既不是存储值也不是 0
        mc.player.displayClientMessage(
                Component.translatable(enabled
                                ? "message.better_experience.extended_gamma.enabled"
                                : "message.better_experience.extended_gamma.disabled",
                        Math.round(ExtendedGamma.baselineGamma() * 100.0D))
                        .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED),
                true);
    }

    /**
     * 按住「调整亮度」热键时, 滚轮改亮度值 (0–1000%) 并在动作栏回显。
     *
     * <p>用 Forge 的 {@code MouseScrollingEvent}, 而不再加一个 {@code MouseHandler} mixin。
     * 已核过 Forge 的补丁, 正好是这里需要的两点:
     * <ul>
     *   <li>它只在<strong>没有界面打开且在游戏内</strong>时触发 (界面那条路走的是另一个事件), 所以不必自己判断界面;</li>
     *   <li>取消它会让原版处理直接 {@code return}, 即<strong>不会顺手切快捷栏</strong>。</li>
     * </ul>
     * 开镜时望远镜的 mixin 在方法最前面就 cancel 了, 所以那条路径不受影响 (开镜时滚轮仍是缩放)。
     *
     * <p>改的是 {@code Options.gamma} 里那个<strong>存储值</strong> (与滑块同一个来源, 不另立一份),
     * 因此照常写进 options.txt、重启后仍在。这里是"改值", 不是"改开关"。
     *
     * <p><strong>开关关闭时本入口整体不放行</strong>: 那时改的值不参与渲染, 玩家对着一个没有任何画面反馈的
     * 滚轮调半天只会更困惑, 还会以为自己没关成功。要调值请先用切换热键把扩展打开。
     */
    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (!ClientHandler.ADJUST_KEY.isDown()) return;

        // 关闭时直接返回。刻意不 cancel —— 让滚轮照常切快捷栏,
        // 否则关掉扩展后按住调整键滚轮会变成什么都不发生
        if (!ExtendedGamma.isEnabled()) return;

        double scrollDelta = event.getScrollDelta();
        if (scrollDelta == 0.0D) return;

        // 与望远镜的滚轮一致: 尊重原版「离散滚轮」与「滚轮灵敏度」两个选项
        double notches = (mc.options.discreteMouseScroll().get() ? Math.signum(scrollDelta) : scrollDelta)
                * mc.options.mouseWheelSensitivity().get();

        OptionInstance<Double> gamma = mc.options.gamma();
        double updated = ExtendedGamma.step(gamma.get(), notches);
        gamma.set(updated);

        // 走到这里开关一定是开着的 (关闭时上面已经返回), 所以不必再分支
        mc.player.displayClientMessage(
                Component.translatable("message.better_experience.extended_gamma.value",
                        Math.round(updated * 100.0D)).withStyle(ChatFormatting.GREEN),
                true);

        event.setCanceled(true);
    }
}
