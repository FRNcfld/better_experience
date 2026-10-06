package com.frnc.better_experience.mixin;

import com.frnc.better_experience.worldheight.WorldHeightOverrides;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.Lifecycle;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

/**
 * 世界高度覆盖的注入点 (详见 {@link WorldHeightOverrides})。
 *
 * <p>1.20.1 的维度类型是<strong>数据包注册表</strong>, 由
 * {@code RegistryDataLoader.loadRegistryContents} 一条条从 {@code data/<命名空间>/dimension_type/*.json}
 * 解出来注册进去。这个方法是所有数据包注册表共用的加载器, 每次加载一张注册表会被调用一次。
 *
 * <p>打两个点:
 * <ol>
 *   <li>{@code @Inject HEAD} —— 轮到维度类型这张注册表时, 就地用它的 {@link ResourceManager} 读本模组的数据包覆盖文件。
 *       加载器自带 {@code ResourceManager} 参数, 不用另找; 也只有在这里读才来得及 (见下)。</li>
 *   <li>{@code @ModifyArg} 注册调用 —— 把要注册进去的 {@code DimensionType} 换成覆盖后的。</li>
 * </ol>
 *
 * <p><strong>为什么不用 {@code AddReloadListenerEvent} 那套</strong>: 世界加载的顺序是
 * 「数据包注册表 (含维度类型) → 再解析 LevelStem → 再跑重载监听器」, 重载监听器跑起来时
 * {@code LevelStem} 已经把 {@code Holder<DimensionType>} 固化在手上了, 那时再改注册表也影响不到维度。
 * 必须赶在「注册之前」, 所以由 mixin 在这个加载器里读。
 *
 * <p>目标是原版类, 因此<strong>不可加 {@code remap = false}</strong>: 方法名 {@code loadRegistryContents}
 * 与 {@code WritableRegistry#register} 都要靠 better_experience.refmap.json 重映射到 SRG。
 *
 * <p>该 mixin 放在配置的 {@code mixins} (双端) 列表里。双端都要过这里: 专用服务端加载的是世界的注册表 (权威,
 * 客户端会通过登录包收到同一份), 而客户端加载自己的数据包时也走同一条路, 单人游戏下两边本来就是同一份。
 */
@Mixin(RegistryDataLoader.class)
public abstract class WorldHeightMixin
{
    /**
     * 轮到维度类型注册表时, 先读本模组的数据包覆盖文件。
     *
     * <p>参数表就是 {@code loadRegistryContents} 的参数表 —— 这里只需要用到 {@code manager} 和 {@code registryKey},
     * 其余的照抄是为了让 Mixin 能把处理方法和目标方法对上号 (少写一个都会注入失败)。
     */
    @Inject(method = "loadRegistryContents", at = @At("HEAD"))
    private static void betterExperience$loadWorldHeightOverrides(
            RegistryOps.RegistryInfoLookup lookup,
            ResourceManager manager,
            ResourceKey<?> registryKey,
            WritableRegistry<?> registry,
            Decoder<?> decoder,
            Map<ResourceKey<?>, Exception> exceptions,
            CallbackInfo ci)
    {
        if (Registries.DIMENSION_TYPE.equals(registryKey))
        {
            WorldHeightOverrides.loadFrom(manager);
        }
    }

    /**
     * 注册每一条时套一层覆盖。
     *
     * <p><strong>为什么用 {@code @ModifyArg} 而不是 {@code @Redirect}</strong>: {@code @Redirect} 是
     * <em>排他</em>的 —— 同一处调用被两个模组重定向时, Mixin 只保留 priority 大的那个, 另一个被整个跳过,
     * 日志里只留一行 {@code conflict. Skipping ... already redirected by ...}。<strong>静默失效</strong>比报错更难查。
     * 而 {@code RegistryDataLoader#loadRegistryContents} 是<em>所有</em>模组的数据包注册表 (维度类型、生物群系、
     * 结构……) 都要经过的路径, 撞车的概率不低; {@code @ModifyArg} 可以叠加, 多个模组各改一次依次传递。
     *
     * <p>处理器写成"消费被调用方法<em>自身</em>的全部实参"的形式, 因为查覆盖表要用到 {@code key}。
     * {@code @ModifyArg} 拿不到<em>外层</em>方法 ({@code loadRegistryContents}) 的参数, 但这里也不需要 ——
     * 数据包已经由上面那个 {@code @Inject HEAD} 读完了。
     *
     * <p>被注册的对象绝大多数都不是维度类型 (生物群系、结构、伤害类型……), {@link WorldHeightOverrides#apply}
     * 的第一句就会原样返回, 所以这次包装的开销可以忽略。
     */
    @ModifyArg(method = "loadRegistryContents", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/core/WritableRegistry;register(Lnet/minecraft/resources/ResourceKey;Ljava/lang/Object;Lcom/mojang/serialization/Lifecycle;)Lnet/minecraft/core/Holder$Reference;"),
            index = 1)
    private static <E> E betterExperience$applyWorldHeight(ResourceKey<E> key, E value, Lifecycle lifecycle)
    {
        return WorldHeightOverrides.apply(key, value);
    }
}
