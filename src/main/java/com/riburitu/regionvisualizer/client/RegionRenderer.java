package com.riburitu.regionvisualizer.client;

import com.riburitu.regionvisualizer.item.RegionSelectorItem;
import com.riburitu.regionvisualizer.util.RegionSelection;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import org.joml.Matrix4f;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.lwjgl.opengl.GL11;

public class RegionRenderer {
    private static Boolean wasInside = null;
    private static RegionSelection lastSelection = null;

    public static void onRenderWorld(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        RegionSelection sel = RegionSelectorItem.getSelection();
        if (sel == null || !sel.isComplete()) return;

        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        BlockPos p1 = sel.pos1, p2 = sel.pos2;
        boolean selectionChanged = (lastSelection == null || !sel.equals(lastSelection));
        if (selectionChanged) {
            lastSelection = sel;
            System.out.println("[RegionVisualizer] Región renderizada: pos1=" + p1 + ", pos2=" + p2);
        }

        double minX = Math.min(p1.getX(), p2.getX());
        double minY = Math.min(p1.getY(), p2.getY());
        double minZ = Math.min(p1.getZ(), p2.getZ());
        double maxX = Math.max(p1.getX(), p2.getX()) + 1;
        double maxY = Math.max(p1.getY(), p2.getY()) + 1;
        double maxZ = Math.max(p1.getZ(), p2.getZ()) + 1;

        boolean inside = isPlayerInsideRegion(player, minX, minY, minZ, maxX, maxY, maxZ);
        if (wasInside == null || wasInside != inside) {
            wasInside = inside;
            player.sendSystemMessage(Component.literal("[RegionVisualizer] Estás " + (inside ? "DENTRO" : "FUERA") + " de la región"));
        }

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();

        // 🔹 Ajuste clave: mover el render según la cámara
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        poseStack.translate(-camera.getPosition().x, -camera.getPosition().y, -camera.getPosition().z);

        Matrix4f matrix = poseStack.last().pose();

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.lineWidth(3.0F);

        BufferBuilder buf = Tesselator.getInstance().getBuilder();
        buf.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

        float r = inside ? 0f : 1f;
        float g = inside ? 1f : 0f;
        float b = 0f;
        float a = 1f;

        // 🔹 Tus líneas originales, sin cambios en la estructura
        addLine(buf, matrix, minX, minY, minZ, maxX, minY, minZ, r, g, b, a);
        addLine(buf, matrix, maxX, minY, minZ, maxX, minY, maxZ, r, g, b, a);
        addLine(buf, matrix, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a);
        addLine(buf, matrix, minX, minY, maxZ, minX, minY, minZ, r, g, b, a);
        addLine(buf, matrix, minX, maxY, minZ, maxX, maxY, minZ, r, g, b, a);
        addLine(buf, matrix, maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, a);
        addLine(buf, matrix, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a);
        addLine(buf, matrix, minX, maxY, maxZ, minX, maxY, minZ, r, g, b, a);
        addLine(buf, matrix, minX, minY, minZ, minX, maxY, minZ, r, g, b, a);
        addLine(buf, matrix, maxX, minY, minZ, maxX, maxY, minZ, r, g, b, a);
        addLine(buf, matrix, maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, a);
        addLine(buf, matrix, minX, minY, maxZ, minX, maxY, maxZ, r, g, b, a);

        Tesselator.getInstance().end();

        int error = GL11.glGetError();
        if (error != GL11.GL_NO_ERROR) {
            System.out.println("[RegionVisualizer] Error OpenGL: " + error);
        }

        RenderSystem.lineWidth(1.0F);
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();

        poseStack.popPose();
    }

    private static void addLine(BufferBuilder buf, Matrix4f matrix,
                                double x1, double y1, double z1,
                                double x2, double y2, double z2,
                                float r, float g, float b, float a) {
        buf.vertex(matrix, (float)x1, (float)y1, (float)z1).color(r, g, b, a).endVertex();
        buf.vertex(matrix, (float)x2, (float)y2, (float)z2).color(r, g, b, a).endVertex();
    }

    private static boolean isPlayerInsideRegion(Player player,
                                               double minX, double minY, double minZ,
                                               double maxX, double maxY, double maxZ) {
        double px = player.getX(), py = player.getY(), pz = player.getZ();
        return px >= minX && px <= maxX
            && py >= minY && py <= maxY
            && pz >= minZ && pz <= maxZ;
    }
}

