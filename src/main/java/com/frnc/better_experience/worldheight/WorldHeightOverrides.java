package com.frnc.better_experience.worldheight;

import com.frnc.better_experience.BetterExperience;
import com.frnc.better_experience.BetterExperienceServerConfig;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.dimension.DimensionType;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

/**
 * 世界高度覆盖: 按<strong>维度类型</strong>改写世界的可用高度范围 (下限与上限)。
 *
 * <p><strong>为什么改一处就够</strong>: 1.20.1 里整个世界的高度只有 {@link DimensionType} 一个来源 ——
 * {@code LevelReader.getMinBuildHeight() / getHeight()} 就是 {@code dimensionType().minY() / height()},
 * 而区块的段数组 ( {@code ChunkAccess} 按 {@code getSectionsCount()} 分配)、高度图 (按高度算位数)、
 * 光照 (逐段存储)、客户端区块包的解码, 全都由它派生。所以把注册表里的 {@code DimensionType} 换掉,
 * 服务端与客户端就整体跟着变。
 *
 * <p><strong>为什么在注册表加载时替换</strong>: 维度类型是<strong>数据包注册表</strong>, 由
 * {@code RegistryDataLoader.loadRegistryContents} 逐条注册。同一批加载里, 维度类型先注册,
 * {@code LevelStem} (维度本身) 之后才解析出 {@code Holder<DimensionType>} 并把它固化下来。
 * 因此必须在「注册之前」把值换掉 —— 这时候换, {@code LevelStem} 拿到的就是覆盖后的持有者,
 * 服务端发给客户端的登录包里也是覆盖后的值 ({@code RegistrySynchronization} 同步了 dimension_type),
 * 双端天然一致, 连没装本模组的客户端也能正确显示 (高度本来就是数据驱动的)。
 *
 * <p>反过来说, <strong>不能</strong>用 {@link com.frnc.better_experience.BetterExperienceDataPackReloadListener}
 * 那套 {@code AddReloadListenerEvent} 来读: 重载监听器跑在数据包注册表<em>加载完之后</em>
 * (世界加载流程是 注册表加载 → ReloadableServerResources → 重载监听器), 那时 {@code LevelStem} 早已固化,
 * 再怎么改都晚了。所以这里由 mixin 在注册表加载途中直接读数据包。
 *
 * <p><strong>数据包位置</strong>: {@code data/<任意命名空间>/world_height/*.json}, 扫描所有命名空间并合并,
 * 于是玩家只要往存档的 {@code datapacks/} 里丢一个自己的数据包就能加维度, 不用改模组 jar。
 * 同一个维度被多个文件写到时, 模组自带的模板排最前、其余按资源 ID 排序, 靠后的覆盖靠前的, 并打一条冲突日志。
 * 文件格式 (顶层是数组):
 *
 * <pre>
 * [
 *   { "dimension": "minecraft:overworld", "min_y": -64, "height": 512 },
 *   { "dimension": "minecraft:the_nether", "height": 256 }
 * ]
 * </pre>
 *
 * <ul>
 *   <li>{@code dimension} —— 维度<strong>类型</strong>的 ID (原版三个是 {@code minecraft:overworld} /
 *       {@code minecraft:the_nether} / {@code minecraft:the_end}; 模组维度看它的 {@code dimension_type} 文件名)。
 *       不是存档里到处的那个维度 ID —— 不过原版两者同名。</li>
 *   <li>{@code min_y} —— 世界底部, 可省略; 省略时保持该维度原本的值。</li>
 *   <li>{@code height} —— 世界总高度 (不是上限坐标)。原版主世界 {@code min_y = -64}、{@code height = 384},
 *       即 -64..319; 想改成 -64..511 就写 {@code min_y = -64}、{@code height = 512}。</li>
 * </ul>
 *
 * <p><strong>它改不了什么</strong>: 地形本身的生成范围由数据包的 {@code noise_settings} 决定, 本模组不动它
 * (原版密度函数按绝对 Y 取值, 挪动 noise_settings 会得到不可预期的地形)。所以调高上限 = 多出可以建造的空气,
 * 调低下限 = 世界底部多出一层空腔, 而不是「凭空长出新地形」。原版自己会做裁剪
 * ({@code NoiseSettings.clampToHeightAccessor}), 高度改小时地形会被裁掉, 不会越界崩服。
 *
 * <p><strong>只对新世界有效</strong>: 区块存档里的段是按当时的高度写下去的, 高度一变就对不上号
 * (高度图长度、段索引全错位), 已生成过的世界会读不出来甚至损坏。改动前务必备份。
 */
public final class WorldHeightOverrides
{
    private static final Logger LOGGER = LogUtils.getLogger();

    /** 数据包里的目录名: {@code data/<任意命名空间>/world_height/*.json} */
    private static final String DIRECTORY = "world_height";
    private static final String SUFFIX = ".json";

