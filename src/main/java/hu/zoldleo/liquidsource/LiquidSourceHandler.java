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

import com.hollingsworth.arsnouveau.common.capability.SourceStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.IFluidTank;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class LiquidSourceHandler implements IFluidHandler, IFluidTank {
    private final Supplier<SourceStorage> storageGetter;

    public LiquidSourceHandler(Supplier<SourceStorage> supplier) {
        storageGetter = supplier;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public @NotNull FluidStack getFluidInTank(int i) {
        return getFluid();
    }

    @Override
    public int getTankCapacity(int i) {
        return getCapacity();
    }

    @Override
    public boolean isFluidValid(int i, @NotNull FluidStack fluidStack) {
        return isFluidValid(fluidStack);
    }

    @Override
    public @NotNull FluidStack getFluid() {
        return new FluidStack(LiquidSource.SOURCE, storageGetter.get().getSource());
    }

    @Override
    public int getFluidAmount() {
        return storageGetter.get().getSource();
    }

    @Override
    public int getCapacity() {
        return storageGetter.get().getMaxSource();
    }

    @Override
    public boolean isFluidValid(FluidStack fluidStack) {
        return fluidStack.is(LiquidSource.SOURCE);
    }

    @Override
    public int fill(@NotNull FluidStack fluidStack, @NotNull FluidAction fluidAction) {
        if (fluidStack.isEmpty() || !isFluidValid(0, fluidStack))
            return 0;

        SourceStorage storage = storageGetter.get();

        if (fluidAction.simulate())
            return Math.min(storage.getMaxSource() - storage.getSource(), fluidStack.getAmount());

        if (storage.getSource() <= 0) {
            storage.setSource(Math.min(storage.getMaxSource(), fluidStack.getAmount()));
            onContentsChanged();
            return storage.getSource();
        }

        int filled = storage.getMaxSource() - storage.getSource();
        if (fluidStack.getAmount() < filled) {
            storage.receiveSource(fluidStack.getAmount(), false);
            filled = fluidStack.getAmount();
        } else {
            storage.setSource(storage.getMaxSource());
        }

        if (filled > 0)
            onContentsChanged();

        return filled;
    }

    @Override
    public @NotNull FluidStack drain(FluidStack fluidStack, @NotNull FluidAction fluidAction) {
        return !fluidStack.isEmpty() ? drain(fluidStack.getAmount(), fluidAction) : FluidStack.EMPTY;
    }

    @Override
    public @NotNull FluidStack drain(int maxDrain, @NotNull FluidAction fluidAction) {
        SourceStorage storage = storageGetter.get();

        int drained = Math.min(storage.getSource(), maxDrain);

        FluidStack stack = new FluidStack(LiquidSource.SOURCE, drained);
        if (fluidAction.execute() && drained > 0) {
            storage.extractSource(drained, false);
            onContentsChanged();
        }

        return stack;
    }

    protected void onContentsChanged() {
        storageGetter.get().onContentsChanged();
    }
}
