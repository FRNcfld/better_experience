package com.frnc.better_experience.gamma.client;

import com.frnc.better_experience.BetterExperience;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 客户端按键注册: 亮度 (伽马) 扩展的「开关」与「调值」两个热键。
 *
 * <p>两个都<strong>默认不绑定</strong>, 需在「控制」中自行设置 (与本模组其它热键一致)。
 */
@Mod.EventBusSubscriber(modid = BetterExperience.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientHandler
{
    public static final String KEY_CATEGORY = "key.categories.better_experience";
    public static final String KEY_TOGGLE_EXTENDED_GAMMA = "key.better_experience.extended_gamma_toggle";
    public static final String KEY_ADJUST_EXTENDED_GAMMA = "key.better_experience.extended_gamma_adjust";

    /** 切换亮度扩展是否生效 */
    public static final KeyMapping TOGGLE_KEY = new KeyMapping(
            KEY_TOGGLE_EXTENDED_GAMMA,
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            KEY_CATEGORY
    );

    /**
     * 按住它再滚轮即可改亮度值 (0–1000%)。
     *
     * <p>读的是<strong>按住状态</strong> ({@code isDown()}), 不是单击, 所以不走
     * {@code consumeClick()} 那条路 —— 见 {@link KeyInputHandler#onMouseScroll}。
     */
    public static final KeyMapping ADJUST_KEY = new KeyMapping(
            KEY_ADJUST_EXTENDED_GAMMA,
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            KEY_CATEGORY
    );

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event)
    {
        event.register(TOGGLE_KEY);
        event.register(ADJUST_KEY);
    }
}
