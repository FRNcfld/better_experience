package com.frnc.better_experience.oceanblessing;

import com.frnc.better_experience.BetterExperience;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 海洋之祝附魔的注册表句柄 (id: {@code better_experience:ocean_blessing}), 以及
 * "这件物品带不带海洋之祝"的统一判定。
 *
 * <p>{@link #hasOceanBlessing} 会被 mixin 在玩家使用三叉戟、以及投掷出去的三叉戟命中时调用,
 * 必须是<strong>不会抛异常</strong>的: 注册表要到 mod 构造期才填充完成, 而 {@link RegistryObject#isPresent()}
 * 让更早的时点也能安全地返回 false, 而不是抛 {@code RegistryObject#get} 那种异常。
 */
public final class OceanBlessingEnchantments
{
    public static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, BetterExperience.MOD_ID);

    public static final RegistryObject<Enchantment> OCEAN_BLESSING =
            ENCHANTMENTS.register("ocean_blessing", OceanBlessingEnchantment::new);

    private OceanBlessingEnchantments()
    {
    }

    /** 由 {@code BetterExperience} 在 mod 构造期调用, 把附魔挂到 mod 事件总线上 */
    public static void register(IEventBus modEventBus)
    {
        ENCHANTMENTS.register(modEventBus);
    }

    /** 这件物品是否带海洋之祝 (只要带了, 等级必为 1); 空物品或尚未注册时返回 false */
    public static boolean hasOceanBlessing(ItemStack stack)
    {
        if (stack.isEmpty() || !OCEAN_BLESSING.isPresent()) return false;
        return EnchantmentHelper.getItemEnchantmentLevel(OCEAN_BLESSING.get(), stack) > 0;
    }
}
