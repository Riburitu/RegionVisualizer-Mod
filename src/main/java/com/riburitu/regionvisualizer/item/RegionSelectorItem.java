// RegionSelectorItem.java

package com.riburitu.regionvisualizer.item;

import com.riburitu.regionvisualizer.util.RegionSelection;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class RegionSelectorItem extends Item {
    private static final RegionSelection selection = new RegionSelection();
    private static BlockPos lastPos1 = null; // Para evitar spam
    private static BlockPos lastPos2 = null; // Para evitar spam

    public RegionSelectorItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos clicked = context.getClickedPos();

        if (player.isShiftKeyDown()) {
            if (!clicked.equals(lastPos2)) { // Solo imprimir si pos2 cambió
                selection.setPos2(clicked);
                lastPos2 = clicked;
                player.sendSystemMessage(Component.literal("Pos2 puesta: " + clicked));
                System.out.println("[RegionVisualizer] Pos2 puesta: " + clicked);
            }
        } else {
            if (!clicked.equals(lastPos1)) { // Solo imprimir si pos1 cambió
                selection.setPos1(clicked);
                lastPos1 = clicked;
                player.sendSystemMessage(Component.literal("Pos1 puesta: " + clicked));
                System.out.println("[RegionVisualizer] Pos1 puesta: " + clicked);
            }
        }

        // Imprimir estado de selección solo si cambió
        if (!clicked.equals(lastPos1) || !clicked.equals(lastPos2)) {
            System.out.println("[RegionVisualizer] Selección actual: pos1=" + selection.pos1 + ", pos2=" + selection.pos2 + ", Selección completa=" + selection.isComplete());
        }

        return InteractionResult.SUCCESS;
    }

    public static RegionSelection getSelection() {
        return selection;
    }

    public static void clearSelection() {
        selection.setPos1(null);
        selection.setPos2(null);
        lastPos1 = null;
        lastPos2 = null;
        System.out.println("[RegionVisualizer] Selección limpiada.");
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        clearSelection();
        System.out.println("[RegionVisualizer] Selección limpiada al salir del jugador: " + event.getEntity().getName().getString());
    }
}
