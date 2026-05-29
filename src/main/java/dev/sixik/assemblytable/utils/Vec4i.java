package dev.sixik.assemblytable.utils;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record Vec4i(int x, int y, int z, int w) {

    public static Codec<Vec4i> CODEC = RecordCodecBuilder.create(instance -> instance
            .group(
                    Codec.INT.fieldOf("x").forGetter(Vec4i::x),
                    Codec.INT.fieldOf("y").forGetter(Vec4i::y),
                    Codec.INT.fieldOf("z").forGetter(Vec4i::z),
                    Codec.INT.fieldOf("w").forGetter(Vec4i::w)
            ).apply(instance, Vec4i::new));
}
