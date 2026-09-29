package com.frnc.better_experience;

import com.frnc.better_experience.droppeditemcleanup.CleanupLists;
import com.mojang.logging.LogUtils;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/**
 * 服务端数据包重载时读取本模组的数据包配置文件并写入内存。
 * 通过 AddReloadListenerEvent 注册: 服务端启动与世界 /reload 时都会执行。
 * 目前加载:
 *   - 掉落物清理黑白名单 (data/better_experience/dropped_item_cleanup/blacklist.json / whitelist.json)
 * prepare 阶段在后台线程解析, apply 阶段在主线程提交, 保证静态数据的内存可见性。
 */
@Mod.EventBusSubscriber(modid = BetterExperience.MOD_ID)
public class BetterExperienceDataPackReloadListener
{
    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void onAddReloadListener(final AddReloadListenerEvent event)
    {
        event.addListener(new SimplePreparableReloadListener<CleanupLists.LoadedLists>()
        {
            @Override
            protected CleanupLists.LoadedLists prepare(ResourceManager resourceManager, ProfilerFiller profiler)
            {
                return CleanupLists.loadFrom(resourceManager);
            }

            @Override
            protected void apply(CleanupLists.LoadedLists data, ResourceManager resourceManager, ProfilerFiller profiler)
            {
                CleanupLists.apply(data);
                LOGGER.info("[better_experience] 数据包配置已加载: 清理黑名单 {} 物品/{} 维度, 白名单 {} 物品/{} 维度",
                        data.itemBlacklist().size(),
                        data.dimensionBlacklist().size(),
                        data.itemWhitelist().size(),
                        data.dimensionWhitelist().size());
            }
        });
    }
}
