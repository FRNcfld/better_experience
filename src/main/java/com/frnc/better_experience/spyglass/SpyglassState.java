package com.frnc.better_experience.spyglass;

/**
 * 望远镜功能里<strong>不依赖客户端专属类</strong>的那部分运行时状态与查询, 供 mixin 调用。
 *
 * <p>刻意放在通用包而不是 {@code spyglass/client}: 参照 {@code elytraflight/ElytraFlightHandler} 的做法,
 * 让混入原版类的代码有个干净的依赖面, 不引入 {@code net.minecraft.client} 之类的东西。
 */
public final class SpyglassState
{
    /**
     * 「使用望远镜」键是否按住。
     *
     * <p>按住期间会把玩家视为正在开镜 (见 {@code SpyglassScopingMixin}), 于是原版的缩放与覆盖层都会生效
     * ——这正是「望远镜放在饰品栏里也能用」的实现方式: 不需要真的拿着物品, 也不需要
     * 原模组那套「自动把望远镜换到副手/切快捷栏」的 hack。
     */
    private static boolean keyHeld;

    private SpyglassState()
    {
    }

    public static void setKeyHeld(boolean held)
    {
        keyHeld = held;
    }

    /** 是否正处于「按键强行开镜」状态 */
    public static boolean isForcedScoping()
    {
        return keyHeld;
    }
}
