package com.frnc.better_experience.stepassist;

/**
 * 上坡辅助的三种模式 (来源: Accessible Step 的 {@code StepMode})。
 *
 * <p>已裁掉来源模组的 id / 序列化体系——那套是为它自己的 JSON 配置和自定义设置界面服务的;
 * 本模组改用 Forge 配置项 + 按键切换, 不需要它们。
 *
 * <ul>
 *   <li>{@link #OFF}: 完全原版。</li>
 *   <li>{@link #STEP}: 按潜行 / 疾跑 / 行走分别用配置里的三档高度平滑上坡。</li>
 *   <li>{@link #AUTO_JUMP}: 交回原版自动跳跃 (即原版「自动跳跃」选项被打开)。</li>
 * </ul>
 *
 * <p><strong>三种模式互斥</strong>: 选中 {@link #OFF} 或 {@link #STEP} 时会把原版自动跳跃关掉。
 * 这是来源模组的既有设计, 不是 bug。
 */
public enum StepAssistMode
{
    OFF("off"),
    STEP("step"),
    AUTO_JUMP("auto_jump");

    private static final String MESSAGE_KEY_PREFIX = "message.better_experience.step_assist.mode.";
    private static final StepAssistMode[] VALUES = values();

    private final String messageKeySuffix;

    StepAssistMode(String messageKeySuffix)
    {
        this.messageKeySuffix = messageKeySuffix;
    }

    /** 动作栏提示用的翻译键 */
    public String messageKey()
    {
        return MESSAGE_KEY_PREFIX + this.messageKeySuffix;
    }

    /** 按键切换时循环到下一个模式 */
    public StepAssistMode next()
    {
        return VALUES[(this.ordinal() + 1) % VALUES.length];
    }
}
