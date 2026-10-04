package com.frnc.better_experience.chunkdevourer;

import com.frnc.better_experience.BetterExperienceServerConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * 区块吞噬者的删除队列: 把一个"删掉这 N×N 个区块"的任务摊到多个 tick 里做完。
 *
 * <p><strong>为什么必须分批</strong>: 3 级一次要删 5×5 = 25 个区块, 约 86 万格非空气方块,
 * 一个 tick 里做完等于把服务端按住十几秒。所以照 {@code CleanupHandler} 那套思路,
 * 每 tick 只删满 {@code chunkDevourerBlocksPerTick} 格就让出主线程: 1 级约几 tick,
 * 3 级按默认配额约 10 秒。
 *
 * <p><strong>遍历顺序: 自上而下</strong>。区块 (行优先) → section 由高到低 → section 内的
 * {@code index} 由大到小, 合起来就是每根柱子严格从顶往下删。方向是刻意的:
 * {@code LevelChunk#setBlockState} 每改一格都要更新 4 个高度图与天光光源, 两者都有
 * "变更在表层以下就直接返回"的早退, 而被删的正好是表层那一格时会向下扫描找下一个不透明方块。
 * 自上而下时下面那格还在, 扫描第一步就命中, 每格 O(1); 自下而上则要一路扫到世界底部,
 * 每列每张高度图各一次, 等级 3 就是几百万次多余的调色板读。
 *
 * <p>每个 section 先问一句 {@link LevelChunkSection#hasOnlyAir()}, 是空气就一次跳掉 4096 格 ——
 * 地表以上的 section 基本都空, 这一条是本功能能跑得动的关键。
 *
 * <p><strong>不缓存 {@code LevelChunk}</strong>: 每 tick 都用
 * {@code ServerChunkCache#getChunkNow} 现取。区块中途卸载时再取就是 null, 那一块直接跳过,
 * 不会拿着失效对象往里写。也因此不会为了够不到的区块去同步加载 / 生成地形 —— 那才是真的卡服。
 *
 * <p><strong>一个维度一条队列</strong>, 队首任务跑完才轮到下一个。宁可让第二个玩家多等几秒,
 * 也不让两份删除负载叠在同一个 tick 上。
 */
public final class ChunkDevourerQueue
{
    private static final Map<ResourceKey<Level>, Deque<Job>> QUEUES = new HashMap<>();

    private ChunkDevourerQueue()
    {
    }

    /**
     * 把"删掉这片区块"排进队列。
     *
     * <p>范围、保留柱子的位置、快捷栏槽位、镐子快照全都在<strong>触发瞬间</strong>由 {@link Job}
     * 从玩家身上一次取齐: 之后玩家跑多远、切到哪一格、镐子还在不在, 都不影响这一次已经定好的范围与代价。
     */
    public static void enqueue(int enchantLevel, ServerPlayer player, BlockPos origin)
    {
        int radius = ChunkDevourerRules.chunkRadiusOf(enchantLevel);

        QUEUES.computeIfAbsent(player.serverLevel().dimension(), key -> new ArrayDeque<>())
                .add(new Job(player, new ChunkPos(origin), radius));
    }

    /** 这名玩家是否还有没跑完的任务 (同一个玩家不叠第二个任务) */
    public static boolean isBusy(ServerPlayer player)
    {
        Deque<Job> jobs = QUEUES.get(player.serverLevel().dimension());
        if (jobs == null) return false;

        for (Job job : jobs)
        {
            if (job.playerId.equals(player.getUUID())) return true;
        }
        return false;
    }

    /** 丢掉所有还没跑完的任务 (总开关关闭时由处理器调用, 让在跑的任务立刻收手) */
    public static void clearAll()
    {
        if (!QUEUES.isEmpty()) QUEUES.clear();
    }

    /**
     * 每 tick 推进每个维度队首的那个任务。
     *
     * <p>维度已经取不到 (卸载 / 换存档) 时整条清掉, 免得静态表里越积越多。
     */
    public static void tick(MinecraftServer server)
    {
        if (QUEUES.isEmpty()) return;

        Iterator<Map.Entry<ResourceKey<Level>, Deque<Job>>> iterator = QUEUES.entrySet().iterator();
        while (iterator.hasNext())
        {
            Map.Entry<ResourceKey<Level>, Deque<Job>> entry = iterator.next();
            Deque<Job> jobs = entry.getValue();
            ServerLevel level = server.getLevel(entry.getKey());

            if (level == null || jobs.isEmpty())
            {
                iterator.remove();
                continue;
            }

            Job job = jobs.peek();
            job.process(level);
            if (job.done) jobs.poll();
        }
    }

    /** 一次"删掉一片区块"的任务 */
    private static final class Job
    {
        private final UUID playerId;

        /**
         * 保留柱子的 x / z: 触发瞬间玩家所站的那一列, 整列从底到顶都不删, 玩家才有落脚点。
         * 是否真的保留由 {@code chunkDevourerKeepPlayerColumn} 决定 (关掉就是字面意义的"整个区块")。
         */
        private final int keepX;
        private final int keepZ;

        /** 触发瞬间的镐子快照: 工具代价会把真镐子损毁, 掉落判定要用这份还带着附魔的副本 */
        private final ItemStack dropTool;

        /**
         * 触发瞬间主手所在的快捷栏槽位, 以及那一格里装的是什么物品。
         *
         * <p>工具代价要等本任务第一次被处理时才结算 (原因见 {@code settleToolCost}), 而那时已经过了一个 tick,
         * 玩家完全可能已经切了快捷栏: 服务端一个 tick 内的顺序是
         * {@code onPreServerTick -> tickChildren(处理所有网络包) -> onPostServerTick},
         * 于是"破坏方块"与"切快捷栏"两个包会在同一次 {@code tickChildren} 里先后处理完,
         * 等轮到我们结算时 {@code getMainHandItem()} 可能已经是另一格的东西了。
         * 照着它扣, 轻则扣错物品, 重则把玩家刚切出来的东西直接销毁; 反过来也留下一个
         * "挖完立刻滚轮切格"就能赖掉代价的口子。所以结算时按<em>槽位</em>取物品, 并核对类型是否还是当初那把。
         */
        private final int toolSlot;
        private final Item toolItem;

        private final int minChunkX;
        private final int maxChunkX;
        private final int minChunkZ;
        private final int maxChunkZ;

        /** 游标: 当前区块 → section (由低到高) → section 内的位置下标 */
        private int chunkX;
        private int chunkZ;
        private int sectionY;
        private int index;

        private boolean started;
        /** 游标是否停在某个还没扫完的区块上 */
        private boolean hasChunk;
        private boolean toolCostSettled;
        private boolean done;

        /** 把这次任务需要的一切都从触发瞬间的玩家身上取齐, 之后再有人动玩家的状态也与本次无关 */
        Job(ServerPlayer player, ChunkPos center, int radius)
        {
            this.playerId = player.getUUID();

            BlockPos keep = player.blockPosition();
            this.keepX = keep.getX();
            this.keepZ = keep.getZ();

            this.toolSlot = player.getInventory().selected;
            ItemStack held = player.getMainHandItem();
            this.toolItem = held.getItem();
            this.dropTool = held.copy();

            this.minChunkX = center.x - radius;
            this.maxChunkX = center.x + radius;
            this.minChunkZ = center.z - radius;
            this.maxChunkZ = center.z + radius;
        }

        /** 推进任务, 直到删满本 tick 的配额或整片删完 */
        void process(ServerLevel level)
        {
            // 每 tick 只查一次触发者: 掉落判定每一格都要用到他, 而按 UUID 查玩家是哈希查找,
            // 等级 3 一次 86 万格就是 86 万次, 没必要
            ServerPlayer actor = player(level);
            settleToolCost(actor);

            int budget = Math.max(1, BetterExperienceServerConfig.chunkDevourerBlocksPerTick);
            int removed = 0;

            while (removed < budget && !this.done)
            {
                if (!this.hasChunk && !this.openNextChunk(level))
                {
                    this.done = true;
                    return;
                }

                if (this.sectionY < level.getMinSection())
                {
                    // 这个区块的 section 扫完了, 换下一个区块
                    this.hasChunk = false;
                    continue;
                }

                LevelChunk chunk = level.getChunkSource().getChunkNow(this.chunkX, this.chunkZ);
                if (chunk == null)
                {
                    // 区块没加载: 整块跳过。玩家周围的区块本来就在加载范围内, 只有贴着加载边界挖才会碰到。
                    this.hasChunk = false;
                    continue;
                }

                LevelChunkSection section = chunk.getSections()[level.getSectionIndexFromSectionY(this.sectionY)];
                if (section == null || section.hasOnlyAir())
                {
                    this.sectionY--;
                    this.index = LevelChunkSection.SECTION_SIZE - 1;
                    continue;
                }

                removed += clearSection(level, chunk, section, budget - removed, actor);
            }
        }

        /** 扫完这一节剩下的位置 (index 由大到小 = 自上而下), 返回本次删掉的格数 */
        private int clearSection(ServerLevel level, LevelChunk chunk, LevelChunkSection section, int remaining,
                ServerPlayer actor)
        {
            int removed = 0;
            int baseX = this.chunkX << 4;
            int baseY = this.sectionY * 16;
            int baseZ = this.chunkZ << 4;

            while (this.index >= 0 && removed < remaining)
            {
                int i = this.index--;
                int localX = i & 15;
                int localZ = (i >> 4) & 15;
                int localY = i >> 8;

                int worldX = baseX + localX;
                int worldZ = baseZ + localZ;
                // 保留脚下那一列: 整列从底到顶都留着, 玩家才有落脚点
                if (BetterExperienceServerConfig.chunkDevourerKeepPlayerColumn
                        && worldX == this.keepX && worldZ == this.keepZ)
                {
                    continue;
                }

                BlockState state = section.getBlockState(localX, localY, localZ);
                if (state.isAir()) continue;

                removeBlock(level, chunk, new BlockPos(worldX, baseY + localY, worldZ), state, actor);
                removed++;
            }

            if (this.index < 0)
            {
                // 本节扫完, 往下一层 (更低) 走
                this.sectionY--;
                this.index = LevelChunkSection.SECTION_SIZE - 1;
            }

            return removed;
        }

        /**
         * 删掉一格。
         *
         * <p>删除本身走 {@code Level#setBlock(..., Block.UPDATE_CLIENTS)} —— 就是 {@code /fill ... air}
         * 的做法: 高度图、光照、方块实体、POI 全交给原版收拾, 客户端那边 {@code ChunkHolder} 还会把同一
         * section 的变更攒成批量包下发, 不会一格里一个包。代价是<strong>不触发邻接更新</strong>,
         * 与水 {@code /fill} 一致 —— 水不会流进来, 悬空的方块也不会掉。
         *
         * <p>掉落另算: 原版 {@code Block#playerDestroy} 内部就是调带工具的 {@code dropResources},
         * 这里照抄同一个调用, 所以时运 / 精准采集 / 经验的表现和徒手挖完全一致。
         * 注意顺序 —— 先把方块实体取出来再删方块, 删完就取不到了。
         *
         * <p>{@code actor} 可能是 null (触发者中途退出了): 掉落照旧, 只是少了"是谁挖的"这层上下文。
         */
        private void removeBlock(ServerLevel level, LevelChunk chunk, BlockPos pos, BlockState state,
                ServerPlayer actor)
        {
            if (BetterExperienceServerConfig.chunkDevourerDropsItems)
            {
                BlockEntity blockEntity = state.hasBlockEntity() ? chunk.getBlockEntity(pos) : null;
                Block.dropResources(state, level, pos, blockEntity, actor, this.dropTool, true);
            }

            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }

        /**
         * 结算工具代价, 整个任务只结算一次。两个枚举值的含义见 {@link ChunkDevourerToolCost}。
         *
         * <p><strong>为什么拖到队列第一次处理时才扣, 而不是在事件里当场扣</strong>:
         * {@code BlockEvent.BreakEvent} 的触发点在 {@code ServerPlayerGameMode#destroyBlock} 的<em>开头</em>,
         * 那时原版还没执行 {@code itemstack.mineBlock(...)} 与掉落结算。若当场把镐子打成空,
         * 原版接着会以空手走完剩下的流程, 结果是中心那个方块被删掉却不掉落。
         *
         * <p><strong>创造模式不付出代价</strong>: 原版 {@code ItemStack#hurtAndBreak} 自己会跳过
         * {@code instabuild} 的玩家, 但"损毁"是绕开它直接改物品栈的 —— 不自己挡一道, 创造模式里就会把
         * 镐子打到只剩 1 点耐久, 又因为 {@code hurtAndBreak} 被跳过而不会碎。
         *
         * <p><strong>{@code DESTROY_TOOL} 无视耐久附魔</strong>: 耐久附魔的豁免判定在
         * {@code ItemStack#hurt} 里 ({@code nextInt(level + 1) > 0}), 走那条路的话耐久 III 有 3/4 的概率
         * 豁免掉那点损耗, 镐子不碎而是停在只剩 1 点耐久, 那就不叫"一把镐换一次"了。
         * 所以这里直接照抄原版损毁物品的那串动作 (广播损坏 -> 数量减一 -> 记统计), 不经过 {@code hurt}。
         */
        private void settleToolCost(ServerPlayer actor)
        {
            if (this.toolCostSettled) return;
            this.toolCostSettled = true;
            if (actor == null || actor.getAbilities().instabuild) return;

            // 按触发瞬间的槽位取, 并核对类型: 玩家可能已经切了格, 或把这把镐挪去了别处 (见 toolSlot 的注释)
            ItemStack tool = actor.getInventory().getItem(this.toolSlot);
            if (tool.isEmpty() || tool.getItem() != this.toolItem || !tool.isDamageableItem()) return;

            if (BetterExperienceServerConfig.chunkDevourerToolCost == ChunkDevourerToolCost.SINGLE_DURABILITY)
            {
                tool.hurtAndBreak(1, actor, broken -> broken.broadcastBreakEvent(EquipmentSlot.MAINHAND));
                return;
            }

            actor.broadcastBreakEvent(EquipmentSlot.MAINHAND);
            Item item = tool.getItem();
            tool.shrink(1);
            actor.awardStat(Stats.ITEM_BROKEN.get(item));
        }

        /** 触发者; 中途退出游戏时为 null */
        private ServerPlayer player(ServerLevel level)
        {
            return level.getServer().getPlayerList().getPlayer(this.playerId);
        }

        /** 游标移到下一个区块; 全部区块都扫过时返回 false */
        private boolean openNextChunk(ServerLevel level)
        {
            if (!this.started)
            {
                this.started = true;
                this.chunkX = this.minChunkX;
                this.chunkZ = this.minChunkZ;
            }
            else if (++this.chunkX > this.maxChunkX)
            {
                this.chunkX = this.minChunkX;
                if (++this.chunkZ > this.maxChunkZ) return false;
            }

            // 自上而下: 从最高那一节开始, index 也从该节的最后一个位置开始
            this.sectionY = level.getMaxSection() - 1;
            this.index = LevelChunkSection.SECTION_SIZE - 1;
            this.hasChunk = true;
            return true;
        }
    }
}
