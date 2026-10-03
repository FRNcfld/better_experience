package com.frnc.better_experience.spyglass.client;

import com.frnc.better_experience.BetterExperienceClientConfig;

import net.minecraft.util.Mth;

/**
 * 望远镜的缩放值 (放大倍数) 与滚轮步进。
 *
 * <p><strong>语义</strong>: 内部存的是<strong>放大倍数</strong> ({@code 1.0} = 不放大, {@code 10.0} = 十倍),
 * 而原版 FOV 修正值是它的倒数 ({@code 1 / zoom})。原版开镜固定 {@code 0.1F}, 也就是 10 倍 —— 与本模组
 * 默认的最大值一致。
 *
 * <p><strong>滚轮是乘性步进</strong> (与原模组一致): 每格按当前倍数的固定比例变化, 所以放大得越大、
 * 每格的绝对变化越小, 手感更线性。同时尊重原版的「滚轮灵敏度」与「离散滚轮」两个选项。
 *
 * <p><strong>持久化</strong>: 变化后由 {@code KeyInputHandler} 在松开按键时写回 CLIENT 配置,
 * 因此重启后仍是上次调好的倍数 (原模组的缩放值只是静态字段, 不落盘)。
 */
public final class SpyglassZoom
{
    /** 当前放大倍数 */
    private static double zoom = BetterExperienceClientConfig.spyglassZoom;

    /** 是否有未写回的改动 */
    private static boolean dirty;

    private SpyglassZoom()
    {
    }

    /** 当前放大倍数 */
    public static double getZoom()
    {
        return zoom;
    }

    /** 当前应该使用的 FOV 修正值 (原版开镜固定为 0.1F) */
    public static float fovModifier()
    {
        return (float) (1.0D / zoom);
    }

    /**
     * 滚轮调整缩放。
     *
     * @param scrollDelta 原版 {@code MouseHandler.onScroll} 拿到的滚轮增量
     * @param discrete    是否启用了原版的「离散滚轮」选项
     * @param sensitivity 原版的滚轮灵敏度
     */
    public static void applyScroll(double scrollDelta, boolean discrete, double sensitivity)
    {
        if (BetterExperienceClientConfig.spyglassMaxZoom <= 1.0D) return;   // 不允许缩放时滚轮无意义

        double notches = (discrete ? Math.signum(scrollDelta) : scrollDelta) * sensitivity;
        if (notches == 0.0D) return;

        double step = BetterExperienceClientConfig.spyglassZoomStep;
        zoom = clamp(zoom * Math.pow(1.0D + step, notches));
        dirty = true;
    }

    /** 把当前倍数钳制进配置允许的区间 */
    private static double clamp(double value)
    {
        return Mth.clamp(value, 1.0D, BetterExperienceClientConfig.spyglassMaxZoom);
    }

    /** 配置改动后重新钳制一次 (例如把最大倍数调小了) */
    public static void reclamp()
    {
        double clamped = clamp(zoom);
        if (clamped != zoom)
        {
            zoom = clamped;
            dirty = true;
        }
    }

    /** 若有改动则写回配置文件, 返回是否真的写了 */
    public static boolean persistIfDirty()
    {
        if (!dirty) return false;

        dirty = false;
        BetterExperienceClientConfig.persistZoom(zoom);
        return true;
    }
}
