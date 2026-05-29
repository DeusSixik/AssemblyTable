package dev.sixik.assemblytable.utils;

import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import com.blamejared.crafttweaker_annotations.annotations.Document;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Ingredient;
import org.openzen.zencode.java.ZenCodeType;

@ZenRegister
@ZenCodeType.Name("assemblytable.api.utils.SizedIngredient")
//@Document("mods/assemblytable/utils/SizedIngredient")
public record SizedIngredient(@ZenCodeType.Getter Ingredient ingredient, @ZenCodeType.Getter int count) {

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
