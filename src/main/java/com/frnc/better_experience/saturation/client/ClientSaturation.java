package com.frnc.better_experience.saturation.client;

/**
 * 客户端持有的、由服务端同步来的饱和度数值缓存, 供 HUD 读取。
 *
 * <p>为什么不用客户端本地的 {@code FoodData}: 原版几乎不同步饱和度 (只在「是否恰为 0」翻转时同步),
 * 且服务端才是权威。所以客户端本地那份经常是过期的, 不能拿来显示。
 *
 * <p>初值全为 0, 表示「还没收到过同步」——此时 HUD 不会显示。
 */
public final class ClientSaturation
{
    private static float saturation;
    private static float exhaustion;
    private static float maxExhaustion;

    private ClientSaturation()
    {
    }

    /** 写入服务端同步来的数值 (只在客户端由 {@code SaturationSyncPacket} 调用) */
    public static void set(float saturation, float exhaustion, float maxExhaustion)
    {
        ClientSaturation.saturation = saturation;
        ClientSaturation.exhaustion = exhaustion;
        ClientSaturation.maxExhaustion = maxExhaustion;
    }

    public static float getSaturation()
    {
        return saturation;
    }

    public static float getExhaustion()
    {
        return exhaustion;
    }

    public static float getMaxExhaustion()
    {
        return maxExhaustion;
    }
}
