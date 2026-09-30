package com.frnc.better_experience.spyglass;

/**
 * 开镜时望远镜覆盖层的样式。
 *
 * <p>原模组还有 {@code Clear} 与 {@code Circle} 两种, 各自带一张它自带的 PNG。那两张图是
 * <strong>GPL 授权素材, 未复制</strong>, 因此这里只保留两种:
 * <ul>
 *   <li>{@link #DEFAULT} —— 复用<strong>原版</strong>的 {@code minecraft:textures/misc/spyglass_scope.png};</li>
 *   <li>{@link #NONE} —— 完全不画覆盖层。</li>
 * </ul>
 */
public enum SpyglassOverlayStyle
{
    DEFAULT,
    NONE
}
