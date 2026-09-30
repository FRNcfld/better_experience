package com.frnc.better_experience.gamma;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;

import net.minecraft.client.OptionInstance;

import java.util.Optional;

/**
 * 亮度 (伽马) 的取值范围 0.0–10.0, 即 0%–1000% (来源: GJEB 的 {@code OptionsMixin$1})。
 *
 * <p>原版用 {@link OptionInstance.UnitDouble} 把亮度限制在 0.0–1.0 (0%–100%), 这里换成自定义的
 * {@code SliderableValueSet}, 上限放宽到 10.0。滑块位置是<strong>线性</strong>映射
 * ({@code value / 10.0}), 所以原版 0–100% 那段位于滑块的左侧 1/10。
 *
 * <p><strong>取值范围恒定, 与 {@link ExtendedGamma} 的开关无关</strong>: 开关关闭时滑块照样能拖到
 * 1000%、照样把值存进 options.txt, 只是那个值不参与渲染 (由 {@link ExtendedGamma#effectiveGamma}
 * 在渲染读取处决定)。这样开关来回切不会影响玩家调好的值。
 *
 * <p>{@link #codec()} 与 {@link OptionInstance.UnitDouble} 的写法保持一致 (只是把范围换成 0–10),
 * 用 {@code either(doubleRange, BOOL)} 兼容早期版本写在 options.txt 里的布尔亮度值
 * ({@code true} 视作满亮度), 否则读旧配置会解析失败。
 *
 * <p><strong>注意</strong>: {@code OptionInstance.SliderableValueSet} 在原版里是包级私有,
 * 本类实现它依赖 {@code META-INF/accesstransformer.cfg} 里的 AT (来源模组也是这么做的)。
 */
public final class ExtendedGammaValueSet implements OptionInstance.SliderableValueSet<Double>
{
    /** 上限 10.0 即 1000%, 与来源模组一致 */
    public static final double MAX_GAMMA = 10.0D;

    private static final ExtendedGammaValueSet INSTANCE = new ExtendedGammaValueSet();

    private ExtendedGammaValueSet()
    {
    }

    public static ExtendedGammaValueSet instance()
    {
        return INSTANCE;
    }

    @Override
    public Optional<Double> validateValue(Double value)
    {
        return value >= 0.0D && value <= MAX_GAMMA ? Optional.of(value) : Optional.empty();
    }

    @Override
    public double toSliderValue(Double value)
    {
        return value / MAX_GAMMA;
    }

    @Override
    public Double fromSliderValue(double sliderValue)
    {
        return sliderValue * MAX_GAMMA;
    }

    @Override
    public Codec<Double> codec()
    {
        return Codec.either(Codec.doubleRange(0.0D, MAX_GAMMA), Codec.BOOL).xmap(
                either -> either.map(value -> value, enabled -> enabled ? 1.0D : 0.0D),
                Either::left);
    }
}
