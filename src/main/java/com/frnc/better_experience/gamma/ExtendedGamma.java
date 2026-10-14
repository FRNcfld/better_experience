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
 *   <li>真正生效的亮度由 {@link #effectiveGamma(double)} 决定: 开关打开时用设置值,
 *       关闭时回到 {@link #baselineGamma()} —— 玩家最后一次设定的原版范围内亮度,
 *       <strong>而不是</strong>把存储值压到 100%（那种做法在存储值已经是 1000% 时等于顶到最亮）;</li>
 *   <li>关闭期间「调整亮度」热键也不放行 (见 {@code gamma/client/KeyInputHandler}):
 *       "关了还能调"会让玩家以为自己没关成功。滑块本身不受影响 —— 那属于原版界面, 本方管不到,
 *       而且改的只是存储值, 关着的时候不参与渲染。</li>
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
 * 与拖动滑块完全等价 (同样不碰开关)。<strong>开关关闭时这条入口整体不放行</strong> (由调用方提前返回)。
 * 这条入口不依赖任何界面, 所以 Sodium / Rubidium / Embeddium
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

    /** 「关掉扩展后回退到哪个亮度」还没被校准过时的兜底值 —— 原版 {@code Options.gamma} 的构造初值 */
    private static final double FALLBACK_BASELINE = 0.5D;

    /**
     * 「关掉扩展后应该回到的亮度」, 即玩家最后一次设定的<strong>原版范围内</strong>的值。
     *
     * <p>为什么不是 {@code min(stored, 1.0)}: 存储值被热键调到 10.0 之后, {@code min(10.0, 1.0) = 1.0} ——
     * 而 <strong>1.0 恰好是原版最亮</strong>。于是"关掉扩展"这个动作反而把画面顶到最亮, 看起来就像开关没作用。
     * 玩家真正期望的是"回到我开扩展之前用的那个亮度", 所以这里记的是那个值。
     *
     * <p>由 {@link #syncBaseline(double)} 在每帧校准, 详见 {@link #effectiveGamma(double)}。
     */
    private static double vanillaBaseline = FALLBACK_BASELINE;

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
     * <p>基准<strong>每帧都跟着存储值走</strong> (只要它落在原版范围内), 关闭时给的是基准而不是存储值本身:
     * <ul>
     *   <li>玩家用原版滑块调到 0.8 → 基准跟着变成 0.8 → 之后关掉扩展回到的就是 0.8;</li>
     *   <li>存储值被热键调到 10.0 (超出原版范围) → 基准不动 → 关掉扩展回到的是那之前的值,
     *       而不是被顶到 1.0 最亮;</li>
     *   <li>扩展<strong>一直关着</strong>时也要能校准, 所以同步放在开关判断<em>之前</em> ——
     *       否则关闭状态下的早退会让基准永远停在 {@link #FALLBACK_BASELINE},
     *       把玩家自己拖出来的 0.8 也一并吃掉。</li>
     * </ul>
     *
     * @param stored 滑块上设置的值 (0.0–{@link ExtendedGammaValueSet#MAX_GAMMA})
     * @return 开关打开时原样返回; 关闭时返回 {@link #baselineGamma()}
     */
    public static double effectiveGamma(double stored)
    {
        syncBaseline(stored);
        return enabled ? stored : vanillaBaseline;
    }

    /** 关掉扩展后<strong>实际生效</strong>的亮度 (供切换热键回显, 让玩家知道画面接下来是什么亮度) */
    public static double baselineGamma()
    {
        return vanillaBaseline;
    }

    /**
     * 用存储值校准基准, 但<strong>只接受原版范围内的值</strong>。
     *
     * <p>超过 100% 的存储值代表的是"扩展开着时"的亮度, 拿它当"关掉后要回到的亮度"就违背了这个功能的语义 ——
     * 而且那个值在原版管线下本来就会被继续拉亮 (光照管线里的 lerp 权重并不封顶, 只有最终颜色被钳到 1.0),
     * 回退到它等于没回退。
     */
    private static void syncBaseline(double stored)
    {
        if (stored >= 0.0D && stored <= VANILLA_MAX_GAMMA)
        {
            vanillaBaseline = stored;
        }
    }
}
