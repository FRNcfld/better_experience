package com.frnc.better_experience;

import com.frnc.better_experience.chunkdevourer.ChunkDevourerToolCost;
import com.frnc.better_experience.stepassist.StepAssistMode;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/**
 * 模组配置。走 Forge 标准配置系统，生成在 {@code config/better_experience-common.toml}。
 *
 * <p>用 COMMON 而不是 SERVER：这些开关都是"服务器整体策略"（清理间隔、默认开关等），
 * 放全局比每个存档一份更好用；专用服务器上以服务端读到的值为准。
 *
 * <p><strong>分节约定</strong>：小节用夹在字段之间的 {@code static} 块里的 {@code push()} / {@code pop()} 划分。
 * 静态初始化按书写顺序执行，因此 push 之后定义的项就归入该小节，pop 之后回到上一层；
 * push 之前挂着的 {@code comment()} 会成为 TOML 里该小节的标题注释。
 * 新增配置项时按所属小节就近插入即可；新增小节则在它开头补 pop、结尾补 push。
 *
 * <p>本类按功能分 11 个小节：{@code [double_jump]}、{@code [elytra_flight]}、{@code [step_assist]}、
 * {@code [item_cleanup]}、{@code [enchanted_golden_apple]}、{@code [flat_bedrock]}、{@code [world_height]}、
 * {@code [extended_gamma]}、{@code [saturation]}、{@code [ocean_blessing]}、{@code [chunk_devourer]}。
 * 每个功能的总开关都是该小节的第一项。
 *
 * <p>配置项定义 / 运行时字段 / {@link #onLoad} 的读取这三处的顺序完全一致，便于对照查找。
 */
