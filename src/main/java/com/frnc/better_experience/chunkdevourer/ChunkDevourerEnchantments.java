package com.frnc.better_experience.chunkdevourer;

import com.frnc.better_experience.BetterExperience;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 区块吞噬者附魔的注册表句柄 (id: {@code better_experience:chunk_devourer}), 以及
 * "这件物品带几级区块吞噬者"的统一判定。
 *
 * <p>与 {@code OceanBlessingEnchantments} 一样自带一个 {@link DeferredRegister}: 同一个注册表可以有多个
 * DeferredRegister, 各自挂到 mod 总线上互不影响。这样每个功能包自给自足, 符合本模组"一功能一包"的分包约定。
 *
 * <p>{@link #levelOf} 会被事件处理器在玩家挖方块的瞬间调用, 必须是<strong>不会抛异常</strong>的:
 * 注册表要到 mod 构造期才填充完成, 而 {@link RegistryObject#isPresent()} 让更早的时点也能安全地返回 0,
 * 而不是抛 {@code RegistryObject#get} 那种异常。
 */
public final class ChunkDevourerEnchantments
{
    public static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, BetterExperience.MOD_ID);

    public static final RegistryObject<Enchantment> CHUNK_DEVOURER =
            ENCHANTMENTS.register("chunk_devourer", ChunkDevourerEnchantment::new);

    private ChunkDevourerEnchantments()
    {
    }

    /** 由 {@code BetterExperience} 在 mod 构造期调用, 把附魔挂到 mod 事件总线上 */
    public static void register(IEventBus modEventBus)
    {
        ENCHANTMENTS.register(modEventBus);
    }

    /** 这件物品带的区块吞噬者等级 (1 ~ 3); 没带、空物品或尚未注册时返回 0 */
    public static int levelOf(ItemStack stack)
    {
        if (stack.isEmpty() || !CHUNK_DEVOURER.isPresent()) return 0;
        return EnchantmentHelper.getItemEnchantmentLevel(CHUNK_DEVOURER.get(), stack);
    }
}
