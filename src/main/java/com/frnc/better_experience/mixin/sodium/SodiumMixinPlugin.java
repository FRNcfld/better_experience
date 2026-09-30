package com.frnc.better_experience.mixin.sodium;

import com.mojang.logging.LogUtils;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * {@code better_experience.sodium.mixins.json} 的插件, 负责在装了 Sodium 系模组时挂上
 * {@link SodiumGameOptionPagesMixin} 并触发 {@link SodiumGameOptionPagesAsm} 的字节码改写。
 *
 * <p>结构与来源模组 (GJEB 的 {@code GJEBAbstractMixinPlugin} / {@code GJEBMixinPlugin}) 一致:
 * 用 {@link #getMixins()} 按需注册那个空 mixin, 再在 {@link #preApply} 里趁机改写目标类的
 * {@code ClassNode}。
 *
 * <p>与来源模组的两处差异:
 * <ul>
 *   <li>mod id 多查 <strong>{@code embeddium}</strong>——1.20.1 上更常用的是 Embeddium,
 *       而来源模组只查了 {@code sodium} 和 {@code rubidium}, 会漏掉它;</li>
 *   <li>目标类名按后缀 {@code .gui.SodiumGameOptionPages} 匹配, 不写死包名。</li>
 * </ul>
 *
 * <p>{@link SodiumGameOptionPagesMixin} 只声明了字符串 {@code targets}, 不引用任何 Sodium 类型,
 * 所以即便没装 Sodium 也能正常加载; 加上 {@link #getMixins()} 在未安装时返回 {@code null},
 * 未安装时根本不会注册它, 也就不会产生"目标类找不到"的警告。
 */
public class SodiumMixinPlugin implements IMixinConfigPlugin
{
    private static final Logger LOGGER = LogUtils.getLogger();

    /** Sodium 本体的 mod id, 以及 Forge 上的两个移植版 */
    private static final String[] SODIUM_MOD_IDS = { "sodium", "rubidium", "embeddium" };

    private static final String SODIUM_OPTIONS_CLASS_SUFFIX = ".gui.SodiumGameOptionPages";

    private static final String SODIUM_OPTIONS_MIXIN = "SodiumGameOptionPagesMixin";

    @Override
    public void onLoad(String mixinPackage)
    {
    }

    @Override
    public String getRefMapperConfig()
    {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName)
    {
        return SodiumMixinPlugin.isSodiumLoaded();
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets)
    {
    }

    @Override
    public List<String> getMixins()
    {
        return SodiumMixinPlugin.isSodiumLoaded() ? List.of(SODIUM_OPTIONS_MIXIN) : null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo)
    {
        if (targetClassName != null && targetClassName.endsWith(SODIUM_OPTIONS_CLASS_SUFFIX))
        {
            SodiumGameOptionPagesAsm.raiseBrightnessSliderMax(targetClass);
        }
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo)
    {
    }

    /** 只在客户端且装了 Sodium 系模组时才启用——Sodium 是纯客户端模组 */
    private static boolean isSodiumLoaded()
    {
        if (FMLEnvironment.dist != Dist.CLIENT) return false;

        LoadingModList mods = LoadingModList.get();
        if (mods == null) return false;

        for (String modId : SODIUM_MOD_IDS)
        {
            if (mods.getModFileById(modId) != null) return true;
        }

        LOGGER.debug("[better_experience] 未检测到 Sodium 系模组, 跳过亮度滑块兼容改写");
        return false;
    }
}
