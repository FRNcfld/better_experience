package com.frnc.better_experience;

import com.frnc.better_experience.stepassist.StepAssistMode;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/**
 * 模组配置。走 Forge 标准配置系统，生成在 {@code config/better_experience-common.toml}。
 *
 * <p>用 COMMON 而不是 SERVER：这些开关都是"服务器整体策略"（清理间隔、默认开关等），
 * 放全局比每个存档一份更好用；专用服务器上以服务端读到的值为准。
 */
@Mod.EventBusSubscriber(modid = BetterExperience.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue DOUBLE_JUMP_ENABLED = BUILDER
            .comment("Default double jump state for each player (each player can toggle their own in-game with the double jump keybind)")
            .define("doubleJumpEnabled", true);

    private static final ForgeConfigSpec.BooleanValue DOUBLE_JUMP_FEATURE_ENABLED = BUILDER
            .comment("Master switch for the double jump feature. When false it is fully disabled and players cannot turn it back on in-game, regardless of doubleJumpEnabled")
            .define("doubleJumpFeatureEnabled", true);

    private static final ForgeConfigSpec.BooleanValue ELYTRA_FLIGHT_ENABLED = BUILDER
            .comment("Master switch for the elytra flight toggle feature. When false the keybind does nothing and elytra flight behaves like vanilla (always allowed)")
            .define("elytraFlightEnabled", true);

    private static final ForgeConfigSpec.BooleanValue STEP_ASSIST_ENABLED = BUILDER
            .comment("Master switch for the step assist feature. When false the mode keybind does nothing and step height stays vanilla")
            .define("stepAssistEnabled", true);

    private static final ForgeConfigSpec.BooleanValue CLEANUP_ENABLED = BUILDER
            .comment("Whether to periodically clean up dropped items")
            .define("cleanupEnabled", true);

    private static final ForgeConfigSpec.IntValue CLEANUP_INTERVAL_SECONDS = BUILDER
            .comment("Seconds between automatic dropped-item cleanups")
            .defineInRange("cleanupIntervalSeconds", 600, 10, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.BooleanValue CLEANUP_PROTECT_NAMED_ITEMS = BUILDER
            .comment("Whether dropped items with a custom name are protected from cleanup")
            .define("cleanupProtectNamedItems", true);

    private static final ForgeConfigSpec.IntValue CLEANUP_MIN_ITEM_AGE_SECONDS = BUILDER
            .comment("Minimum age (seconds) of an item before it can be cleaned, protecting fresh drops")
            .defineInRange("cleanupMinimumItemAgeSeconds", 10, 0, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.IntValue CLEANUP_MAX_ITEMS_PER_BATCH = BUILDER
            .comment("Maximum items removed per tick during cleanup (to avoid lag)")
            .defineInRange("cleanupMaxItemsPerBatch", 500, 1, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.BooleanValue CLEANUP_BROADCAST_RESULT = BUILDER
            .comment("Whether to broadcast a chat message after each cleanup")
            .define("cleanupBroadcastResult", true);

    private static final ForgeConfigSpec.IntValue CLEANUP_WARNING_SECONDS = BUILDER
            .comment("Broadcast a warning this many seconds before each cleanup (0 = disabled)")
            .defineInRange("cleanupWarningSeconds", 10, 0, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.BooleanValue CLEANUP_ITEM_BLACKLIST_ENABLE = BUILDER
            .comment("Whether the item blacklist applies (blacklist = must clean; see data/better_experience/dropped_item_cleanup/blacklist.json)")
            .define("cleanupItemBlacklistEnable", false);

    private static final ForgeConfigSpec.BooleanValue CLEANUP_ITEM_WHITELIST_ENABLE = BUILDER
            .comment("Whether the item whitelist applies (whitelist = protected; see data/better_experience/dropped_item_cleanup/whitelist.json)")
            .define("cleanupItemWhitelistEnable", false);

    private static final ForgeConfigSpec.BooleanValue CLEANUP_DIMENSION_BLACKLIST_ENABLE = BUILDER
            .comment("Whether the dimension blacklist applies (blacklist = clean)")
            .define("cleanupDimensionBlacklistEnable", false);

    private static final ForgeConfigSpec.BooleanValue CLEANUP_DIMENSION_WHITELIST_ENABLE = BUILDER
            .comment("Whether the dimension whitelist applies (whitelist = skip)")
            .define("cleanupDimensionWhitelistEnable", false);

    private static final ForgeConfigSpec.BooleanValue CLEANUP_PROTECT_PLAYER_DEATH_DROPS = BUILDER
            .comment("Whether items dropped on player death are protected from cleanup")
            .define("cleanupProtectPlayerDeathDrops", true);

    private static final ForgeConfigSpec.IntValue CLEANUP_DEATH_DROP_PROTECTION_SECONDS = BUILDER
            .comment("How many seconds player death drops are protected from cleanup")
            .defineInRange("cleanupPlayerDeathDropProtectionSeconds", 30, 0, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.BooleanValue ENCHANTED_GOLDEN_APPLE_BUFF_ENABLED = BUILDER
            .comment("Whether to buff the Enchanted Golden Apple's Regeneration effect (vanilla: Regeneration II for 20s -> Regeneration V for 60s). Takes effect immediately, no restart needed")
            .define("enchantedGoldenAppleBuffEnabled", true);

    private static final ForgeConfigSpec.BooleanValue FLAT_BEDROCK_ENABLED = BUILDER
            .comment("Whether bedrock generates as a single flat layer at the bottom of the Overworld and the top/bottom of the Nether, instead of the vanilla 1-5 block jagged layers. Only affects newly generated chunks")
            .define("flatBedrockEnabled", true);

    private static final ForgeConfigSpec.BooleanValue EXTENDED_GAMMA_ENABLED = BUILDER
            .comment("Whether the brightness (gamma) slider goes up to 1000% instead of vanilla's 100%. Requires a game restart to take effect")
            .define("extendedGammaEnabled", true);

    private static final ForgeConfigSpec.EnumValue<StepAssistMode> STEP_ASSIST_MODE = BUILDER
            .comment("Default step assist mode. OFF = vanilla, STEP = walk up blocks smoothly, AUTO_JUMP = vanilla auto-jump. Each player can cycle their own mode in-game with the K key")
            .defineEnum("stepAssistMode", StepAssistMode.OFF);

    private static final ForgeConfigSpec.DoubleValue STEP_ASSIST_STEP_HEIGHT = BUILDER
            .comment("Height (in blocks) that can be stepped up smoothly while walking. Vanilla is 0.6. Warning: high values may cause problems on multiplayer servers")
            .defineInRange("stepAssistStepHeight", 1.25D, 0.0D, 10.0D);

    private static final ForgeConfigSpec.DoubleValue STEP_ASSIST_SNEAK_HEIGHT = BUILDER
            .comment("Height (in blocks) that can be stepped up smoothly while sneaking. Vanilla is 0.6")
            .defineInRange("stepAssistSneakHeight", 0.6D, 0.0D, 10.0D);

    private static final ForgeConfigSpec.DoubleValue STEP_ASSIST_SPRINT_HEIGHT = BUILDER
            .comment("Height (in blocks) that can be stepped up smoothly while sprinting. Vanilla is 0.6")
            .defineInRange("stepAssistSprintHeight", 1.25D, 0.0D, 10.0D);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean doubleJumpEnabled;
    // 功能级总开关: 初值同样取配置文件默认值, 因为它们在配置加载完成前就可能被读取
    public static boolean doubleJumpFeatureEnabled = true;
    public static boolean elytraFlightEnabled = true;
    public static boolean stepAssistEnabled = true;
    public static boolean cleanupEnabled;
    public static int cleanupIntervalSeconds;
    public static boolean cleanupProtectNamedItems;
    public static int cleanupMinimumItemAgeSeconds;
    public static int cleanupMaxItemsPerBatch;
    public static boolean cleanupBroadcastResult;
    public static int cleanupWarningSeconds;
    public static boolean cleanupItemBlacklistEnable;
    public static boolean cleanupItemWhitelistEnable;
    public static boolean cleanupDimensionBlacklistEnable;
    public static boolean cleanupDimensionWhitelistEnable;
    public static boolean cleanupProtectPlayerDeathDrops;
    public static int cleanupPlayerDeathDropProtectionSeconds;
    // 初值写成配置文件的默认值: 该字段在配置加载完成前就已是可读状态, 避免被误判为"关闭"
    public static boolean enchantedGoldenAppleBuffEnabled = true;
    // 同上: 这两个开关也会在配置加载完成前被读取——基岩 mixin 在世界生成期读,
    // 伽马 mixin 在 Options 构造期读, 所以初值必须与配置文件默认值一致, 不能依赖 onLoad
    public static boolean flatBedrockEnabled = true;
    public static boolean extendedGammaEnabled = true;
    // 上坡辅助同样在世界加载前就可能被读取, 初值取配置文件默认值
    public static StepAssistMode stepAssistMode = StepAssistMode.OFF;
    public static double stepAssistStepHeight = 1.25D;
    public static double stepAssistSneakHeight = 0.6D;
    public static double stepAssistSprintHeight = 1.25D;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        doubleJumpEnabled = DOUBLE_JUMP_ENABLED.get();
        doubleJumpFeatureEnabled = DOUBLE_JUMP_FEATURE_ENABLED.get();
        elytraFlightEnabled = ELYTRA_FLIGHT_ENABLED.get();
        stepAssistEnabled = STEP_ASSIST_ENABLED.get();
        cleanupEnabled = CLEANUP_ENABLED.get();
        cleanupIntervalSeconds = CLEANUP_INTERVAL_SECONDS.get();
        cleanupProtectNamedItems = CLEANUP_PROTECT_NAMED_ITEMS.get();
        cleanupMinimumItemAgeSeconds = CLEANUP_MIN_ITEM_AGE_SECONDS.get();
        cleanupMaxItemsPerBatch = CLEANUP_MAX_ITEMS_PER_BATCH.get();
        cleanupBroadcastResult = CLEANUP_BROADCAST_RESULT.get();
        cleanupWarningSeconds = CLEANUP_WARNING_SECONDS.get();
        cleanupItemBlacklistEnable = CLEANUP_ITEM_BLACKLIST_ENABLE.get();
        cleanupItemWhitelistEnable = CLEANUP_ITEM_WHITELIST_ENABLE.get();
        cleanupDimensionBlacklistEnable = CLEANUP_DIMENSION_BLACKLIST_ENABLE.get();
        cleanupDimensionWhitelistEnable = CLEANUP_DIMENSION_WHITELIST_ENABLE.get();
        cleanupProtectPlayerDeathDrops = CLEANUP_PROTECT_PLAYER_DEATH_DROPS.get();
        cleanupPlayerDeathDropProtectionSeconds = CLEANUP_DEATH_DROP_PROTECTION_SECONDS.get();
        enchantedGoldenAppleBuffEnabled = ENCHANTED_GOLDEN_APPLE_BUFF_ENABLED.get();
        flatBedrockEnabled = FLAT_BEDROCK_ENABLED.get();
        extendedGammaEnabled = EXTENDED_GAMMA_ENABLED.get();
        stepAssistMode = STEP_ASSIST_MODE.get();
        stepAssistStepHeight = STEP_ASSIST_STEP_HEIGHT.get();
        stepAssistSneakHeight = STEP_ASSIST_SNEAK_HEIGHT.get();
        stepAssistSprintHeight = STEP_ASSIST_SPRINT_HEIGHT.get();
    }
}
