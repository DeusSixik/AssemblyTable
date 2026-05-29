package dev.sixik.assemblytable.utils;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Ingredient;

public record SizedIngredient(Ingredient ingredient, int count) {

    public static final Codec<SizedIngredient> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(SizedIngredient::ingredient),
            Codec.INT.optionalFieldOf("count", 1).forGetter(SizedIngredient::count) // По умолчанию 1, если не указано
    ).apply(inst, SizedIngredient::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SizedIngredient> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, SizedIngredient::ingredient,
            ByteBufCodecs.VAR_INT, SizedIngredient::count,
            SizedIngredient::new
    );
}
