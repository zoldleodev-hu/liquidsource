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

import com.hollingsworth.arsnouveau.ArsNouveau;
import com.hollingsworth.arsnouveau.api.source.AbstractSourceMachine;
import com.hollingsworth.arsnouveau.common.block.tile.SourceJarTile;
import com.hollingsworth.arsnouveau.common.items.data.BlockFillContents;
import com.hollingsworth.arsnouveau.setup.registry.BlockRegistry;
import com.hollingsworth.arsnouveau.setup.registry.DataComponentRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.EmptyFluid;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

@EventBusSubscriber
@Mod(LiquidSource.MODID)
public class LiquidSource {
    public static final String MODID = "liquidsource";
    private static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(BuiltInRegistries.FLUID, MODID);
    private static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, MODID);
    public static final DeferredHolder<Fluid, Fluid> SOURCE = FLUIDS.register("source", () -> new EmptyFluid() {
        @Override
        public @NotNull FluidType getFluidType() {
            return SOURCE_TYPE.get();
        }
    });
    public static final DeferredHolder<FluidType, FluidType> SOURCE_TYPE = FLUID_TYPES.register("source", () -> new FluidType(FluidType.Properties.create()) {
        @SuppressWarnings("all")
        @Override
        public void initializeClient(@NotNull Consumer<IClientFluidTypeExtensions> consumer) {
            consumer.accept(new IClientFluidTypeExtensions() {
                @Override
                public int getTintColor() {
                    return IClientFluidTypeExtensions.super.getTintColor();
                }

                @Override
                public ResourceLocation getStillTexture() {
                    return ResourceLocation.fromNamespaceAndPath(ArsNouveau.MODID, "block/mana_still");
                }
            });
        }
    });

    public LiquidSource(IEventBus bus, ModContainer container) {
        FLUIDS.register(bus);
        FLUID_TYPES.register(bus);
        bus.addListener(this::registerCapabilities);
        container.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    public void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlock(
                Capabilities.FluidHandler.BLOCK,
                (level, pos, state, entity, dir) -> {
                    if (Compat.isMEJar(entity))
                        return Compat.getMESourceHandler(entity);
                    if (Compat.isAdditionsJar(entity))
                        return Compat.getEnderSourceHandler(entity);
                    if (entity instanceof AbstractSourceMachine tile)
                        return new LiquidSourceHandler(tile::getSourceStorage);
                    return null;
                },
                Config.liquidSourceBlocks.toArray(new Block[0]));
    }

    @SubscribeEvent
    public static void fillJar(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel().getBlockEntity(event.getPos()) instanceof SourceJarTile jar))
            return;

        ItemStack stack = event.getItemStack();
        Player player = event.getEntity();
        if (!(stack.is(BlockRegistry.SOURCE_JAR.get().asItem())
                || stack.is(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("allthemodium", "allthemodium_source_jar"))))
                || player.isSpectator() || player.isShiftKeyDown() || stack.isEmpty())
            return;

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);

        if (!(player instanceof ServerPlayer))
            return;

        int mana = BlockFillContents.get(stack);
        mana -= jar.addSource(mana, false);

        if (stack.getCount() == 1) {
            stack.set(DataComponentRegistry.BLOCK_FILL_CONTENTS, mana == 0 ? null : new BlockFillContents(mana));
            return;
        }

        ItemStack single = stack.copyWithCount(1);
        single.set(DataComponentRegistry.BLOCK_FILL_CONTENTS, mana == 0 ? null : new BlockFillContents(mana));
        if (player.addItem(single))
            stack.shrink(1);
    }
}