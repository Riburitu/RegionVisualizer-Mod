// RegionVisualizer.java

package com.riburitu.regionvisualizer;

import com.riburitu.regionvisualizer.registry.ModItems;
import com.riburitu.regionvisualizer.client.RegionRenderer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import com.riburitu.regionvisualizer.commands.RegionCommands;
import com.riburitu.regionvisualizer.item.RegionSelectorItem;
import com.riburitu.regionvisualizer.util.Region;
import com.riburitu.regionvisualizer.util.RegionManager;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Mod(RegionVisualizer.MODID)
public class RegionVisualizer {
    public static final String MODID = "regionvisualizer";
    private final RegionManager regionManager = new RegionManager();
    private final RegionCommands regionCommands = new RegionCommands(regionManager);
    private final Map<ServerPlayer, String> lastRegion = new HashMap<>(); // Rastrea la última región del jugador

    public RegionVisualizer() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.ITEMS.register(modEventBus);
        modEventBus.addListener(ModItems::registerCreativeTab);

        modEventBus.addListener(this::onClientSetup);
        MinecraftForge.EVENT_BUS.register(this); // Registrar la clase para eventos
        MinecraftForge.EVENT_BUS.register(RegionSelectorItem.class); // Registrar RegionSelectorItem para eventos
        MinecraftForge.EVENT_BUS.register(this); // Registrar para PlayerTickEvent
    }

    private void onClientSetup(final FMLClientSetupEvent event) {
        MinecraftForge.EVENT_BUS.addListener(RegionRenderer::onRenderWorld);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        regionCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        ServerLevel level = event.getServer().getLevel(Level.OVERWORLD);
        if (level != null) {
            regionManager.loadRegions(level); // Cargar regiones al iniciar el servidor
            System.out.println("[RegionVisualizer] Regiones cargadas en el nivel: " + level.dimension().location());
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.player.level().isClientSide) {
            ServerPlayer player = (ServerPlayer) event.player;
            ServerLevel level = player.serverLevel();
            String currentRegion = getCurrentRegion(level, player.blockPosition());

            String lastRegionName = lastRegion.getOrDefault(player, null);
            if (currentRegion != null && !currentRegion.equals(lastRegionName)) {
                player.displayClientMessage(Component.literal(player.getName().getString() + " ha entrado en " + currentRegion), false);
                System.out.println("[RegionVisualizer] " + player.getName().getString() + " ha entrado en " + currentRegion);
                lastRegion.put(player, currentRegion);
            } else if (currentRegion == null && lastRegionName != null) {
                player.displayClientMessage(Component.literal(player.getName().getString() + " se alejo de la región."), false);
                System.out.println("[RegionVisualizer] " + player.getName().getString() + " se alejo de la región.");
                lastRegion.remove(player); // Limpia el seguimiento si sale de la región
            }
        }
    }

    private String getCurrentRegion(ServerLevel level, BlockPos pos) {
        Optional<Region> regionOpt = regionManager.getRegions().stream()
            .filter(region -> region.contains(pos))
            .findFirst();
        return regionOpt.map(Region::getName).orElse(null);
    }
}
