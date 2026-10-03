package com.frnc.better_experience.gamma;

import com.frnc.better_experience.BetterExperienceServerConfig;

import net.minecraft.util.Mth;

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
 * <p>生效亮度的拦截点是 {@code LightTexture#updateLightTexture}——那是原版<strong>唯一把
 * {@code options.gamma()} 的值用于渲染的地方</strong> (已对整个 Minecraft 源码核实: 另一处
 * {@code VideoSettingsScreen} 只是把该选项交给界面, 并不取值), 见
 * {@link com.frnc.better_experience.mixin.GammaLightTextureMixin}。在那里把值压回去, 而不是改存储值,
 * 正是"能调但不生效"能成立的原因。
 *
 * <p>与其他功能的热键开关一致: 状态是会话级的, 热键切换只改内存态, 不写回配置文件。
 *
 * <p><strong>「按住热键 + 滚轮」调值</strong>走 {@link #step(double, double)}: 它只算新值、只改存储值,
 * 与拖动滑块完全等价 (同样不碰开关)。这条入口不依赖任何界面, 所以 Sodium / Rubidium / Embeddium
 * 的视频设置界面也能用 —— 那些界面里的滑块上限仍是 0–100%, 热键是那里唯一能调到 1000% 的途径。
 *
 * <p><strong>不要再为了"让那些界面的滑块也能拖到 1000%"去改写 Sodium 系的界面类。</strong>
 * 曾有一版正是这么做的 (ASM 改写 {@code SodiumGameOptionPages} 里亮度滑块的上限常量), 实测在
 * <strong>Embeddium 0.3.31</strong> 下会让视频设置界面<strong>完全打不开</strong>: 点击有按压动画、
 * 随后无任何反应, 且无异常、无日志 (去掉那次改写即恢复正常)。上游 GJEB 也从不改 Embeddium 的界面类
 * (它只在 sodium / rubidium 下改, 而它支持的 Embeddium 1.x 用的是 Embeddium 自己的页面类)。
 * 该兼容层现已整层移除, 调值一律走热键。
 */
public final class ExtendedGamma
{
    /** 原版亮度上限 (100%) */
    public static final double VANILLA_MAX_GAMMA = 1.0D;

    /**
     * 「按住热键 + 滚轮」每格改变多少伽马值 —— {@code 0.1} 即 <strong>10 个百分点</strong> (1.0 = 100%)。
     *
     * <p>取值是按"两头都不难受"定的: 原版常用的 0–100% 段有 10 格, 全程 0–1000% 共 100 格。
     * 想要更细就调小 (0.05 会让全程变 200 格), 想更快就调大。
     */
    public static final double SCROLL_STEP = 0.1D;

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

    /** 当前开关状态 (供滚轮调值时的回显判断用) */
    public static boolean isEnabled()
    {
        return enabled;
    }

    /**
     * 滚轮步进一次, 把结果钳制在 0.0–{@link ExtendedGammaValueSet#MAX_GAMMA}。
     *
     * <p>只算新值, <strong>不碰开关、也不写任何地方</strong>: 与拖动滑块完全等价, 调用方负责写进
     * {@code Options.gamma} 与回显。
     *
     * @param current 当前存储值 (0.0–10.0)
     * @param notches 滚轮格数 (正为向上/变大, 负为向下/变小; 已按原版灵敏度归一化)
     */
    public static double step(double current, double notches)
    {
        if (notches == 0.0D) return current;

        return Mth.clamp(current + notches * SCROLL_STEP, 0.0D, ExtendedGammaValueSet.MAX_GAMMA);
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
