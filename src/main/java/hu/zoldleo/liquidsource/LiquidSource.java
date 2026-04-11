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
import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;
import com.hollingsworth.arsnouveau.api.source.AbstractSourceMachine;
import com.hollingsworth.arsnouveau.api.source.ISourceCap;
import com.hollingsworth.arsnouveau.common.block.tile.SourceJarTile;
import com.hollingsworth.arsnouveau.setup.registry.BlockRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
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
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
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
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, MODID);

    public static final DeferredHolder<Fluid, Fluid> SOURCE = FLUIDS.register("source", () -> new EmptyFluid() {
        @Override
        public @NotNull FluidType getFluidType() {
            return SOURCE_TYPE.get();
        }

        @Override
        public @NotNull Item getBucket() {
            return SOURCE_BUCKET.get();
        }
    });
    public static final DeferredHolder<FluidType, FluidType> SOURCE_TYPE = FLUID_TYPES.register("source", () -> new FluidType(FluidType.Properties.create().sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL).sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)) {
        @SuppressWarnings("all")
        @Override
        public void initializeClient(@NotNull Consumer<IClientFluidTypeExtensions> consumer) {
            consumer.accept(new IClientFluidTypeExtensions() {
                @Override
                public ResourceLocation getStillTexture() {
                    return ResourceLocation.fromNamespaceAndPath(ArsNouveau.MODID, "block/mana_still");
                }
            });
        }
    });
    public static final DeferredHolder<Fluid, Fluid> DENSE_SOURCE = FLUIDS.register("dense_source", () -> new EmptyFluid() {
        @Override
        public @NotNull FluidType getFluidType() {
            return DENSE_SOURCE_TYPE.get();
        }

        @Override
        public @NotNull Item getBucket() {
            return DENSE_SOURCE_BUCKET.get();
        }
    });
    public static final DeferredHolder<FluidType, FluidType> DENSE_SOURCE_TYPE = FLUID_TYPES.register("dense_source", () -> new FluidType(FluidType.Properties.create().sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL).sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)) {
        @SuppressWarnings("all")
        @Override
        public void initializeClient(@NotNull Consumer<IClientFluidTypeExtensions> consumer) {
            consumer.accept(new IClientFluidTypeExtensions() {
                @Override
                public ResourceLocation getStillTexture() {
                    return ResourceLocation.fromNamespaceAndPath(MODID, "block/dense_source");
                }
            });
        }
    });
    public static final DeferredHolder<Item, BucketItem> SOURCE_BUCKET = ITEMS.register("source_bucket", () -> new BucketItem(SOURCE.get(), new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, BucketItem> DENSE_SOURCE_BUCKET = ITEMS.register("dense_source_bucket", () -> new BucketItem(DENSE_SOURCE.get(), new Item.Properties().stacksTo(1)));

    public LiquidSource(IEventBus bus, ModContainer container) {
        FLUIDS.register(bus);
        FLUID_TYPES.register(bus);
        ITEMS.register(bus);

        GlyphRegistry.registerSpell(TransportFluidGlyph.INSTANCE);

        bus.addListener(this::addCreative);
        bus.addListener(this::registerCapabilities);
        container.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(new ItemStack(SOURCE_BUCKET));
            event.accept(new ItemStack(DENSE_SOURCE_BUCKET));
        }
    }

    public void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlock(
                Capabilities.FluidHandler.BLOCK,
                (level, pos, state, entity, dir) -> {
                    if (Compat.isMEJar(entity))
                        return Compat.getMESourceHandler(entity);
                    if (Compat.isAdditionsJar(entity))
                        return Compat.getAdditionsSourceHandler(entity);
                    if (entity instanceof AbstractSourceMachine tile)
                        return new LiquidSourceHandler(tile::getSourceStorage);
                    return null;
                },
                Config.liquidSourceBlocks.toArray(new Block[0]));
    }

    @SubscribeEvent
    public static void fillJar(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        Player player = event.getEntity();

        if (player.isSpectator() || player.isShiftKeyDown() || stack.isEmpty())
            return;

        BlockEntity blockEntity = event.getLevel().getBlockEntity(event.getPos());

        if (blockEntity instanceof SourceJarTile jar) {
            if (handleCreativeJar(jar, stack, player) || handleBucket(jar, stack, player, event.getHand())) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
            }
            return;
        }

        if (Compat.isMEJar(blockEntity)) {
            ISourceCap jar = (ISourceCap)blockEntity;
            if (handleMECreativeJar(jar, stack, player) || handleMEBucket(jar, stack, player, event.getHand())) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
            }
        }
    }

    // ars_nouveau:creative_source_jar
    private static boolean handleCreativeJar(SourceJarTile jar, ItemStack stack, Player player) {
        if (!stack.is(BlockRegistry.CREATIVE_SOURCE_JAR.get().asItem()))
            return false; // Item is not a creative source jar, don't cancel

        if (!(player instanceof ServerPlayer))
            return true; // Client-side, cancel

        if (jar.getType() == BuiltInRegistries.BLOCK_ENTITY_TYPE.get(Compat.ADDITIONSJAR)) {
            jar.setSource(Compat.ADDITIONSMAXSOURCE);
            return true; // Additions jar handled, cancel
        }

        jar.addSource(1_000_000, false); // Creative jar capacity is 1m

        player.level().playSound(null, player.getX(), player.getY() + 0.5, player.getZ(), SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);

        return true; // Single item handled, cancel
    }

    // minecraft:bucket, any source or dense source bucket
    @SuppressWarnings("all")
    private static boolean handleBucket(SourceJarTile jar, ItemStack stack, Player player, InteractionHand hand) {
        if (stack.is(Items.BUCKET)) {
            if (!(player instanceof ServerPlayer))
                return true; // Client-side, cancel

            if (jar.getType() == BuiltInRegistries.BLOCK_ENTITY_TYPE.get(Compat.ADDITIONSJAR) && (jar.getSource() >= 1000) || player.hasInfiniteMaterials())
                jar.setSource(jar.getSource() - 1000); // Additions jar
            else if (jar.removeSource(1000, true) == 1000 || player.hasInfiniteMaterials())
                jar.removeSource(1000, false); // "Normal jars"
            else
                return true; // Couldn't extract source, still cancel

            player.level().playSound(null, player.getX(), player.getY() + 0.5, player.getZ(), SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);

            if (player.hasInfiniteMaterials())
                return true;

            if (stack.getCount() == 1) {
                player.setItemInHand(hand, new ItemStack(SOURCE_BUCKET));
                return true; // Single item handled, cancel
            }

            stack.shrink(1);
            player.addItem(new ItemStack(SOURCE_BUCKET));

            return true; // Multiple items handled, cancel
        }

        if (stack.getItem() instanceof BucketItem bucket && (bucket.content.isSame(SOURCE.get()) || bucket.content.isSame(DENSE_SOURCE.get()))) {
            if (!(player instanceof ServerPlayer))
                return true; // Client-side, cancel

            int toFill = bucket.content.isSame(DENSE_SOURCE.get()) ? 10_000 : 1000;

            if (jar.getType() == BuiltInRegistries.BLOCK_ENTITY_TYPE.get(Compat.ADDITIONSJAR) && (jar.getSource() <= Compat.ADDITIONSMAXSOURCE - toFill || player.hasInfiniteMaterials()))
                jar.setSource(jar.getSource() + toFill); // Additions jar
            else if (jar.addSource(toFill, true) == toFill || player.hasInfiniteMaterials())
                jar.addSource(toFill, false); // "Normal jars"
            else
                return true; // Couldn't insert source, still cancel

            player.level().playSound(null, player.getX(), player.getY() + 0.5, player.getZ(), SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);

            if (player.hasInfiniteMaterials())
                return true;

            if (stack.getCount() == 1) {
                player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                return true; // Single item handled, cancel
            }

            stack.shrink(1);
            player.addItem(new ItemStack(Items.BUCKET));

            return true; // Multiple item handled, cancel
        }

        return false; // Item is not a bucket, don't cancel
    }

    // ars_nouveau:creative_source_jar
    private static boolean handleMECreativeJar(ISourceCap jar, ItemStack stack, Player player) {
        if (!stack.is(BlockRegistry.CREATIVE_SOURCE_JAR.get().asItem()))
            return false; // Item is not a creative source jar, don't cancel

        if (!(player instanceof ServerPlayer))
            return true; // Client-side, cancel

        jar.receiveSource(1_000_000, false); // Creative jar capacity is 1m

        player.level().playSound(null, player.getX(), player.getY() + 0.5, player.getZ(), SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);

        return true; // Single item handled, cancel
    }

    // minecraft:bucket, liquidsourcejars:source_bucket
    @SuppressWarnings("all")
    private static boolean handleMEBucket(ISourceCap jar, ItemStack stack, Player player, InteractionHand hand) {
        if (stack.is(Items.BUCKET)) {
            if (!(player instanceof ServerPlayer))
                return true; // Client-side, cancel

            if (jar.extractSource(1000, true) != 1000)
                return true; // Couldn't extract source, still cancel
            jar.extractSource(1000, false);

            player.level().playSound(null, player.getX(), player.getY() + 0.5, player.getZ(), SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);

            if (player.hasInfiniteMaterials())
                return true;

            if (stack.getCount() == 1) {
                player.setItemInHand(hand, new ItemStack(SOURCE_BUCKET));
                return true; // Single item handled, cancel
            }

            stack.shrink(1); // Not considering creative mode
            player.addItem(new ItemStack(SOURCE_BUCKET));

            return true; // Multiple items handled, cancel
        }

        if (stack.is(SOURCE_BUCKET)) {
            if (!(player instanceof ServerPlayer))
                return true; // Client-side, cancel

            if (jar.receiveSource(1000, true) != 1000)
                return true; // Couldn't insert source, still cancel
            jar.receiveSource(1000, false);

            player.level().playSound(null, player.getX(), player.getY() + 0.5, player.getZ(), SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);

            if (player.hasInfiniteMaterials())
                return true;

            if (stack.getCount() == 1) {
                player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                return true; // Single item handled, cancel
            }

            stack.shrink(1); // Not considering creative mode
            player.addItem(new ItemStack(Items.BUCKET));

            return true; // Multiple item handled, cancel
        }

        return false; // Item is not a bucket, don't cancel
    }
}