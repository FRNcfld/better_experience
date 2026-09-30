package com.frnc.better_experience.saturation.client;

import com.frnc.better_experience.Config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.level.GameType;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.text.DecimalFormat;

/**
 * HUD 上的饱和度读数 (来源: Saturation Plus 的 {@code SaturationOverlay})。
 *
 * <p>画的是<strong>两行数字</strong>而不是进度条, 与原模组一致: 饥饿条右上方显示饱和度数值 (黄色),
 * 其下一行显示消耗度占上限的百分比 (暗黄)。位置沿用原模组的偏移量 (x 中心 +92, y 底边上 38 / 28 像素)。
 *
 * <p>与原模组两处不同:
 * <ul>
 *   <li>游戏模式判断改用 {@code Minecraft.gameMode.getPlayerMode()}, 因此<strong>不需要 Access Transformer</strong>
 *       (原模组为了让 {@code AbstractClientPlayer} 的 {@code getPlayerInfo()} 可访问专门加了 AT);</li>
 *   <li>数据来源是服务端同步来的 {@link ClientSaturation}, 而不是客户端本地那份可能过期的 {@code FoodData}。</li>
 * </ul>
 *
 * <p>显示的开关放在这个类里而不是注册时: 这样改配置无需重启就能生效。
 */
public final class SaturationOverlay implements IGuiOverlay
{
    /** 与原模组一致的格式 */
    private static final DecimalFormat VALUE_FORMAT = new DecimalFormat("0.00");
    private static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("0%");

    private static final int COLOR_SATURATION = 0xFFFFFF00;   // 黄
    private static final int COLOR_EXHAUSTION = 0xFF808000;   // 暗黄

    /** 相对屏幕中心 / 底边的像素偏移, 取自原模组的 (x+92, y-38) 与 (x+92, y-28) */
    private static final int X_OFFSET = 92;
    private static final int SATURATION_Y_OFFSET = 38;
    private static final int EXHAUSTION_Y_OFFSET = 28;

    public static final SaturationOverlay INSTANCE = new SaturationOverlay();

    private SaturationOverlay()
    {
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height)
    {
        // 客户端配置只决定"要不要显示"; 服务端那份决定实际机制是否生效
        if (!Config.saturationEnabled || !Config.saturationShowHud) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gameMode == null) return;

        GameType mode = mc.gameMode.getPlayerMode();
        if (mode != GameType.SURVIVAL && mode != GameType.ADVENTURE) return;

        float maxExhaustion = ClientSaturation.getMaxExhaustion();
        // 还没收到过同步 (初值 0), 或上限被配置成"禁用消耗"(服务端会发 Float.MAX_VALUE)
        if (maxExhaustion <= 0.0F) return;

        float exhaustion = ClientSaturation.getExhaustion();
        Font font = mc.font;
        int x = width / 2 + X_OFFSET;

        graphics.drawString(font, VALUE_FORMAT.format(ClientSaturation.getSaturation()),
                x, height - SATURATION_Y_OFFSET, COLOR_SATURATION);
        graphics.drawString(font, PERCENT_FORMAT.format(exhaustion / maxExhaustion),
                x, height - EXHAUSTION_Y_OFFSET, COLOR_EXHAUSTION);
    }
}