    /**
     * 允许的世界高度上限 (格)。
     *
     * <p>原版协议允许的上限大得离谱 (16777184), 但区块是按 {@code height / 16} 个段实打实分配数组的,
     * 高度真填成天文数字会让每个区块凭空申请上百万个段, 直接 OOM。这里卡在 4096 —— 已经是原版主世界的
     * 十倍多, 足够任何正常玩法, 又能挡住手滑写错的数量级。
     */
    private static final int HEIGHT_LIMIT = 4096;

    /** 允许的世界底部下限 (格), 同样是为了挡住离谱的数值 */
    private static final int BOTTOM_LIMIT = -4096;

    /** 从数据包解析出来的覆盖表: 维度类型 ID -> 目标高度范围 */
    private static volatile Map<ResourceLocation, Override> overrides = Map.of();

    /**
     * 一条覆盖。
     *
     * @param minY   目标世界底部, {@code null} 表示保持该维度原本的底部
     * @param height 目标世界总高度 (格)
     */
    public record Override(@Nullable Integer minY, int height)
    {
    }

    /**
     * 读取数据包里的全部覆盖文件并写入内存。由 mixin 在维度类型注册表开始加载时调用。
     *
     * <p>功能总开关关闭时直接清空 —— 既不读文件也不打日志, 保证「关掉 = 完全不干预」。
     */
    public static void loadFrom(ResourceManager manager)
    {
        if (!BetterExperienceServerConfig.worldHeightEnabled)
        {
            overrides = Map.of();
            return;
        }

        // 排序后再合并, 让「同一个维度被多个数据包写到」时的结果稳定可复现
        Map<ResourceLocation, Override> parsed = new TreeMap<>();
        Map<ResourceLocation, Resource> files = new TreeMap<>(WorldHeightOverrides::compareFiles);
        files.putAll(manager.listResources(DIRECTORY, path -> path.getPath().endsWith(SUFFIX)));

        for (Map.Entry<ResourceLocation, Resource> entry : files.entrySet())
        {
            readFile(entry.getKey(), entry.getValue(), parsed);
        }

        overrides = Map.copyOf(parsed);
        LOGGER.info("[better_experience] 世界高度覆盖已加载: {} 个维度 (来自 {} 个数据包文件)",
                overrides.size(), files.size());
    }

    /**
     * 决定合并顺序: <strong>模组自带的模板永远排最前</strong> (data/better_experience/world_height/ 里那份),
     * 其余数据包的文件按资源 ID 排序跟在后面。
     *
     * <p>这样一来, 玩家自己数据包里的文件一定覆盖模板 —— 如果只按资源 ID 排序, 命名空间恰好排在
     * {@code better_experience} 前面的数据包反而会被模板盖掉, 那就太反直觉了。
     */
    private static int compareFiles(ResourceLocation a, ResourceLocation b)
    {
        boolean aBuiltin = BetterExperience.MOD_ID.equals(a.getNamespace());
        boolean bBuiltin = BetterExperience.MOD_ID.equals(b.getNamespace());
        if (aBuiltin != bBuiltin)
        {
            return aBuiltin ? -1 : 1;
        }
        return a.compareTo(b);
    }

    /**
     * 把一条注册中的对象按覆盖表改写。只认维度类型的值, 其它注册表原样返回。
     *
     * <p>这个方法会被<strong>每一次注册</strong>调用 (每张数据包注册表的每一条目), 所以最前面那两句
     * 是快速返回路径: 没配覆盖、或者不是维度类型, 立刻原样返回。
     */
    @SuppressWarnings("unchecked")
    public static <E> E apply(ResourceKey<E> key, E value)
    {
        if (overrides.isEmpty() || !(value instanceof DimensionType type))
        {
            return value;
        }

        Override override = overrides.get(key.location());
        if (override == null)
        {
            return value;
        }

        DimensionType patched = patch(key.location(), type, override);
        return patched == null ? value : (E) patched;
    }

