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

import hu.zoldleo.liquidsource.mixin.MESourceJarBlockEntityAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class Compat {
    public static final String ENERGISTIQUE = "arseng";

    private static final Map<String, Boolean> MODS = new HashMap<>();

    public static boolean isModLoaded(String id) {
        return MODS.computeIfAbsent(id, Compat::check);
    }

    private static boolean check(final String id) {
        ModList modList = ModList.get();
        return (modList != null && modList.isLoaded(id)) || LoadingModList.get().getModFileById(id) != null;
    }

    public static LiquidSourceHandler getMESourceHandler(BlockEntity entity) {
        if (entity == null || !entity.getClass().getName().equals("gripe._90.arseng.block.entity.MESourceJarBlockEntity"))
            return null;
        MESourceJarBlockEntityAccessor jar = (MESourceJarBlockEntityAccessor)entity;
        return new LiquidSourceHandler(null) {
            @Override
            public int getFluidAmount() {
                return jar.liquidsource$getSource();
            }

            @Override
            public int getCapacity() {
                return jar.liquidsource$getSourceCapacity();
            }

            @Override
            public int fill(@NotNull FluidStack fluidStack, @NotNull FluidAction fluidAction) {
                if (fluidStack.isEmpty() || !isFluidValid(0, fluidStack))
                    return 0;

                if (fluidAction.simulate())
                    return Math.min(jar.liquidsource$getSourceCapacity() - jar.liquidsource$getSource(), fluidStack.getAmount());

                int filled = jar.liquidsource$receiveSource(fluidStack.getAmount(), false);

                if (filled > 0)
                    onContentsChanged();

                return filled;
            }

            @Override
            public @NotNull FluidStack drain(int maxDrain, @NotNull FluidAction fluidAction) {
                int drained = Math.min(jar.liquidsource$getSource(), maxDrain);

                if (fluidAction.execute() && drained > 0) {
                    drained = jar.liquidsource$extractSource(drained, false);
                    onContentsChanged();
                }

                return new FluidStack(LiquidSource.SOURCE, drained);
            }

            @Override
            protected void onContentsChanged() {
            }
        };
    }
}