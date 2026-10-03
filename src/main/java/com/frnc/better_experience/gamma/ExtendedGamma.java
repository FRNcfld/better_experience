package com.frnc.better_experience.gamma;

import com.frnc.better_experience.BetterExperienceServerConfig;

/**
 * 亮度 (伽马) 扩展的<strong>运行时开关</strong>。
 *
 * <p>为什么需要一个运行时开关, 而不是直接读配置: 原版 {@code Options.gamma} 只在
 * {@code Options} 构造时创建一次, 游戏内改配置不会重建它。所以
 * {@link com.frnc.better_experience.mixin.GammaOptionsMixin} 会<strong>无条件</strong>装入带开关的
 * {@link ExtendedGammaValueSet}, 让取值范围可以随时切换; 配置项 {@code extendedGammaEnabled}
 * 只决定这个开关的<strong>初值</strong>。
 *
 * <p><strong>开关只决定"这个值生不生效", 不碰存储的亮度值本身</strong>:
 * <ul>
 *   <li>滑块任何时候都能拖到 1000%（取值集合恒为 0–10, 见 {@link ExtendedGammaValueSet}）, 与开关无关;</li>
 *   <li>拖出来的值原样存进 options.txt, 开关来回切不会改动它, 所以重启后依然是玩家设的那个值;</li>
 *   <li>真正生效的亮度由 {@link #effectiveGamma(double)} 决定: 开关打开时用设置值, 关闭时按原版
 *       （即压到原版上限 100%）表现。</li>
 * </ul>
 *
 * <p>生效亮度的拦截点是 {@code LightTexture#updateLightTexture}——那是原版<strong>唯一</strong>读取
 * {@code options.gamma()} 的地方 (已对整个 Minecraft 源码核实过), 见
 * {@link com.frnc.better_experience.mixin.GammaLightTextureMixin}。在那里把值压回去, 而不是改存储值,
 * 正是"能拖但不生效"能成立的原因。
 *
 * <p>与其他功能的热键开关一致: 状态是会话级的, 热键切换只改内存态, 不写回配置文件。
 */
public final class ExtendedGamma
{
    /** 原版亮度上限 (100%) */
    public static final double VANILLA_MAX_GAMMA = 1.0D;

    private static boolean enabled = BetterExperienceServerConfig.extendedGammaEnabled;

    private ExtendedGamma()
    {
    }

    /** 游戏内切换, 返回切换后的新状态 */
    public static boolean toggle()
    {
        enabled = !enabled;
        return enabled;
    }

    /**
     * 实际用于渲染的亮度值。存储值不受影响, 所以开关来回切不会丢玩家设置。
     *
     * @param stored 滑块上设置的值 (0.0–10.0)
     * @return 开关打开时原样返回; 关闭时压到原版上限 (即原版滑块拉满的表现)
     */
    public static double effectiveGamma(double stored)
    {
        return enabled ? stored : Math.min(stored, VANILLA_MAX_GAMMA);
    }
}
