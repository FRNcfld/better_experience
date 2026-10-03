package com.frnc.better_experience.droppeditemcleanup;

import com.frnc.better_experience.BetterExperienceServerConfig;
import com.frnc.better_experience.BetterExperience;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 死亡掉落保护 (参考 CleanMaid 的 DeathDropProtectionModule):
 *   玩家死亡时, 登记其掉落物实体的 UUID 与当前游戏刻; 在
 *   cleanupPlayerDeathDropProtectionSeconds 保护期内, 这些掉落物不会被清理。
 */
@Mod.EventBusSubscriber(modid = BetterExperience.MOD_ID)
public class DeathDropProtection
{
    /** 掉落物实体 UUID -> 登记时的游戏刻 */
    private static final Map<UUID, Long> PROTECTED_DROPS = new HashMap<>();

    /** 玩家死亡时登记其掉落物 */
    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event)
    {
        if (!BetterExperienceServerConfig.cleanupProtectPlayerDeathDrops) return;
        if (!(event.getEntity() instanceof Player player)) return;

        long gameTime = player.level().getGameTime();
        for (ItemEntity drop : event.getDrops())
        {
            PROTECTED_DROPS.put(drop.getUUID(), gameTime);
        }
    }

    /** 该掉落物是否处于死亡保护期内 */
    public static boolean isProtected(UUID entityUuid, long nowGameTime)
    {
        int seconds = BetterExperienceServerConfig.cleanupPlayerDeathDropProtectionSeconds;
        if (seconds <= 0) return false;
        Long start = PROTECTED_DROPS.get(entityUuid);
        return start != null && (nowGameTime - start) < seconds * 20L;
    }

    /** 清理已过保护期的记录, 避免 Map 无限增长 */
    public static void cleanupExpired(long nowGameTime)
    {
        int seconds = BetterExperienceServerConfig.cleanupPlayerDeathDropProtectionSeconds;
        if (seconds <= 0)
        {
            PROTECTED_DROPS.clear();
            return;
        }
        long window = seconds * 20L;
        PROTECTED_DROPS.entrySet().removeIf(e -> (nowGameTime - e.getValue()) >= window);
    }
}
