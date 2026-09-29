package com.frnc.better_experience.droppeditemcleanup;

import com.frnc.better_experience.BetterExperience;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * 掉落物清理的维度/物品黑白名单, 由数据包提供:
 *   data/better_experience/dropped_item_cleanup/blacklist.json 与 whitelist.json, 格式:
 *     { "items": ["minecraft:diamond"], "dimensions": ["minecraft:overworld"] }
 *   语义: 黑名单 = 必须清理 (命中即清); 白名单 = 受保护 (命中即免清理)。
 * 数据由 {@link com.frnc.better_experience.BetterExperienceDataPackReloadListener} 在服务端数据包重载时 (含 /reload) 读取,
 * prepare 阶段读取 (loadFrom), apply 阶段写入内存 (apply)。
 */
public class CleanupLists
{
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String BLACKLIST_PATH = "dropped_item_cleanup/blacklist.json";
    private static final String WHITELIST_PATH = "dropped_item_cleanup/whitelist.json";

    private static Set<ResourceLocation> itemBlacklist = Set.of();
    private static Set<ResourceLocation> itemWhitelist = Set.of();
    private static Set<ResourceLocation> dimensionBlacklist = Set.of();
    private static Set<ResourceLocation> dimensionWhitelist = Set.of();

    /** 一次数据包重载读取到的完整黑白名单 (prepare 阶段产物) */
    public record LoadedLists(
            Set<ResourceLocation> itemBlacklist,
            Set<ResourceLocation> itemWhitelist,
            Set<ResourceLocation> dimensionBlacklist,
            Set<ResourceLocation> dimensionWhitelist)
    {
    }

    /** 从数据包资源管理器读取黑白名单; 资源缺失时保持空清单 */
    public static LoadedLists loadFrom(ResourceManager manager)
    {
        return new LoadedLists(
                readList(manager, BLACKLIST_PATH, "items"),
                readList(manager, WHITELIST_PATH, "items"),
                readList(manager, BLACKLIST_PATH, "dimensions"),
                readList(manager, WHITELIST_PATH, "dimensions"));
    }

    /** 将读取结果写入内存 (apply 阶段, 主线程) */
    public static void apply(LoadedLists data)
    {
        itemBlacklist = data.itemBlacklist();
        itemWhitelist = data.itemWhitelist();
        dimensionBlacklist = data.dimensionBlacklist();
        dimensionWhitelist = data.dimensionWhitelist();
        LOGGER.info("[better_experience] 掉落清理黑白名单已加载: 黑名单 {} 物品/{} 维度, 白名单 {} 物品/{} 维度",
                itemBlacklist.size(), dimensionBlacklist.size(), itemWhitelist.size(), dimensionWhitelist.size());
    }

    /** 从数据包资源中读取指定 key 的 ResourceLocation 列表 */
    private static Set<ResourceLocation> readList(ResourceManager manager, String path, String key)
    {
        Set<ResourceLocation> result = new HashSet<>();
        try
        {
            Optional<Resource> resource = manager.getResource(ResourceLocation.fromNamespaceAndPath(BetterExperience.MOD_ID, path));
            if (resource.isEmpty())
            {
                LOGGER.warn("[better_experience] 数据包资源 {}:{} 不存在, 对应清单为空", BetterExperience.MOD_ID, path);
                return result;
            }
            try (var in = resource.get().open())
            {
                JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                JsonElement elem = root.get(key);
                if (elem != null && elem.isJsonArray())
                {
                    for (JsonElement e : (JsonArray) elem)
                    {
                        ResourceLocation id = ResourceLocation.tryParse(e.getAsString());
                        if (id != null)
                        {
                            result.add(id);
                        }
                    }
                }
            }
        }
        catch (IOException | RuntimeException ignored)
        {
            // 读取失败时保持空清单, 不阻断游戏
        }
        return result;
    }

    public static boolean isItemBlacklisted(ResourceLocation id)
    {
        return itemBlacklist.contains(id);
    }

    public static boolean isItemWhitelisted(ResourceLocation id)
    {
        return itemWhitelist.contains(id);
    }

    public static boolean isDimensionBlacklisted(ResourceLocation id)
    {
        return dimensionBlacklist.contains(id);
    }

    public static boolean isDimensionWhitelisted(ResourceLocation id)
    {
        return dimensionWhitelist.contains(id);
    }
}
