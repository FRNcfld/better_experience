package com.frnc.better_experience.spyglass.integration;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * Curios 查询 —— <strong>本模组唯一引用 Curios 类的类</strong>。
 *
 * <p>为什么要单独拆出来: Curios 是可选前置, 未安装时它的类在运行期根本不存在。如果有任何一个会被无条件
 * 加载的类直接引用 Curios 类型, 加载它就是 {@code NoClassDefFoundError}。把引用集中到这里之后,
 * {@link SpyglassFinder} 只在确认 Curios 已加载时才会首次触及本类, JVM 也就只在那时解析这些符号。
 *
 * <p>连这个方法体也包了 try/catch: Curios 5.x 的 API 在小版本间有增补, 万一玩家装的是缺少
 * {@code findFirstCurio} 的旧版, 这里宁可当"没找到", 也不要让游戏崩。
 */
final class CuriosLookup
{
    private CuriosLookup()
    {
    }

    /** 玩家身上（含所有 curio 槽位）是否有一副望远镜 */
    static boolean hasSpyglass(LivingEntity entity)
    {
        try
        {
            return CuriosApi.getCuriosInventory(entity)
                    .resolve()
                    .map(handler -> handler.findFirstCurio(Items.SPYGLASS).isPresent())
                    .orElse(false);
        }
        catch (RuntimeException | LinkageError e)
        {
            // API 版本不匹配等情况: 当作没找到, 由调用方退化到背包查找
            return false;
        }
    }
}
