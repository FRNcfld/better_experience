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
 * <p>为什么单独一份、而不是塞进 {@link Config} 的 COMMON: 这里放的是<strong>客户端偏好</strong>
 * (缩放倍数、准星、覆盖层样式)。COMMON 配置在专用服务器上也存在, 把纯客户端偏好写进去语义不对;
 * 而 CLIENT 类型只在客户端加载、不参与同步, 正是这类设置该待的地方。
 *
 * <p>{@code spyglassZoom} 是唯一一个<strong>由游戏写回</strong>的项: 滚轮调完缩放后会把当前倍数存回来,
 * 下次启动直接恢复 (原模组的缩放值是静态字段, 不落盘)。
 */
@Mod.EventBusSubscriber(modid = BetterExperience.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientConfig
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue SPYGLASS_ENABLED = BUILDER
            .comment("Master switch for the spyglass improvements feature")
            .define("spyglassEnabled", true);

    private static final ForgeConfigSpec.DoubleValue SPYGLASS_MAX_ZOOM = BUILDER
            .comment("Maximum magnification while scoping. Vanilla is 10.0")
            .defineInRange("spyglassMaxZoom", 10.0D, 1.0D, 10.0D);

    private static final ForgeConfigSpec.DoubleValue SPYGLASS_ZOOM_STEP = BUILDER
            .comment("How much one scroll notch changes the zoom, as a proportion of the current zoom (0.1 = 10% per notch)")
            .defineInRange("spyglassZoomStep", 0.1D, 0.01D, 1.0D);

    private static final ForgeConfigSpec.BooleanValue SPYGLASS_SHOW_CROSSHAIR = BUILDER
            .comment("Whether to keep showing the crosshair while scoping. Vanilla shows it, so true keeps vanilla behaviour")
            .define("spyglassShowCrosshair", true);

    private static final ForgeConfigSpec.BooleanValue SPYGLASS_SHOW_ZOOM_TEXT = BUILDER
            .comment("Whether to show the current magnification in the spyglass view")
            .define("spyglassShowZoomText", true);

    private static final ForgeConfigSpec.BooleanValue SPYGLASS_SMOOTH_CAMERA = BUILDER
            .comment("Slow the camera down proportionally to the magnification, for finer aiming while zoomed in")
            .define("spyglassSmoothCamera", false);

    private static final ForgeConfigSpec.EnumValue<SpyglassOverlayStyle> SPYGLASS_OVERLAY = BUILDER
            .comment("Spyglass overlay style. DEFAULT reuses the vanilla scope texture, NONE draws no overlay at all")
            .defineEnum("spyglassOverlay", SpyglassOverlayStyle.DEFAULT);

    private static final ForgeConfigSpec.DoubleValue SPYGLASS_ZOOM = BUILDER
            .comment("Current magnification, written back by the game so it is remembered across restarts")
            .defineInRange("spyglassZoom", 10.0D, 1.0D, 10.0D);

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
