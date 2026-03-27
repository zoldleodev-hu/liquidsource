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

package hu.zoldleo.liquidsource.mixin;

import gripe._90.arseng.block.entity.MESourceJarBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MESourceJarBlockEntity.class)
public interface MESourceJarBlockEntityAccessor {
    @Invoker("receiveSource")
    int liquidsource$receiveSource(int source, boolean simulate);

    @Invoker("extractSource")
    int liquidsource$extractSource(int source, boolean simulate);

    @Invoker("getSource")
    int liquidsource$getSource();

    @Invoker("getSourceCapacity")
    int liquidsource$getSourceCapacity();
}