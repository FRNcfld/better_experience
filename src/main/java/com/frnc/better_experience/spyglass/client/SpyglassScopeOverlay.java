package com.frnc.better_experience.spyglass.client;

import com.frnc.better_experience.BetterExperienceClientConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.text.DecimalFormat;

/**
 * 开镜时在屏幕中央下方显示当前放大倍数。
 *
 * <p><strong>这是新增功能, 不是移植</strong>: 原模组的 class 里没有任何文字渲染, 它不显示缩放倍数。
 *
 * <p>用独立的 overlay 而不是塞进 {@code Gui.renderSpyglassOverlay} 的注入里, 这样
 * 「覆盖层样式设为 NONE」时倍数读数依然能显示 (若用 TAIL 注入, HEAD 处 cancel 之后 TAIL 就不会执行了)。
 */
public final class SpyglassScopeOverlay implements IGuiOverlay
{
    public static final SpyglassScopeOverlay INSTANCE = new SpyglassScopeOverlay();

    private static final DecimalFormat ZOOM_FORMAT = new DecimalFormat("0.0");
    private static final int COLOR = 0xFFFFFFFF;
    /** 放在准星下方一点, 避免遮住准星 */
    private static final int Y_OFFSET = 24;

    private SpyglassScopeOverlay()
    {
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height)
    {
        if (!BetterExperienceClientConfig.spyglassEnabled || !BetterExperienceClientConfig.spyglassShowZoomText) return;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !player.isScoping()) return;

        Component text = Component.translatable("message.better_experience.spyglass.zoom",
                ZOOM_FORMAT.format(SpyglassZoom.getZoom()));

        Font font = Minecraft.getInstance().font;
        graphics.drawString(font, text, (width - font.width(text)) / 2, height / 2 + Y_OFFSET, COLOR);
    }
}
