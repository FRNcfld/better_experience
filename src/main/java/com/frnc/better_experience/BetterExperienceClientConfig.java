package com.frnc.better_experience;

import com.frnc.better_experience.spyglass.SpyglassOverlayStyle;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/**
 * 客户端配置, 生成在 {@code config/better_experience-client.toml}。
 *
 * <p>为什么单独一份、而不是塞进 {@link BetterExperienceServerConfig} 的 COMMON: 这里放的是<strong>客户端偏好</strong>
 * (缩放倍数、准星、覆盖层样式)。COMMON 配置在专用服务器上也存在, 把纯客户端偏好写进去语义不对;
 * 而 CLIENT 类型只在客户端加载、不参与同步, 正是这类设置该待的地方。
 *
 * <p>本类只有望远镜一组选项, 所以 TOML 里只有 {@code [spyglass]} 一个小节。
 *
 * <p><strong>分节约定</strong>: 小节用夹在字段之间的 {@code static} 块里的 {@code push()} / {@code pop()} 划分。
 * 静态初始化按书写顺序执行, 因此 push 之后定义的项就归入该小节, pop 之后回到上一层;
 * push 之前挂着的 {@code comment()} 会成为 TOML 里该小节的标题注释。
 * 新增配置项时按所属小节就近插入即可; 新增小节则在它开头补 pop、结尾补 push。
 *
 * <p>配置项定义 / 运行时字段 / {@link #onLoad} 的读取这三处的顺序完全一致, 便于对照查找。
 */
@Mod.EventBusSubscriber(modid = BetterExperience.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class BetterExperienceClientConfig
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // ==================== 望远镜 ====================
    static
    {
        BUILDER.comment("望远镜改进: 不手持也能开镜、滚轮缩放并记住设置").push("spyglass");
    }

    private static final ForgeConfigSpec.BooleanValue SPYGLASS_ENABLED = BUILDER
            .comment("望远镜改进功能的总开关",
                     "关闭后滚轮缩放、准星、覆盖层等全部回到原版行为, 且玩家无法在游戏内重新打开")
            .define("spyglassEnabled", true);

    private static final ForgeConfigSpec.DoubleValue SPYGLASS_MAX_ZOOM = BUILDER
            .comment("开镜时能达到的最大放大倍数。原版为 10.0")
            .defineInRange("spyglassMaxZoom", 10.0D, 1.0D, 10.0D);

    private static final ForgeConfigSpec.DoubleValue SPYGLASS_ZOOM_STEP = BUILDER
            .comment("滚轮每一格改变多少缩放, 取当前倍数的比例 (0.1 = 每格 10%)")
            .defineInRange("spyglassZoomStep", 0.1D, 0.01D, 1.0D);

    private static final ForgeConfigSpec.BooleanValue SPYGLASS_SHOW_CROSSHAIR = BUILDER
            .comment("开镜时是否保留准星。原版会显示, 所以填 true 即原版表现")
            .define("spyglassShowCrosshair", true);

    private static final ForgeConfigSpec.BooleanValue SPYGLASS_SHOW_ZOOM_TEXT = BUILDER
            .comment("开镜时是否在准星下方显示当前放大倍数")
            .define("spyglassShowZoomText", true);

    private static final ForgeConfigSpec.BooleanValue SPYGLASS_SMOOTH_CAMERA = BUILDER
            .comment("是否按放大倍数同比放慢镜头移动, 便于放大后精细瞄准")
            .define("spyglassSmoothCamera", false);

    private static final ForgeConfigSpec.EnumValue<SpyglassOverlayStyle> SPYGLASS_OVERLAY = BUILDER
            .comment("开镜覆盖层样式。DEFAULT = 沿用原版的望远镜贴图, NONE = 完全不画覆盖层")
            .defineEnum("spyglassOverlay", SpyglassOverlayStyle.DEFAULT);

    /** 唯一一个由游戏写回的项, 见 {@link #persistZoom(double)}。 */
    private static final ForgeConfigSpec.DoubleValue SPYGLASS_ZOOM = BUILDER
            .comment("当前放大倍数, 由游戏在停止开镜时写回, 用来跨启动记住设置",
                     "手改这里只会在下次开镜时被覆盖")
            .defineInRange("spyglassZoom", 10.0D, 1.0D, 10.0D);

    // ==================== 收尾 ====================
    static
    {
        BUILDER.pop();
    }

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    // 初值写成配置文件的默认值: 这些字段会在配置加载完成前就被读取 (键位注册、mixin 查询)
    public static boolean spyglassEnabled = true;
    public static double spyglassMaxZoom = 10.0D;
    public static double spyglassZoomStep = 0.1D;
    public static boolean spyglassShowCrosshair = true;
    public static boolean spyglassShowZoomText = true;
    public static boolean spyglassSmoothCamera = false;
    public static SpyglassOverlayStyle spyglassOverlay = SpyglassOverlayStyle.DEFAULT;
    public static double spyglassZoom = 10.0D;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        // 关键: ModConfigEvent 对**所有**配置都会触发。不加这个判断, CLIENT 配置加载时
        // 会连带去读还没加载的 COMMON 配置 (反之亦然), 直接抛 "Cannot get config value before config is loaded"。
        if (event.getConfig().getType() != ModConfig.Type.CLIENT) return;

        spyglassEnabled = SPYGLASS_ENABLED.get();
        spyglassMaxZoom = SPYGLASS_MAX_ZOOM.get();
        spyglassZoomStep = SPYGLASS_ZOOM_STEP.get();
        spyglassShowCrosshair = SPYGLASS_SHOW_CROSSHAIR.get();
        spyglassShowZoomText = SPYGLASS_SHOW_ZOOM_TEXT.get();
        spyglassSmoothCamera = SPYGLASS_SMOOTH_CAMERA.get();
        spyglassOverlay = SPYGLASS_OVERLAY.get();
        spyglassZoom = SPYGLASS_ZOOM.get();
    }

    /** 把当前缩放倍数写回配置文件 (由 {@code SpyglassZoom} 在停止开镜时调用) */
    public static void persistZoom(double zoom)
    {
        SPYGLASS_ZOOM.set(zoom);
        SPEC.save();
    }
}
