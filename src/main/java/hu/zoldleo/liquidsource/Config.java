//  This file is part of Liquid Source.
//  Copyright (C) 2026 ZoldLeo
//
//  This library is free software: you can redistribute it and/or
//  modify it under the terms of the GNU Lesser General Public
//  License as published by the Free Software Foundation; either
//  version 3 of the License, or (at your option) any later version.
//
//  This library is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
//  Lesser General Public License for more details.
//
//  You should have received a copy of the GNU Lesser General Public
//  License along with this library. If not, see https://www.gnu.org/licenses/lgpl-3.0.html;.
//
//  zoldleo.dev@gmail.com

package hu.zoldleo.liquidsource;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings("deprecation")
@EventBusSubscriber
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    private static final ModConfigSpec.ConfigValue<List<? extends String>> LIQUID_SOURCE_BLOCKS = BUILDER.comment("A list of all the block that source can be pumped out of").defineListAllowEmpty("liquidSourceBlocks", List.of("ars_nouveau:source_jar", "ars_nouveau:creative_source_jar", "endersourcejars:endersourcejar", "ars_additions:ender_source_jar", "allthemodium:allthemodium_source_jar", "arseng:me_source_jar"), (obj) -> true);
    public static final ModConfigSpec SPEC = BUILDER.build();

    public static Set<Block> liquidSourceBlocks;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        liquidSourceBlocks = LIQUID_SOURCE_BLOCKS.get().stream().map(name -> BuiltInRegistries.BLOCK.get(ResourceLocation.parse(name))).collect(Collectors.toSet());
    }
}