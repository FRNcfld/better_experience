package com.frnc.better_experience.mixin;

import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 暴露 {@link ThrownTrident} 里那个存着"原始三叉戟物品"的私有字段 {@code tridentItem}。
 *
 * <p><strong>为什么需要它:</strong> 引雷有两条落雷路径, 其中"打避雷针"那一条在<em>方块侧</em>
 * ({@code LightningRodBlock#onProjectileHit}, 见 {@link LightningRodBlockMixin})。那里只拿得到一个
 * {@code Projectile}, 而 {@code ThrownTrident} 取物品的唯一方法 {@code getPickupItem()} 是
 * {@code protected} —— 方块侧访问不到。于是用 {@code @Accessor} 把字段读出来,
 * 让两条路径共用同一个"这件物品带不带海洋之祝"的判定入口。
 *
 * <p>只读, 不提供 setter。
 */
@Mixin(ThrownTrident.class)
public interface ThrownTridentAccessor
{
    @Accessor("tridentItem")
    ItemStack betterExperience$tridentItem();
}
