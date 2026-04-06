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

import com.hollingsworth.arsnouveau.api.ANFakePlayer;
import com.hollingsworth.arsnouveau.api.item.inv.ExtractedStack;
import com.hollingsworth.arsnouveau.api.item.inv.InventoryManager;
import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.AbstractEffect;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.SpellSchool;
import com.hollingsworth.arsnouveau.api.spell.SpellSchools;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.TileCaster;
import com.hollingsworth.arsnouveau.api.util.SpellUtil;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAOE;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAmplify;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentDampen;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentExtract;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentPierce;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentRandomize;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentSensitive;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import static hu.zoldleo.liquidsource.LiquidSource.MODID;

public class TransportFluidGlyph extends AbstractEffect {
    public static final TransportFluidGlyph INSTANCE = new TransportFluidGlyph();

    private TransportFluidGlyph() {
        super(ResourceLocation.fromNamespaceAndPath(MODID, "glyph_transport_fluid"), "Transport Fluid");
    }

    @Override
    public void onResolveBlock(BlockHitResult rayTraceResult, Level world, @NotNull LivingEntity shooter, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver) {
        List<BlockPos> posList = SpellUtil.calcAOEBlocks(shooter, rayTraceResult.getBlockPos(), rayTraceResult, spellStats);
        Player fakePlayer = ANFakePlayer.getPlayer((ServerLevel)world, (shooter instanceof Player player) ? player.getUUID() : null);

        for(BlockPos pos : posList) {
            if (!world.isInWorldBounds(pos))
                continue;

            pos = rayTraceResult.isInside() ? pos : pos.relative(rayTraceResult.getDirection());
            if (!world.getBlockState(pos).canBeReplaced())
                continue;

            BlockEvent.EntityPlaceEvent event = NeoForge.EVENT_BUS.post(new BlockEvent.EntityPlaceEvent(BlockSnapshot.create(world.dimension(), world, pos), world.getBlockState(pos), fakePlayer));
            if (event.isCanceled())
                continue;

            place(new BlockHitResult(new Vec3(pos.getX(), pos.getY(), pos.getZ()), rayTraceResult.getDirection(), pos, false), world, shooter, spellStats, spellContext, fakePlayer);
        }
    }

    @Override
    public void onResolveEntity(EntityHitResult rayTraceResult, Level world, @NotNull LivingEntity shooter, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver) {
        Entity entity = rayTraceResult.getEntity();
        onResolveBlock(new BlockHitResult(entity.position(), Direction.DOWN, entity.blockPosition(), true), world, shooter, spellStats, spellContext, resolver);
    }

    public void place(BlockHitResult resolveResult, Level level, @NotNull LivingEntity shooter, SpellStats spellStats, SpellContext spellContext, Player fakePlayer) {
        TileCaster turret = spellContext.getCaster() instanceof TileCaster tile ? tile : null;
        InventoryManager inventory = spellContext.getCaster().getInvManager();
        BlockPos placePos = resolveResult.getBlockPos();
        BlockPos hitPos = placePos.relative(resolveResult.getDirection().getOpposite());
        IFluidHandler targetTank = level.getCapability(Capabilities.FluidHandler.BLOCK, hitPos, resolveResult.getDirection());

        if (!spellStats.hasBuff(AugmentExtract.INSTANCE)) {
            if (turret != null) {
                if (fromTankToTank(level, hitPos, turret.getTile().getBlockPos(), targetTank, spellStats.getAmpMultiplier()))
                    return;
                if (!spellStats.isSensitive()) {
                    if (fromTankToBlock(level, hitPos, turret.getTile().getBlockPos()))
                        return;
                    if (fromTankToGround(level, resolveResult, turret.getTile().getBlockPos()))
                        return;
                }
            }
            if (fromInventoryToTank(level, hitPos, inventory, targetTank, spellStats.isRandomized(), shooter, fakePlayer, spellStats.getAmpMultiplier()))
                return;
            if (!spellStats.isSensitive()) {
                if (fromInventoryToBlock(level, hitPos, inventory, spellStats.isRandomized(), shooter, fakePlayer))
                    return;
                if (fromInventoryToGround(level, resolveResult, inventory, spellStats.isRandomized(), shooter, fakePlayer))
                    return;
            }
            if (!spellStats.isSensitive() && turret != null) {
                if (fromGroundOrBlockToTank(level, hitPos, turret.getTile().getBlockPos(), targetTank))
                    return;
                if (fromGroundOrBlockToBlock(level, hitPos, turret.getTile().getBlockPos()))
                    return;
                if (fromGroundOrBlockToGround(level, resolveResult, turret.getTile().getBlockPos()))
                    return;
            }
            return;
        }

        if (!spellStats.isSensitive()) {
            if (turret != null) {
                if (fromGroundOrBlockToTankReverse(level, hitPos, turret.getTile().getBlockPos()))
                    return;
                if (fromGroundOrBlockToTankReverse(level, placePos, turret.getTile().getBlockPos()))
                    return;
            }
            if (fromGroundOrBlockToInventoryReverse(level, hitPos, inventory, spellStats.isRandomized(), shooter, fakePlayer))
                return;
            if (fromGroundOrBlockToInventoryReverse(level, placePos, inventory, spellStats.isRandomized(), shooter, fakePlayer))
                return;
        }
        if (turret != null && fromTankToTankReverse(level, hitPos, turret.getTile().getBlockPos(), targetTank, spellStats.getAmpMultiplier()))
            return;
        fromTankToInventoryReverse(level, hitPos, inventory, spellStats.isRandomized(), targetTank, shooter, fakePlayer, spellStats.getAmpMultiplier());
    }

