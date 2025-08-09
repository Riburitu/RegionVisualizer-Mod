package com.riburitu.regionvisualizer.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.riburitu.regionvisualizer.util.Region;
import com.riburitu.regionvisualizer.util.RegionManager;
import com.riburitu.regionvisualizer.item.RegionSelectorItem;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Optional;

public class RegionCommands {
    private final RegionManager regionManager;

    public RegionCommands(RegionManager regionManager) {
        this.regionManager = regionManager;
    }

    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("region")
                .then(Commands.literal("add")
                    .then(Commands.argument("name", StringArgumentType.word())
                        .executes(ctx -> {
                            CommandSourceStack source = ctx.getSource();
                            ServerPlayer player = source.getPlayerOrException();

                            BlockPos pos1 = RegionSelectorItem.getSelection().pos1;
                            BlockPos pos2 = RegionSelectorItem.getSelection().pos2;
                            if (pos1 == null || pos2 == null) {
                                source.sendFailure(Component.literal("Debes seleccionar dos posiciones con el selector."));
                                return 0;
                            }
                            String name = StringArgumentType.getString(ctx, "name");

                            Region region = new Region(name, pos1, pos2);
                            regionManager.addRegion(region);

                            ServerLevel level = player.serverLevel();
                            regionManager.saveRegions(level);

                            source.sendSuccess(() -> Component.literal("Región '" + name + "' guardada."), true);
                            return 1;
                        })))
                .then(Commands.literal("remove")
                    .then(Commands.argument("name", StringArgumentType.word())
                        .executes(ctx -> {
                            CommandSourceStack source = ctx.getSource();
                            String name = StringArgumentType.getString(ctx, "name");

                            boolean exists = regionManager.getRegions().stream()
                                .anyMatch(r -> r.getName().equalsIgnoreCase(name));

                            if (!exists) {
                                source.sendFailure(Component.literal("No se encontró la región '" + name + "'."));
                                return 0;
                            }

                            regionManager.removeRegion(name);

                            ServerLevel serverLevel = source.getLevel();
                            regionManager.saveRegions(serverLevel);

                            source.sendSuccess(() -> Component.literal("Región '" + name + "' eliminada."), true);
                            return 1;
                        })))
                .then(Commands.literal("list")
                    .executes(ctx -> {
                        CommandSourceStack source = ctx.getSource();
                        List<Region> regions = regionManager.getRegions();
                        if (regions.isEmpty()) {
                            source.sendSuccess(() -> Component.literal("No hay regiones guardadas."), false);
                        } else {
                            source.sendSuccess(() -> Component.literal("Regiones guardadas:"), false);
                            for (Region r : regions) {
                                source.sendSuccess(() -> Component.literal("- " + r.getName()), false);
                            }
                        }
                        return 1;
                    }))
                .then(Commands.literal("tp")
                    .then(Commands.argument("name", StringArgumentType.word())
                        .executes(ctx -> {
                            CommandSourceStack source = ctx.getSource();
                            ServerPlayer player = source.getPlayerOrException();
                            String name = StringArgumentType.getString(ctx, "name");
                            Optional<Region> optRegion = regionManager.getRegions().stream()
                                .filter(r -> r.getName().equalsIgnoreCase(name))
                                .findFirst();
                            if (optRegion.isEmpty()) {
                                source.sendFailure(Component.literal("Región no encontrada."));
                                return 0;
                            }
                            Region region = optRegion.get();
                            BlockPos center = region.getCenter();
                            player.teleportTo(center.getX() + 0.5, center.getY(), center.getZ() + 0.5);
                            source.sendSuccess(() -> Component.literal("Teletransportado a la región '" + name + "'."), true);
                            return 1;
                        })))
                .then(Commands.literal("cancel")
                    .executes(ctx -> {
                        CommandSourceStack source = ctx.getSource();
                        RegionSelectorItem.clearSelection();
                        source.sendSuccess(() -> Component.literal("Selección de posiciones cancelada."), true);
                        return 1;
                    }))
        );
    }
}