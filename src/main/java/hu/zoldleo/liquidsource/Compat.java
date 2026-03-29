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

import com.hollingsworth.arsnouveau.api.source.ISourceCap;
import com.hollingsworth.arsnouveau.api.source.ISourceTile;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class Compat {
    public static final String ENERGISTIQUE = "arseng";
    public static final String ADDITIONS = "ars_additions";
    //public static final String ATM = "allthemodium";
    //public static final String ENDERJARS = "allthemodium";

    public static final ResourceLocation ADDITIONSJAR = ResourceLocation.fromNamespaceAndPath(ADDITIONS, "ender_source_jar");

    public static final int ADDITIONSMAXSOURCE = 10_000;

    private static final Map<String, Boolean> MODS = new HashMap<>();

    public static boolean isModLoaded(String id) {
        return MODS.computeIfAbsent(id, Compat::check);
    }

    private static boolean check(final String id) {
        ModList modList = ModList.get();
        return (modList != null && modList.isLoaded(id)) || LoadingModList.get().getModFileById(id) != null;
    }

    public static boolean isMEJar(BlockEntity entity) {
        return entity != null && entity.getClass().getSimpleName().equals("MESourceJarBlockEntity") && isModLoaded(ENERGISTIQUE);
    }

    public static LiquidSourceHandler getMESourceHandler(BlockEntity entity) {
        ISourceCap jar = (ISourceCap)entity;
        return new LiquidSourceHandler(null) {
            @Override
            public int getFluidAmount() {
                return jar.getSource();
            }

            @Override
            public int getCapacity() {
                return jar.getSourceCapacity() + jar.getSource(); // ArsEng gives the remaining space as capacity
            }

            @Override
            public int fill(@NotNull FluidStack fluidStack, @NotNull FluidAction fluidAction) {
                if (fluidStack.isEmpty() || !isFluidValid(0, fluidStack))
                    return 0;

                if (fluidAction.simulate())
                    return Math.min(jar.getSourceCapacity(), fluidStack.getAmount()); // ArsEng gives the remaining space as capacity

                int filled = jar.receiveSource(fluidStack.getAmount(), false);

                if (filled > 0)
                    onContentsChanged();

                return filled;
            }

            @Override
            public @NotNull FluidStack drain(int maxDrain, @NotNull FluidAction fluidAction) {
                int drained = Math.min(jar.getSource(), maxDrain);

                if (fluidAction.execute() && drained > 0) {
                    drained = jar.extractSource(drained, false);
                    onContentsChanged();
                }

                return new FluidStack(LiquidSource.SOURCE, drained);
            }

            @Override
            protected void onContentsChanged() {
            }
        };
    }

    public static boolean isAdditionsJar(BlockEntity entity) {
        return entity != null && entity.getClass().getSimpleName().equals("EnderSourceJarTile") && isModLoaded(ADDITIONS);
    }

    public static LiquidSourceHandler getAdditionsSourceHandler(BlockEntity entity) {
        ISourceTile jar = (ISourceTile)entity;
        return new LiquidSourceHandler(null) {
            @Override
            public int getFluidAmount() {
                return jar.getSource();
            }

            @Override
            public int getCapacity() {
                return jar.getMaxSource();
            }

            @Override
            public int fill(@NotNull FluidStack fluidStack, @NotNull FluidAction fluidAction) {
                if (fluidStack.isEmpty() || !isFluidValid(0, fluidStack))
                    return 0;

                int toInsert = Math.min(jar.getMaxSource() - jar.getSource(), fluidStack.getAmount());
                if (fluidAction.execute() && toInsert > 0) {
                    jar.setSource(jar.getSource() + toInsert);
                    onContentsChanged();
                }

                return toInsert;
            }

            @Override
            public @NotNull FluidStack drain(int maxDrain, @NotNull FluidAction fluidAction) {
                int drained = Math.min(jar.getSource(), maxDrain);

                if (fluidAction.execute() && drained > 0) {
                    jar.setSource(jar.getSource() - drained);
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