    @SuppressWarnings("all")
    protected boolean fromTankToTank(Level level, BlockPos hitPos, BlockPos turretPos, IFluidHandler targetTank, double amp) {
        if (targetTank == null)
            return false;
        int toDrain = amp < 1 ? (int)(1000 * Math.pow(2, amp)) : (int)(1000 * (amp + 1));
        boolean drained = false;
        for(Direction dir : Direction.values()) {
            BlockPos relativePos = turretPos.relative(dir);
            IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, relativePos, level.getBlockState(relativePos), null, dir.getOpposite());
            if (handler == null)
                continue;
            FluidStack simulatedStack = handler.drain(toDrain, IFluidHandler.FluidAction.SIMULATE);
            if (simulatedStack.isEmpty())
                continue;
            int filled = targetTank.fill(simulatedStack, IFluidHandler.FluidAction.EXECUTE);
            if (filled <= 0)
                continue;
            handler.drain(filled, IFluidHandler.FluidAction.EXECUTE);
            playEmptySound(simulatedStack.getFluid(), level, hitPos);
            toDrain -= filled;
            drained = true;
        }
        return drained;
    }

    @SuppressWarnings("all")
    protected boolean fromTankToBlock(Level level, BlockPos hitPos, BlockPos turretPos) {
        if (!(level.getBlockState(hitPos).getBlock() instanceof LiquidBlockContainer target))
            return false;
        final int toDrainForPlacement = 1000;
        ArrayList<FluidStack> drainedStacks = new ArrayList<>();
        ArrayList<IFluidHandler> drainFrom = new ArrayList<>();
        sides:
        for(Direction dir : Direction.values()) {
            BlockPos relativePos = turretPos.relative(dir);
            IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, relativePos, level.getBlockState(relativePos), null, dir.getOpposite());
            if (handler == null)
                continue;
            FluidStack simulatedStack = handler.drain(toDrainForPlacement, IFluidHandler.FluidAction.SIMULATE);
            if (simulatedStack.isEmpty())
                continue;
            for (FluidStack drainedStack : drainedStacks) {
                if (FluidStack.isSameFluidSameComponents(simulatedStack, drainedStack)) {
                    drainedStack.grow(simulatedStack.getAmount());
                    drainFrom.add(handler);
                    continue sides;
                }
            }
            drainedStacks.add(simulatedStack);
            drainFrom.add(handler);
        }
        for (FluidStack drainedStack : drainedStacks) {
            if (drainedStack.getAmount() < toDrainForPlacement)
                continue;
            if (!target.canPlaceLiquid(null, level, hitPos, level.getBlockState(hitPos), drainedStack.getFluid()))
                continue;
            int toDrain = toDrainForPlacement;
            for(IFluidHandler handler : drainFrom)
                toDrain -= handler.drain(drainedStack.copyWithAmount(toDrain), IFluidHandler.FluidAction.EXECUTE).getAmount();
            target.placeLiquid(level, hitPos, level.getBlockState(hitPos), drainedStack.getFluid().defaultFluidState());
            playEmptySound(drainedStack.getFluid(), level, hitPos);
            return true;
        }
        return false;
    }

    @SuppressWarnings("all")
    protected boolean fromTankToGround(Level level, BlockHitResult result, BlockPos turretPos) {
        final int toDrainForPlacement = 1000;
        ArrayList<FluidStack> drainedStacks = new ArrayList<>();
        ArrayList<IFluidHandler> drainFrom = new ArrayList<>();
        sides:
        for(Direction dir : Direction.values()) {
            BlockPos relativePos = turretPos.relative(dir);
            IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, relativePos, level.getBlockState(relativePos), null, dir.getOpposite());
            if (handler == null)
                continue;
            FluidStack simulatedStack = handler.drain(toDrainForPlacement, IFluidHandler.FluidAction.SIMULATE);
            if (simulatedStack.isEmpty())
                continue;
            for (FluidStack drainedStack : drainedStacks) {
                if (FluidStack.isSameFluidSameComponents(simulatedStack, drainedStack)) {
                    drainedStack.grow(simulatedStack.getAmount());
                    drainFrom.add(handler);
                    continue sides;
                }
            }
            drainedStacks.add(simulatedStack);
            drainFrom.add(handler);
        }
        for (FluidStack drainedStack : drainedStacks) {
            if (drainedStack.getAmount() < toDrainForPlacement)
                continue;
            if (attemptPlace(level, drainedStack.getFluid(), result) == InteractionResult.FAIL)
                continue;
            int toDrain = toDrainForPlacement;
            for(IFluidHandler handler : drainFrom)
                toDrain -= handler.drain(drainedStack.copyWithAmount(toDrain), IFluidHandler.FluidAction.EXECUTE).getAmount();
            playEmptySound(drainedStack.getFluid(), level, result.getBlockPos());
            return true;
        }
        return false;
    }

    protected boolean fromInventoryToTank(Level level, BlockPos hitPos, InventoryManager inventory, IFluidHandler targetTank, boolean randomized, @NotNull LivingEntity shooter, Player fakePlayer, double amp) {
        if (targetTank == null)
            return false;
        boolean filled = false;
        Fluid fillFluid = Fluids.EMPTY;
        for (int i = 0; i <= (int)amp; i++) {
            Fluid fluid = fillFluid;
            Predicate<ItemStack> predicate = stack -> !stack.isEmpty() && stack.getItem() instanceof BucketItem bucket && (fluid == Fluids.EMPTY || fluid == bucket.content) &&  targetTank.fill(new FluidStack(bucket.content, 1000), IFluidHandler.FluidAction.SIMULATE) >= 1000;
            ExtractedStack extractItem = randomized ? inventory.extractRandomItem(predicate, 1) : inventory.extractItem(predicate, 1);
            if (extractItem.isEmpty())
                continue;
            fillFluid = ((BucketItem)extractItem.getStack().getItem()).content;
            targetTank.fill(new FluidStack(fillFluid, 1000), IFluidHandler.FluidAction.EXECUTE);
            extractItem.replaceAndReturnOrDrop(ItemUtils.createFilledResult(extractItem.getStack(), fakePlayer, BucketItem.getEmptySuccessItem(extractItem.getStack(), fakePlayer)), level, shooter.getOnPos());
            filled = true;
        }
        if (filled) {
            playEmptySound(fillFluid, level, hitPos);
            return true;
        }
        return false;
    }

    protected boolean fromInventoryToBlock(Level level, BlockPos hitPos, InventoryManager inventory, boolean randomized, @NotNull LivingEntity shooter, Player fakePlayer) {
        if (!(level.getBlockState(hitPos).getBlock() instanceof LiquidBlockContainer target))
            return false;
        Predicate<ItemStack> predicate = stack -> !stack.isEmpty() && stack.getItem() instanceof BucketItem bucket && target.canPlaceLiquid(null, level, hitPos, level.getBlockState(hitPos), bucket.content);
        ExtractedStack extractItem = randomized ? inventory.extractRandomItem(predicate, 1) : inventory.extractItem(predicate, 1);
        if (extractItem.isEmpty())
            return false;
        Fluid fluid = ((BucketItem)extractItem.getStack().getItem()).content;
        if (target.placeLiquid(level, hitPos, level.getBlockState(hitPos), fluid.defaultFluidState())) {
            extractItem.replaceAndReturnOrDrop(ItemUtils.createFilledResult(extractItem.getStack(), fakePlayer, BucketItem.getEmptySuccessItem(extractItem.getStack(), fakePlayer)), level, shooter.getOnPos());
            playEmptySound(fluid, level, hitPos);
            return true;
        }
        extractItem.returnOrDrop(level, shooter.getOnPos());
        return false;
    }

    protected boolean fromInventoryToGround(Level level, BlockHitResult result, InventoryManager inventory, boolean randomized, @NotNull LivingEntity shooter, Player fakePlayer) {
        Predicate<ItemStack> predicate = stack -> !stack.isEmpty() && stack.getItem() instanceof BucketItem bucket && bucket.content instanceof FlowingFluid;
        ExtractedStack extractItem = randomized ? inventory.extractRandomItem(predicate, 1) : inventory.extractItem(predicate, 1);
        if (extractItem.isEmpty())
            return false;
        BucketItem bucket = (BucketItem)extractItem.getStack().getItem();
        if (attemptPlace(level, extractItem.getStack(), bucket, result, fakePlayer) == InteractionResult.FAIL) {
            extractItem.returnOrDrop(level, shooter.getOnPos());
            return false;
        }
        playEmptySound(bucket.content, level, result.getBlockPos());
        extractItem.replaceAndReturnOrDrop(ItemUtils.createFilledResult(extractItem.getStack(), fakePlayer, BucketItem.getEmptySuccessItem(extractItem.getStack(), fakePlayer)), level, shooter.getOnPos());
        return true;
    }

    protected boolean fromGroundOrBlockToTank(Level level, BlockPos hitPos, BlockPos turretPos, IFluidHandler targetTank) {
        if (targetTank == null)
            return false;
        for(Direction dir : Direction.values()) {
            BlockPos relativePos = turretPos.relative(dir);
            BlockState state = level.getBlockState(relativePos);
            Block block = state.getBlock();
            if (block instanceof LiquidBlock liquid) {
                if (targetTank.fill(new FluidStack(liquid.fluid, 1000), IFluidHandler.FluidAction.SIMULATE) < 1000)
                    continue;
                if (liquid.pickupBlock(null, level, relativePos, state).isEmpty())
                    continue;
                targetTank.fill(new FluidStack(liquid.fluid, 1000), IFluidHandler.FluidAction.EXECUTE);
                playEmptySound(liquid.fluid, level, hitPos);
                return true;
            }
            if (block instanceof SimpleWaterloggedBlock water) {
                if (targetTank.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.SIMULATE) >= 1000 && state.getValue(BlockStateProperties.WATERLOGGED)) {
                    targetTank.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
                    water.pickupBlock(null, level, relativePos, state);
                    playEmptySound(Fluids.WATER, level, hitPos);
                    return true;
                }
            }
        }
        return false;
    }

    protected boolean fromGroundOrBlockToBlock(Level level, BlockPos hitPos, BlockPos turretPos) {
        BlockState hitState = level.getBlockState(hitPos);
        if (!(hitState.getBlock() instanceof LiquidBlockContainer target))
            return false;
        for(Direction dir : Direction.values()) {
            BlockPos relativePos = turretPos.relative(dir);
            BlockState state = level.getBlockState(relativePos);
            Block block = state.getBlock();
            if (block instanceof LiquidBlock liquid) {
                if (!target.canPlaceLiquid(null, level, hitPos, hitState, liquid.fluid))
                    continue;
                if (liquid.pickupBlock(null, level, relativePos, state).isEmpty())
                    continue;
                target.placeLiquid(level, hitPos, hitState, liquid.fluid.getSource(false));
                playEmptySound(liquid.fluid, level, hitPos);
                return true;
            }
            if (block instanceof SimpleWaterloggedBlock water) {
                if (target.canPlaceLiquid(null, level, hitPos, hitState, Fluids.WATER) && state.getValue(BlockStateProperties.WATERLOGGED)) {
                    target.placeLiquid(level, hitPos, hitState, Fluids.WATER.getSource(false));
                    water.pickupBlock(null, level, relativePos, state);
                    playEmptySound(Fluids.WATER, level, hitPos);
                    return true;
                }
            }
        }
        return false;
    }

    protected boolean fromGroundOrBlockToGround(Level level, BlockHitResult result, BlockPos turretPos) {
        for(Direction dir : Direction.values()) {
            BlockPos relativePos = turretPos.relative(dir);
            BlockState state = level.getBlockState(relativePos);
            Block block = state.getBlock();
            if (block instanceof LiquidBlock liquid) {
                if (liquid.pickupBlock(null, level, relativePos, state).isEmpty())
                    continue;
                if (attemptPlace(level, liquid.fluid, result) == InteractionResult.FAIL)
                    continue;
                playEmptySound(liquid.fluid, level, result.getBlockPos());
                return true;
            }
            if (block instanceof SimpleWaterloggedBlock water) {
                if (state.getValue(BlockStateProperties.WATERLOGGED) && attemptPlace(level, Fluids.WATER, result) != InteractionResult.FAIL) {
                    water.pickupBlock(null, level, relativePos, state);
                    playEmptySound(Fluids.WATER, level, result.getBlockPos());
                    return true;
                }
            }
        }
        return false;
    }

    protected boolean fromGroundOrBlockToTankReverse(Level level, BlockPos hitPos, BlockPos turretPos) {
        BlockState state = level.getBlockState(hitPos);
        Block block = state.getBlock();
        if (block instanceof LiquidBlock liquid) {
            for(Direction dir : Direction.values()) {
                BlockPos relativePos = turretPos.relative(dir);
                IFluidHandler targetTank = level.getCapability(Capabilities.FluidHandler.BLOCK, relativePos, dir.getOpposite());
                if (targetTank == null)
                    continue;
                if (targetTank.fill(new FluidStack(liquid.fluid.getSource(), 1000), IFluidHandler.FluidAction.SIMULATE) < 1000)
                    continue;
                if (liquid.pickupBlock(null, level, hitPos, state).isEmpty())
                    continue;
                targetTank.fill(new FluidStack(liquid.fluid.getSource(), 1000), IFluidHandler.FluidAction.EXECUTE);
                playEmptySound(liquid.fluid, level, hitPos);
                return true;
            }
        }
        if (block instanceof SimpleWaterloggedBlock water && state.getValue(BlockStateProperties.WATERLOGGED)) {
            for(Direction dir : Direction.values()) {
                BlockPos relativePos = turretPos.relative(dir);
                IFluidHandler targetTank = level.getCapability(Capabilities.FluidHandler.BLOCK, relativePos, dir.getOpposite());
                if (targetTank == null)
                    continue;
                if (targetTank.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.SIMULATE) >= 1000) {
                    targetTank.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
                    water.pickupBlock(null, level, hitPos, state);
                    playEmptySound(Fluids.WATER, level, relativePos);
                    return true;
                }
            }
        }
        return false;
    }

    @SuppressWarnings("all")
    protected boolean fromGroundOrBlockToInventoryReverse(Level level, BlockPos hitPos, InventoryManager inventory, boolean randomized, @NotNull LivingEntity shooter, Player fakePlayer) {
        BlockState state = level.getBlockState(hitPos);
        if (!(state.getBlock() instanceof BucketPickup pickup))
            return false;
        Predicate<ItemStack> predicate = (stack -> !stack.isEmpty() && stack.getItem() instanceof BucketItem bucket && bucket.content == Fluids.EMPTY);
        ExtractedStack extractItem = randomized ? inventory.extractRandomItem(predicate, 1) : inventory.extractItem(predicate, 1);
        if (extractItem.isEmpty())
            return false;
        ItemStack filledStack = pickup.pickupBlock(fakePlayer, level, hitPos, state);
        if (filledStack.isEmpty()) {
            extractItem.returnOrDrop(level, shooter.getOnPos());
            return false;
        }
        extractItem.replaceAndReturnOrDrop(filledStack, level, shooter.getOnPos());
        if (filledStack.getItem() instanceof BucketItem bucket)
            playFillSound(bucket.content, level, hitPos);
        return true;
    }

    protected boolean fromTankToTankReverse(Level level, BlockPos hitPos, BlockPos turretPos, IFluidHandler tank, double amp) {
        if (tank == null)
            return false;
        FluidStack simulatedStack = tank.drain(amp < 1 ? (int)(1000 * Math.pow(2, amp)) : (int)(1000 * (amp + 1)), IFluidHandler.FluidAction.SIMULATE);
        if (simulatedStack.isEmpty())
            return false;
        Fluid fluid = simulatedStack.getFluid();
        boolean drained = false;
        for(Direction dir : Direction.values()) {
            BlockPos relativePos = turretPos.relative(dir);
            IFluidHandler targetTank = level.getCapability(Capabilities.FluidHandler.BLOCK, relativePos, level.getBlockState(relativePos), null, dir.getOpposite());
            if (targetTank == null)
                continue;
            int filled = targetTank.fill(simulatedStack, IFluidHandler.FluidAction.EXECUTE);
            if (filled <= 0)
                continue;
            tank.drain(filled, IFluidHandler.FluidAction.EXECUTE);
            simulatedStack.shrink(filled);
            drained = true;
            if (simulatedStack.isEmpty())
                break;
        }
        if (drained) {
            playFillSound(fluid, level, hitPos);
            return true;
        }
        return false;
    }

    @SuppressWarnings("all")
    protected boolean fromTankToInventoryReverse(Level level, BlockPos hitPos, InventoryManager inventory, boolean randomized, IFluidHandler tank, @NotNull LivingEntity shooter, Player fakePlayer, double amp) {
        if (tank == null)
            return false;
        Fluid fluid = Fluids.EMPTY;
        for (int i = 0; i <= (int)amp; i++) {
            FluidStack simulatedStack = fluid == Fluids.EMPTY ? tank.drain(1000, IFluidHandler.FluidAction.SIMULATE) : tank.drain(new FluidStack(fluid,1000), IFluidHandler.FluidAction.SIMULATE);
            if (simulatedStack.isEmpty() || simulatedStack.getAmount() < 1000)
                break;
            Predicate<ItemStack> predicate = (stack -> !stack.isEmpty() && stack.getItem() instanceof BucketItem bucket && bucket.content == Fluids.EMPTY);
            ExtractedStack extractItem = randomized ? inventory.extractRandomItem(predicate, 1) : inventory.extractItem(predicate, 1);
            if (extractItem.isEmpty())
                break;
            tank.drain(1000, IFluidHandler.FluidAction.EXECUTE);
            extractItem.replaceAndReturnOrDrop(ItemUtils.createFilledResult(extractItem.getStack(), fakePlayer, new ItemStack(simulatedStack.getFluid().getBucket())), level, shooter.getOnPos());
            fluid = simulatedStack.getFluid();
        }
        if (fluid == Fluids.EMPTY)
            return false;
        playFillSound(fluid, level, hitPos);
        return true;
    }

    public static InteractionResult attemptPlace(Level world, ItemStack stack, BucketItem item, BlockHitResult result, Player fakePlayer) {
        fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, stack);
        if (item.emptyContents(fakePlayer, world, result.getBlockPos(), result, stack)) {
            item.checkExtraContent(fakePlayer, world, stack, result.getBlockPos());
            return InteractionResult.sidedSuccess(world.isClientSide());
        }
        return InteractionResult.FAIL;
    }

    public static InteractionResult attemptPlace(Level world, Fluid fluid, BlockHitResult result) {
        if (fluid.getBucket() instanceof BucketItem bucket && bucket.emptyContents(null, world, result.getBlockPos(), result, null))
            return InteractionResult.sidedSuccess(world.isClientSide());
        return  InteractionResult.FAIL;
    }

    @Override
    public int getDefaultManaCost() {
        return 10;
    } // probably fine? (10)

    @Override
    public @NotNull Set<AbstractAugment> getCompatibleAugments() {
        return augmentSetOf(AugmentAOE.INSTANCE, AugmentPierce.INSTANCE, AugmentRandomize.INSTANCE, AugmentSensitive.INSTANCE, AugmentExtract.INSTANCE, AugmentAmplify.INSTANCE, AugmentDampen.INSTANCE);
    }

    @Override
    protected void addDefaultAugmentLimits(Map<ResourceLocation, Integer> defaults) {
        defaults.put(AugmentSensitive.INSTANCE.getRegistryName(), 1);
        defaults.put(AugmentExtract.INSTANCE.getRegistryName(), 1);
        defaults.put(AugmentRandomize.INSTANCE.getRegistryName(), 1);
    }

    @Override
    public String getBookDescription() {
        return "Places fluids from the casters inventory. If cast by a player, this spell will place fluids from the hot bar first. Extract causes the fluid to be picked up instead, and Sensitive will stop the spell from picking up or placing fluid as blocks.";
    }

    @Override
    public @NotNull Set<SpellSchool> getSchools() {
        return setOf(SpellSchools.MANIPULATION);
    }

    protected static void playEmptySound(Fluid fluid, Level level, BlockPos pos) {
        SoundEvent sound = fluid.getFluidType().getSound(SoundActions.BUCKET_EMPTY);
        if (sound != null)
            level.playSound(null, pos, sound, SoundSource.BLOCKS);
    }

    protected static void playFillSound(Fluid fluid, Level level, BlockPos pos) {
        SoundEvent sound = fluid.getFluidType().getSound(SoundActions.BUCKET_FILL);
        if (sound != null)
            level.playSound(null, pos, sound, SoundSource.BLOCKS);
    }
}