    /**
     * 构造改写后的 {@link DimensionType}; 数值不合法时打日志并返回 {@code null} (调用方保持原值)。
     *
     * <p>{@code DimensionType} 是 record, 校验全在紧凑构造器里 (高度至少 16、必须是 16 的倍数、
     * 底部必须是 16 的倍数、{@code min_y + height} 不能超过原版硬上限、{@code logicalHeight} 不能大于高度),
     * 这里先自己校验一遍是为了给出<strong>能看懂的中文提示</strong>, 而不是让游戏抛一句
     * "height has to be multiple of 16" 崩在世界加载上。
     */
    @Nullable
    private static DimensionType patch(ResourceLocation id, DimensionType original, Override override)
    {
        int minY = override.minY() != null ? override.minY() : original.minY();
        int height = override.height();

        if (height < 16 || height % 16 != 0)
        {
            LOGGER.warn("[better_experience] 世界高度覆盖 {}: height = {} 不合法, 必须是 16 的倍数且不小于 16, 已忽略该项",
                    id, height);
            return null;
        }
        if (minY % 16 != 0)
        {
            LOGGER.warn("[better_experience] 世界高度覆盖 {}: min_y = {} 不合法, 必须是 16 的倍数, 已忽略该项", id, minY);
            return null;
        }
        if (minY < BOTTOM_LIMIT || height > HEIGHT_LIMIT || minY + height > HEIGHT_LIMIT)
        {
            LOGGER.warn("[better_experience] 世界高度覆盖 {}: min_y = {} / height = {} 超出本模组允许的范围 "
                            + "(底部不低于 {}, 高度不超过 {}, 且 min_y + height 不大于 {}), 已忽略该项",
                    id, minY, height, BOTTOM_LIMIT, HEIGHT_LIMIT, HEIGHT_LIMIT);
            return null;
        }
        if (minY == original.minY() && height == original.height())
        {
            // 和原值一样, 没必要换一个新实例 (保持对象同一性, 也少一次无意义的日志)
            return original;
        }

        // logicalHeight 是「逻辑高度」(传送门 / 紫颂果落脚等用), 原版要求它不大于 height,
        // 把世界改矮时必须一起夹紧, 否则 record 的构造器会直接抛异常
        int logicalHeight = Math.min(original.logicalHeight(), height);

        DimensionType patched = new DimensionType(
                original.fixedTime(),
                original.hasSkyLight(),
                original.hasCeiling(),
                original.ultraWarm(),
                original.natural(),
                original.coordinateScale(),
                original.bedWorks(),
                original.respawnAnchorWorks(),
                minY,
                height,
                logicalHeight,
                original.infiniburn(),
                original.effectsLocation(),
                original.ambientLight(),
                original.monsterSettings());

        LOGGER.info("[better_experience] 世界高度覆盖 {}: {}{} 格 -> {}{} 格",
                id,
                original.minY(), range(original.minY(), original.height()),
                minY, range(minY, height));
        return patched;
    }

    /** 把一个高度范围写成 "..上界" 的形式, 只用于日志 */
    private static String range(int minY, int height)
    {
        return ".." + (minY + height - 1) + " (共 " + height + " 格)";
    }

    /** 读取单个数据包文件; 解析失败的条目逐条跳过, 不阻断游戏 */
    private static void readFile(ResourceLocation file, Resource resource, Map<ResourceLocation, Override> out)
    {
        try (var in = resource.open())
        {
            JsonElement root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            if (!root.isJsonArray())
            {
                LOGGER.warn("[better_experience] 世界高度文件 {} 的顶层不是 JSON 数组, 已忽略该文件", file);
                return;
            }

            for (JsonElement element : root.getAsJsonArray())
            {
                if (!element.isJsonObject())
                {
                    LOGGER.warn("[better_experience] 世界高度文件 {} 里有不是对象的条目, 已忽略: {}", file, element);
                    continue;
                }

                JsonObject object = element.getAsJsonObject();

                ResourceLocation dimension = readId(object, "dimension");
                if (dimension == null)
                {
                    LOGGER.warn("[better_experience] 世界高度文件 {} 的条目缺少合法的 dimension, 已忽略: {}", file, element);
                    continue;
                }

                Integer height = readInt(object, "height", file, dimension);
                if (height == null)
                {
                    LOGGER.warn("[better_experience] 世界高度文件 {} 的条目 {} 缺少合法的 height, 已忽略", file, dimension);
                    continue;
                }

                // min_y 可省略, 省略时保持该维度原本的世界底部
                Integer minY = readInt(object, "min_y", file, dimension);
                if (minY == null && object.has("min_y"))
                {
                    LOGGER.warn("[better_experience] 世界高度文件 {} 的条目 {} 的 min_y 不是整数, 已按「省略」处理 (保持原世界底部)",
                            file, dimension);
                }
                Override previous = out.put(dimension, new Override(minY, height));
                if (previous != null)
                {
                    LOGGER.warn("[better_experience] 维度 {} 的世界高度被多个数据包文件重复定义, 以 {} 为准 "
                            + "(模组自带的模板排最前, 其余按资源 ID 排序, 靠后的覆盖靠前的)", dimension, file);
                }
            }
        }
        catch (IOException | RuntimeException e)
        {
            LOGGER.warn("[better_experience] 读取世界高度文件 {} 失败, 已忽略: {}", file, e.toString());
        }
    }

    /** 读一个 {@code namespace:path} 形式的 ID, 缺失或不合法时返回 null */
    @Nullable
    private static ResourceLocation readId(JsonObject object, String key)
    {
        JsonElement element = object.get(key);
        return element == null || !element.isJsonPrimitive() ? null : ResourceLocation.tryParse(element.getAsString());
    }

    /** 读一个整数; 缺失或不是整数时返回 null, 并记下是哪个文件 / 哪个维度出错 */
    @Nullable
    private static Integer readInt(JsonObject object, String key, ResourceLocation file, ResourceLocation dimension)
    {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull())
        {
            return null;
        }
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber())
        {
            LOGGER.warn("[better_experience] 世界高度文件 {} 的条目 {} 的 {} 不是整数, 读到的是 {}",
                    file, dimension, key, element);
            return null;
        }
        return element.getAsInt();
    }
}