@Mod.EventBusSubscriber(modid = BetterExperience.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class BetterExperienceServerConfig
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // ==================== 二段跳 ====================
    static
    {
        BUILDER.comment("二段跳: 空中再按一次跳跃键, 可再获得一次跳跃").push("double_jump");
    }

    private static final ForgeConfigSpec.BooleanValue DOUBLE_JUMP_ENABLED = BUILDER
            .comment("每个玩家的二段跳初始开关",
                     "玩家可以用按键绑定各自切换, 不受这里影响")
            .define("doubleJumpEnabled", true);

    private static final ForgeConfigSpec.BooleanValue DOUBLE_JUMP_FEATURE_ENABLED = BUILDER
            .comment("二段跳功能的总开关",
                     "关闭后该功能整体禁用, 玩家无法在游戏内重新打开 (与上面的初始开关无关)")
            .define("doubleJumpFeatureEnabled", true);

    // ==================== 鞘翅飞行开关 ====================
    static
    {
        BUILDER.pop().comment("鞘翅飞行开关: 关闭时无法起飞").push("elytra_flight");
    }

    private static final ForgeConfigSpec.BooleanValue ELYTRA_FLIGHT_ENABLED = BUILDER
            .comment("鞘翅飞行开关功能的总开关",
                     "关闭后按键无效, 鞘翅行为回到原版 (始终可以起飞)")
            .define("elytraFlightEnabled", true);

    // ==================== 上坡辅助 ====================
    static
    {
        BUILDER.pop().comment("上坡辅助: 可直接走上整格方块").push("step_assist");
    }

    private static final ForgeConfigSpec.BooleanValue STEP_ASSIST_ENABLED = BUILDER
            .comment("上坡辅助功能的总开关",
                     "关闭后模式按键无效, 台阶高度保持原版")
            .define("stepAssistEnabled", true);

    private static final ForgeConfigSpec.EnumValue<StepAssistMode> STEP_ASSIST_MODE = BUILDER
            .comment("上坡辅助的初始档位",
                     "OFF = 原版, STEP = 平滑走上整格, AUTO_JUMP = 原版自动跳跃",
                     "玩家可以用按键各自循环切换自己的档位")
            .defineEnum("stepAssistMode", StepAssistMode.OFF);

    private static final ForgeConfigSpec.DoubleValue STEP_ASSIST_STEP_HEIGHT = BUILDER
            .comment("行走时能平滑走上的高度 (格), 原版为 0.6",
                     "注意: 数值过大时服务端可能拒绝你的位移并把角色拉回, 多人服务器请谨慎调高")
            .defineInRange("stepAssistStepHeight", 1.25D, 0.0D, 10.0D);

    private static final ForgeConfigSpec.DoubleValue STEP_ASSIST_SNEAK_HEIGHT = BUILDER
            .comment("潜行时能平滑走上的高度 (格), 原版为 0.6")
            .defineInRange("stepAssistSneakHeight", 0.6D, 0.0D, 10.0D);

    private static final ForgeConfigSpec.DoubleValue STEP_ASSIST_SPRINT_HEIGHT = BUILDER
            .comment("疾跑时能平滑走上的高度 (格), 原版为 0.6")
            .defineInRange("stepAssistSprintHeight", 1.25D, 0.0D, 10.0D);

    // ==================== 掉落物清理 ====================
    static
    {
        BUILDER.pop().comment("掉落物定时清理: 支持维度 / 物品黑白名单与多种保护规则").push("item_cleanup");
    }

    private static final ForgeConfigSpec.BooleanValue CLEANUP_ENABLED = BUILDER
            .comment("是否定期清理掉落物")
            .define("cleanupEnabled", true);

    private static final ForgeConfigSpec.IntValue CLEANUP_INTERVAL_SECONDS = BUILDER
            .comment("两次自动清理之间的间隔 (秒)")
            .defineInRange("cleanupIntervalSeconds", 600, 10, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.IntValue CLEANUP_WARNING_SECONDS = BUILDER
            .comment("清理前多少秒广播一次预警 (0 = 不预警)")
            .defineInRange("cleanupWarningSeconds", 10, 0, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.BooleanValue CLEANUP_BROADCAST_RESULT = BUILDER
            .comment("清理结束后是否广播结果")
            .define("cleanupBroadcastResult", true);

    private static final ForgeConfigSpec.IntValue CLEANUP_MIN_ITEM_AGE_SECONDS = BUILDER
            .comment("掉落物至少存在多少秒后才可能被清理, 用于保护刚掉落的东西")
            .defineInRange("cleanupMinimumItemAgeSeconds", 10, 0, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.BooleanValue CLEANUP_PROTECT_NAMED_ITEMS = BUILDER
            .comment("是否保护带自定义名称的掉落物")
            .define("cleanupProtectNamedItems", true);

    private static final ForgeConfigSpec.BooleanValue CLEANUP_PROTECT_PLAYER_DEATH_DROPS = BUILDER
            .comment("是否保护玩家死亡时掉落的物品")
            .define("cleanupProtectPlayerDeathDrops", true);

    private static final ForgeConfigSpec.IntValue CLEANUP_DEATH_DROP_PROTECTION_SECONDS = BUILDER
            .comment("玩家死亡掉落受保护的时长 (秒)")
            .defineInRange("cleanupPlayerDeathDropProtectionSeconds", 30, 0, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.IntValue CLEANUP_MAX_ITEMS_PER_BATCH = BUILDER
            .comment("清理时每 tick 最多移除多少个, 用于避免卡顿")
            .defineInRange("cleanupMaxItemsPerBatch", 500, 1, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.BooleanValue CLEANUP_ITEM_WHITELIST_ENABLE = BUILDER
            .comment("物品白名单是否生效 (白名单 = 受保护, 不清)",
                     "名单在数据包里: data/better_experience/dropped_item_cleanup/whitelist.json")
            .define("cleanupItemWhitelistEnable", false);

    private static final ForgeConfigSpec.BooleanValue CLEANUP_ITEM_BLACKLIST_ENABLE = BUILDER
            .comment("物品黑名单是否生效 (黑名单 = 必定清理, 无视后面的保护规则)",
                     "名单在数据包里: data/better_experience/dropped_item_cleanup/blacklist.json")
            .define("cleanupItemBlacklistEnable", false);

    private static final ForgeConfigSpec.BooleanValue CLEANUP_DIMENSION_WHITELIST_ENABLE = BUILDER
            .comment("维度白名单是否生效 (白名单 = 跳过该维度, 不清)")
            .define("cleanupDimensionWhitelistEnable", false);

    private static final ForgeConfigSpec.BooleanValue CLEANUP_DIMENSION_BLACKLIST_ENABLE = BUILDER
            .comment("维度黑名单是否生效 (黑名单 = 清理该维度)")
            .define("cleanupDimensionBlacklistEnable", false);

    // ==================== 附魔金苹果强化 ====================
    static
    {
        BUILDER.pop().comment("附魔金苹果强化").push("enchanted_golden_apple");
    }

    private static final ForgeConfigSpec.BooleanValue ENCHANTED_GOLDEN_APPLE_BUFF_ENABLED = BUILDER
            .comment("是否强化附魔金苹果的生命恢复效果",
                     "原版为生命恢复 II 级 20 秒, 本模组改为生命恢复 V 级 60 秒",
                     "立即生效, 无需重启")
            .define("enchantedGoldenAppleBuffEnabled", true);

    // ==================== 平坦基岩 ====================
    static
    {
        BUILDER.pop().comment("平坦基岩").push("flat_bedrock");
    }

    private static final ForgeConfigSpec.BooleanValue FLAT_BEDROCK_ENABLED = BUILDER
            .comment("主世界底部与下界顶 / 底是否只生成 1 层基岩, 取代原版 1-5 层的锯齿状",
                     "只影响新生成的区块, 已生成过的区块不会改变")
            .define("flatBedrockEnabled", true);

    // ==================== 世界高度覆盖 ====================
    static
    {
        BUILDER.pop().comment("世界高度覆盖: 按维度改写世界的可用高度范围 (数据包驱动)").push("world_height");
    }

    private static final ForgeConfigSpec.BooleanValue WORLD_HEIGHT_ENABLED = BUILDER
            .comment("世界高度覆盖的总开关",
                     "具体每个维度改成多高写在数据包里: data/<任意命名空间>/world_height/*.json",
                     "格式 (顶层是数组; dimension 填维度类型 ID, 原版三个是 minecraft:overworld / the_nether / the_end):",
                     "  [ { \"dimension\": \"minecraft:overworld\", \"min_y\": -64, \"height\": 512 } ]",
                     "min_y 可省略, 省略时保持该维度原本的世界底部;",
                     "height 是世界总高度而不是上限坐标 —— 原版主世界是 min_y = -64 / height = 384 (即 -64..319),",
                     "想让它变成 -64..511 就写 min_y = -64 / height = 512",
                     "本模组允许的范围: 高度 16..4096 且必须是 16 的倍数; min_y 必须是 16 的倍数且不低于 -4096,",
                     "并且 min_y + height 不超过 4096 (卡这个范围是为了挡住手滑写错的数量级: 区块是按 高度/16 个段分配数组的)",
                     "注意一: 只改变可用的高度范围, 不改变地形生成范围 (地形由数据包的 noise_settings 决定)。",
                     "  调高上限 = 多出可以建造的空气, 调低下限 = 世界底部多出一层空腔, 而不是凭空长出新地形",
                     "注意二: 只对**新世界**有效。已经生成过区块的世界改了高度会让区块数据与高度对不上号,",
                     "  存档会读不出来甚至损坏 —— 改动前务必备份",
                     "关闭本项时完全不读数据包, 也不改任何维度")
            .define("worldHeightEnabled", false);

    // ==================== 亮度扩展 ====================
    static
    {
        BUILDER.pop().comment("亮度 (伽马) 扩展").push("extended_gamma");
    }

    private static final ForgeConfigSpec.BooleanValue EXTENDED_GAMMA_ENABLED = BUILDER
            .comment("亮度 (伽马) 上限是否由原版 100% 放宽到 1000%",
                     "本项只决定游戏内开关的初值, 改动需要重启游戏才生效; 游戏内可用热键随时切换开关与调整数值",
                     "关闭时按原版亮度渲染 (值仍能调整, 只是不生效)",
                     "Sodium / Rubidium / Embeddium 的视频设置界面里亮度滑块仍是 0-100%"
                             + " (本模组不修改任何界面类), 那些环境用「调整亮度」热键 (按住 + 滚轮) 调值")
            .define("extendedGammaEnabled", true);

    // ==================== 饱和度机制 ====================
    static
    {
        BUILDER.pop().comment("饱和度机制: 饱和度不被饥饿值封顶, 可无限进食").push("saturation");
    }

    private static final ForgeConfigSpec.BooleanValue SATURATION_ENABLED = BUILDER
            .comment("饱和度机制的总开关",
                     "关闭后下列设置全部跳过, 食物行为与原版完全一致")
            .define("saturationEnabled", true);

    private static final ForgeConfigSpec.BooleanValue SATURATION_ALWAYS_HUNGRY = BUILDER
            .comment("是否允许随时进食, 不看饥饿条有多满",
                     "要把饥饿满时多出来的营养转成饱和度, 需要打开它")
            .define("saturationAlwaysHungry", true);

    private static final ForgeConfigSpec.BooleanValue SATURATION_HUNGER_LIMITS_SATURATION = BUILDER
            .comment("饱和度是否被当前饥饿值封顶 (这是原版行为)",
                     "保持 false 才能让饱和度存到饥饿值以上")
            .define("saturationHungerLimitsSaturation", false);

    private static final ForgeConfigSpec.IntValue SATURATION_MAX_SATURATION = BUILDER
            .comment("储存的饱和度硬上限, 负数表示不设上限")
            .defineInRange("saturationMaxSaturation", -1, Integer.MIN_VALUE, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.IntValue SATURATION_MAX_EXHAUSTION = BUILDER
            .comment("累积多少消耗度才会掉饥饿值或饱和度 (原版为 4)",
                     "<= 0 表示完全不掉")
            .defineInRange("saturationMaxExhaustion", 4, Integer.MIN_VALUE, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.DoubleValue SATURATION_OVERFLOW_RATIO = BUILDER
            .comment("饥饿满时进食, 溢出的营养按多大比例转成饱和度",
                     "1.0 = 1 点溢出营养换 1 点饱和度")
            .defineInRange("saturationOverflowRatio", 1.0D, 0.0D, 100.0D);

    private static final ForgeConfigSpec.IntValue SATURATION_EFFECT_DECAY_RATE = BUILDER
            .comment("饱和度状态效果的增益随饱和度升高而衰减的强度",
                     "越大衰减越狠, 0 = 不衰减")
            .defineInRange("saturationEffectDecayRate", 2, 0, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.BooleanValue SATURATION_SHOW_HUD = BUILDER
            .comment("是否在 HUD 上显示饱和度读数 (仅生存 / 冒险模式)",
                     "这是客户端显示选项")
            .define("saturationShowHud", true);

    // ==================== 海洋之祝 ====================
    static
    {
        BUILDER.pop().comment("海洋之祝: 三叉戟专属附魔, 解除激流与水 / 雨、引雷与雷雨天、穿刺与水生生物的绑定").push("ocean_blessing");
    }

    private static final ForgeConfigSpec.BooleanValue OCEAN_BLESSING_ENABLED = BUILDER
            .comment("海洋之祝附魔功能的总开关",
                     "关闭后此附魔不再解除激流的水 / 雨限制与引雷的雷雨天限制, 也不再让穿刺的加伤对所有目标生效",
                     "附魔本身与其附魔书仍然存在 (创造模式原材料页里照常能取到), 只是不再有任何效果",
                     "注意: 激流与引雷在原版互斥, 同一把三叉戟只能二选一, 因此这两个效果不会同时生效")
            .define("oceanBlessingEnabled", true);

    // ==================== 区块吞噬者 ====================
    static
    {
        BUILDER.pop().comment("区块吞噬者: 钻石镐及以上专属附魔, 挖方块时删除以它所在区块为中心的一整片区块").push("chunk_devourer");
    }

    private static final ForgeConfigSpec.BooleanValue CHUNK_DEVOURER_ENABLED = BUILDER
            .comment("区块吞噬者附魔功能的总开关",
                     "关闭后此附魔不再有任何效果, 挖方块就是普通挖方块",
                     "已经排进队列、正在删的任务会被立刻中止 (不会留一个跑到一半的任务占着内存)",
                     "附魔本身与其附魔书仍然存在 (创造模式原材料页里照常能取到), 只是不再触发")
            .define("chunkDevourerEnabled", true);

    private static final ForgeConfigSpec.BooleanValue CHUNK_DEVOURER_DROPS_ITEMS = BUILDER
            .comment("被吞噬的方块是否掉落物品与经验",
                     "false (默认) = 单纯删除, 什么都不掉也不给经验, 符合「吞噬」的定位",
                     "true = 按原版规则掉落, 时运 / 精准采集照常生效, 经验照常给",
                     "注意: 3 级一次约 86 万格方块, 打开后会一次刷出海量掉落物与实体, 谨慎使用")
            .define("chunkDevourerDropsItems", false);

    private static final ForgeConfigSpec.EnumValue<ChunkDevourerToolCost> CHUNK_DEVOURER_TOOL_COST = BUILDER
            .comment("触发一次所付出的工具代价",
                     "DESTROY_TOOL (默认) = 直接把镐子损毁, 也就是一把镐换一次; 此项无视耐久附魔",
                     "SINGLE_DURABILITY = 不论删了多少格, 整次操作只扣 1 点耐久 (耐久附魔可豁免)",
                     "两种都不按格扣耐久: 按格扣的话 5×5 一次要多扣约 86 万点, 任何镐子都是秒碎",
                     "创造模式两种都不消耗耐久 (与原版一致)")
            .defineEnum("chunkDevourerToolCost", ChunkDevourerToolCost.DESTROY_TOOL);

    private static final ForgeConfigSpec.IntValue CHUNK_DEVOURER_BLOCKS_PER_TICK = BUILDER
            .comment("每 tick 最多删除多少格方块, 用来把负载摊到多个 tick, 避免服务端卡死",
                     "等级 1 (1 个区块) 约 3.5 万格, 等级 2 (9 个) 约 31 万格, 等级 3 (25 个) 约 86 万格",
                     "按默认 4096 算: 等级 1 约需几 tick, 等级 3 约需 10 秒",
                     "调小更平滑但更慢, 调大更快但每 tick 的卡顿更明显")
            .defineInRange("chunkDevourerBlocksPerTick", 4096, 1, 65536);

    private static final ForgeConfigSpec.BooleanValue CHUNK_DEVOURER_KEEP_PLAYER_COLUMN = BUILDER
            .comment("是否保留触发者脚下那一列 (1×1, 整列从世界底部到建筑上限) 不删",
                     "默认 true 是为了让玩家有个落脚点",
                     "改成 false 就是字面意义的「整个区块」: 连脚下一起删到世界底部,",
                     "而世界底部之下没有方块, 玩家会直接掉进虚空摔死 —— 除非开着创造 / 鞘翅 / 缓降")
            .define("chunkDevourerKeepPlayerColumn", true);

    private static final ForgeConfigSpec.IntValue CHUNK_DEVOURER_BARTER_WEIGHT = BUILDER
            .comment("猪灵交易出这本附魔书的权重 (交易只出 1 级书, 2 / 3 级必须靠铁砧合并)",
                     "原版猪灵交易表只有一个池、18 个条目, 权重合计 459",
                     "本项表示「相当于往原版池里塞一个权重为 N 的条目」, 命中率 = N / (459 + N)",
                     "默认 20 约为 4.2%, 也就是平均约 24 次交易出一本",
                     "参考: 原版表里最常见的条目权重是 40 (约 8%), 最稀有的附魔书 (灵魂疾行) 是 5 (约 1.1%)",
                     "一把 3 级镐要 4 本 1 级书 (1+1=2, 1+1=2, 2+2=3), 而镐子每触发一次就损毁,",
                     "所以这个值给得比一般的稀有附魔宽松; 调到 459 则每次必出, 方便调试")
            .defineInRange("chunkDevourerBarterWeight", 20, 1, 459);

    // ==================== 收尾 ====================
    static
    {
        BUILDER.pop();
    }

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    // ==================== 运行时缓存值 ====================
    // 其余代码只读这里, 不直接碰上面的 ConfigValue。
    // 初值规则: 会在配置加载完成之前就被读取的项, 初值必须写成本配置里的默认值, 否则加载前
    // 那一小段时间会按 false / 0 使用; 只在加载之后才被读到的项 (例如掉落物清理) 留空即可。

    // ---- 二段跳 ----
    public static boolean doubleJumpEnabled;

    // 功能级总开关: 初值同样取配置文件默认值, 因为它们在配置加载完成前就可能被读取
    public static boolean doubleJumpFeatureEnabled = true;

    // ---- 鞘翅飞行开关 ----
    public static boolean elytraFlightEnabled = true;

    // ---- 上坡辅助 ----
    public static boolean stepAssistEnabled = true;

    // 上坡辅助同样在世界加载前就可能被读取, 初值取配置文件默认值
    public static StepAssistMode stepAssistMode = StepAssistMode.OFF;
    public static double stepAssistStepHeight = 1.25D;
    public static double stepAssistSneakHeight = 0.6D;
    public static double stepAssistSprintHeight = 1.25D;

    // ---- 掉落物清理 ----
    public static boolean cleanupEnabled;
    public static int cleanupIntervalSeconds;
    public static int cleanupWarningSeconds;
    public static boolean cleanupBroadcastResult;
    public static int cleanupMinimumItemAgeSeconds;
    public static boolean cleanupProtectNamedItems;
    public static boolean cleanupProtectPlayerDeathDrops;
    public static int cleanupPlayerDeathDropProtectionSeconds;
    public static int cleanupMaxItemsPerBatch;
    public static boolean cleanupItemWhitelistEnable;
    public static boolean cleanupItemBlacklistEnable;
    public static boolean cleanupDimensionWhitelistEnable;
    public static boolean cleanupDimensionBlacklistEnable;

    // ---- 附魔金苹果强化 ----
    // 初值写成配置文件的默认值: 该字段在配置加载完成前就已是可读状态, 避免被误判为"关闭"
    public static boolean enchantedGoldenAppleBuffEnabled = true;

    // ---- 平坦基岩 ----
    // 基岩 mixin 在世界生成期就会读它, 伽马 mixin 在 Options 构造期读 extendedGammaEnabled,
    // 两者都早于配置加载完成, 所以初值必须与配置文件默认值一致, 不能依赖 onLoad
    public static boolean flatBedrockEnabled = true;

    // ---- 世界高度覆盖 ----
    // 在数据包注册表加载时被读 (那时配置早已加载完), 初值同样与配置文件默认值保持一致
    public static boolean worldHeightEnabled = false;

    // ---- 亮度扩展 ----
    public static boolean extendedGammaEnabled = true;

    // ---- 饱和度机制 ----
    // 同样把初值写成配置文件默认值 (mixin 在运行时读, 不受加载顺序影响, 但保持一致更安全)
    public static boolean saturationEnabled = true;
    public static boolean saturationAlwaysHungry = true;
    public static boolean saturationHungerLimitsSaturation = false;
    public static int saturationMaxSaturation = -1;
    public static int saturationMaxExhaustion = 4;
    public static double saturationOverflowRatio = 1.0D;
    public static int saturationEffectDecayRate = 2;
    public static boolean saturationShowHud = true;

    // ---- 海洋之祝 ----
    // 只在配置加载之后才会被读到 (玩家使用三叉戟时、构建创造模式标签页时), 所以按上面的规则留空
    public static boolean oceanBlessingEnabled;

    // ---- 区块吞噬者 ----
    // 破坏方块的事件处理器在游戏跑起来之后才读它们, 但队列是静态的、也读配置, 保持一致更安全,
    // 因此初值仍与配置文件默认值一致, 不依赖 onLoad
    public static boolean chunkDevourerEnabled = true;
    public static boolean chunkDevourerDropsItems = false;
    public static ChunkDevourerToolCost chunkDevourerToolCost = ChunkDevourerToolCost.DESTROY_TOOL;
    public static int chunkDevourerBlocksPerTick = 4096;
    public static boolean chunkDevourerKeepPlayerColumn = true;
    public static int chunkDevourerBarterWeight = 20;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        // ModConfigEvent 对**所有**配置都会触发。本模组现在有两份配置 (COMMON + CLIENT),
        // 不加这个判断就会在其中一份加载时去读另一份尚未加载的值, 抛
        // "Cannot get config value before config is loaded"。
        if (event.getConfig().getType() != ModConfig.Type.COMMON) return;

        doubleJumpEnabled = DOUBLE_JUMP_ENABLED.get();
        doubleJumpFeatureEnabled = DOUBLE_JUMP_FEATURE_ENABLED.get();
        elytraFlightEnabled = ELYTRA_FLIGHT_ENABLED.get();
        stepAssistEnabled = STEP_ASSIST_ENABLED.get();
        stepAssistMode = STEP_ASSIST_MODE.get();
        stepAssistStepHeight = STEP_ASSIST_STEP_HEIGHT.get();
        stepAssistSneakHeight = STEP_ASSIST_SNEAK_HEIGHT.get();
        stepAssistSprintHeight = STEP_ASSIST_SPRINT_HEIGHT.get();
        cleanupEnabled = CLEANUP_ENABLED.get();
        cleanupIntervalSeconds = CLEANUP_INTERVAL_SECONDS.get();
        cleanupWarningSeconds = CLEANUP_WARNING_SECONDS.get();
        cleanupBroadcastResult = CLEANUP_BROADCAST_RESULT.get();
        cleanupMinimumItemAgeSeconds = CLEANUP_MIN_ITEM_AGE_SECONDS.get();
        cleanupProtectNamedItems = CLEANUP_PROTECT_NAMED_ITEMS.get();
        cleanupProtectPlayerDeathDrops = CLEANUP_PROTECT_PLAYER_DEATH_DROPS.get();
        cleanupPlayerDeathDropProtectionSeconds = CLEANUP_DEATH_DROP_PROTECTION_SECONDS.get();
        cleanupMaxItemsPerBatch = CLEANUP_MAX_ITEMS_PER_BATCH.get();
        cleanupItemWhitelistEnable = CLEANUP_ITEM_WHITELIST_ENABLE.get();
        cleanupItemBlacklistEnable = CLEANUP_ITEM_BLACKLIST_ENABLE.get();
        cleanupDimensionWhitelistEnable = CLEANUP_DIMENSION_WHITELIST_ENABLE.get();
        cleanupDimensionBlacklistEnable = CLEANUP_DIMENSION_BLACKLIST_ENABLE.get();
        enchantedGoldenAppleBuffEnabled = ENCHANTED_GOLDEN_APPLE_BUFF_ENABLED.get();
        flatBedrockEnabled = FLAT_BEDROCK_ENABLED.get();
        worldHeightEnabled = WORLD_HEIGHT_ENABLED.get();
        extendedGammaEnabled = EXTENDED_GAMMA_ENABLED.get();
        saturationEnabled = SATURATION_ENABLED.get();
        saturationAlwaysHungry = SATURATION_ALWAYS_HUNGRY.get();
        saturationHungerLimitsSaturation = SATURATION_HUNGER_LIMITS_SATURATION.get();
        saturationMaxSaturation = SATURATION_MAX_SATURATION.get();
        saturationMaxExhaustion = SATURATION_MAX_EXHAUSTION.get();
        saturationOverflowRatio = SATURATION_OVERFLOW_RATIO.get();
        saturationEffectDecayRate = SATURATION_EFFECT_DECAY_RATE.get();
        saturationShowHud = SATURATION_SHOW_HUD.get();
        oceanBlessingEnabled = OCEAN_BLESSING_ENABLED.get();
        chunkDevourerEnabled = CHUNK_DEVOURER_ENABLED.get();
        chunkDevourerDropsItems = CHUNK_DEVOURER_DROPS_ITEMS.get();
        chunkDevourerToolCost = CHUNK_DEVOURER_TOOL_COST.get();
        chunkDevourerBlocksPerTick = CHUNK_DEVOURER_BLOCKS_PER_TICK.get();
        chunkDevourerKeepPlayerColumn = CHUNK_DEVOURER_KEEP_PLAYER_COLUMN.get();
        chunkDevourerBarterWeight = CHUNK_DEVOURER_BARTER_WEIGHT.get();
    }
}
