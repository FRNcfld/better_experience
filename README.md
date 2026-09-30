# Better Experience

面向 Minecraft **1.20.1 / Forge** 的原版体验优化模组。不依赖任何第三方前置（Sodium / Embeddium 为可选兼容，未安装也能正常运行）。

## 环境要求

| 项目 | 版本 |
|---|---|
| Minecraft | 1.20.1 |
| Forge | 47.x（`loaderVersion="[47,)"`） |
| Java | 17 |

## 功能

| 功能 | 说明 | 生效位置 |
|---|---|---|
| **二段跳** | 空中再按一次跳跃键，可再获得一次跳跃；二段跳后的摔落伤害削减 30% | 客户端施加速度，服务端权威 |
| **鞘翅飞行开关** | 关闭时无法起飞（拦截原版 `tryToStartFallFlying`，换客户端绕不过去） | 双端 |
| **掉落物定时清理** | 定时清理掉落物；支持维度 / 物品黑白名单，以及命名物品、新鲜掉落、玩家死亡掉落的保护 | 服务端 |
| **附魔金苹果强化** | 生命恢复 V 级 60 秒、抗性提升 III 级、伤害吸收 V 级、营养 10 / 饱和度满 | 双端（全局） |
| **平坦基岩** | 主世界底部与下界顶/底只生成 1 层基岩，取代原版的 1–5 层锯齿状 | 服务端（世界生成） |
| **亮度扩展** | 亮度（伽马）滑块上限由原版 100% 放宽到 **1000%**；含 Sodium / Embeddium 兼容 | 纯客户端 |
| **上坡辅助** | 可直接走上整格方块；按键在「关闭 / 上坡 / 自动跳跃」三档间循环，行走·潜行·疾跑三档高度可调 | 纯客户端 |
| **饱和度机制** | 饱和度不再被饥饿值封顶；饥饿满时进食，多出的营养按比例（默认 1:1，可配）转成饱和度；可无限进食；HUD 显示饱和度与消耗度 | 服务端权威 |
| **望远镜改进** | 望远镜放在**饰品栏**（Curios，可选）或背包里也能按键使用；滚轮调整放大（默认最大 10 倍，可配）并**记住**设置；开镜时显示准星与放大倍数 | 纯客户端 |

## 热键

> ⚠️ **所有热键默认均不绑定**，需要自行设置。位置：**选项 → 控制 → 按键绑定 → 更好的体验**。

| 热键 | 作用 |
|---|---|
| 切换二段跳开关 | 开 / 关自己的二段跳 |
| 切换鞘翅飞行开关 | 开 / 关自己的鞘翅起飞 |
| 循环上坡辅助模式 | 关闭 → 上坡 → 自动跳跃 → 关闭 … |
| 亮度扩展开关 | 开关 1000% 亮度（关闭时按原版亮度渲染） |
| 使用望远镜 | **按住**即以望远镜视角观察（无需手持，饰品栏/背包里有即可） |

提示统一显示在**动作栏**（物品栏上方），不刷屏聊天栏。

## 配置

配置文件有两份（首次启动后生成）：

- `config/better_experience-common.toml` —— 服务端 / 世界机制（掉落物清理、饱和度、上坡辅助、平坦基岩等）
- `config/better_experience-client.toml` —— 纯客户端偏好（望远镜的缩放倍数、准星、覆盖层样式等）。其中 `spyglassZoom` 是**由游戏写回**的：滚轮调完缩放后会记住，下次启动直接恢复

**每一项功能都有独立的总开关，默认全部开启。** 总开关关闭时该项功能被整体禁用，玩家在游戏内**无法**重新打开。此外还有若干数值可调，例如：

- `stepAssistMode` / `stepAssistStepHeight` / `stepAssistSneakHeight` / `stepAssistSprintHeight` — 上坡辅助的初始档位与三档高度
- `cleanupIntervalSeconds` / `cleanupWarningSeconds` — 掉落物清理的间隔与预警时间
- `cleanupItemBlacklistEnable` 等四个开关 — 黑白名单是否生效，名单本身在数据包里（`data/better_experience/dropped_item_cleanup/`）

> 本模组的配置是 **COMMON** 类型，双端各读各的文件、**不做同步**。因此双端 / 服务端功能的开关以**服务端**的配置为准，纯客户端功能（亮度扩展、上坡辅助）以你自己客户端的配置为准。多人服务器上，二段跳与鞘翅的开关状态由服务端下发，你本地改配置不会生效。

## 注意事项

- **平坦基岩只影响新生成的区块。** 已经生成过的区块不会改变。
- **亮度扩展**：滑块任何时候都能拖到 1000%，但功能关闭时按原版亮度渲染（即"能拖，不生效"）。设置值会被记住，开关来回切不会影响它。
- **上坡辅助是纯客户端功能**，不改服务端。服务端那份你的上坡高度仍是原版 0.6，所以把高度设得过大时，服务端可能拒绝你的位移、把你拉回去。多人游戏请谨慎调高。
- **附魔金苹果强化**的数值目前是硬编码的，只有总开关可配。
- 如果你使用**食物信息类模组**（AppleSkin 等），在客户端与服务端配置不一致时，它们显示的会是客户端那份数值，而实际生效的以服务端为准。

## 构建

```bash
./gradlew build       # 产物在 build/libs/
./gradlew runClient   # 开发环境启动客户端
./gradlew runServer   # 开发环境启动服务端
```

首次构建会下载并反编译 Minecraft，耗时较长；后续增量构建约十几秒。

> 本项目启用了 Access Transformer（`src/main/resources/META-INF/accesstransformer.cfg`），用于把原版包级私有的 `OptionInstance$ValueSet` / `OptionInstance$SliderableValueSet` 公开化，以便实现自定义的亮度取值集合。修改该文件时注意：**保持纯 ASCII，且不要出现只有 `#` 的空注释行**——那会被解析成一条空规则并直接中断构建。

## 来源与致谢

本模组的若干功能灵感来自以下模组，在此致谢：

| 功能 | 来源 | 许可证 |
|---|---|---|
| 上坡辅助 | [Accessible Step](https://github.com/SecretOnline/accessible-step)（secret_online） | MPL-2.0 |
| 平坦基岩 | [Flat Bedrock](https://www.curseforge.com/minecraft/mc-mods/flat-bedrock) | MIT |
| 亮度扩展 | [GJEB (GammaJustExtremeBright)](https://github.com/MC-U-Team/GJEB-GammaJustExtremeBright)（HyCraftHD / Team U-Team） | Apache-2.0 |
| 附魔金苹果强化 | 附魔金苹果重生（enchanted_golden_apple_reborn） | — |
| 饱和度机制 | [Saturation Plus](https://www.curseforge.com/minecraft/mc-mods/saturation-plus)（MrKirbychu） | CC0-1.0 |
| 望远镜改进 | [Spyglass Improvements](https://github.com/juancarloscp52/spyglass-improvements)（juancarloscp52） | GPL-3.0 |

移植时依照本模组的既有约定做了重写（配置改为 Forge 配置项、提示改为动作栏、热键默认不绑定等），并非原样搬运。

本仓库**不包含**上述模组的源码、二进制或美术素材：参考用的解包产物只作本地查阅，已列入 `.gitignore`。素材授权不一致的尤其没有复制——例如望远镜原模组的 `Clear` / `Circle` 两种覆盖层样式各带一张 GPL 授权的 PNG，这里只保留了复用原版贴图的 `DEFAULT` 与不画覆盖层的 `NONE`（见 `SpyglassOverlayStyle`）。

## 许可证

[MIT](LICENSE)
