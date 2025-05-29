package com.parkertenbroeck.remotestorage;


import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.*;
//import net.minecraft.world.phys.Vec3;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.debug.DebugRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
//import pro.mikey.fabric.xray.ScanController;
//import pro.mikey.fabric.xray.records.BlockPosWithColor;
//import pro.mikey.fabric.xray.storage.SettingsStore;

import java.util.OptionalInt;
import java.util.concurrent.atomic.AtomicBoolean;


public class OutlineRenderer {
    public static synchronized void render(WorldRenderContext context) {
//        var pos = new BlockPos(1,2,3);
//        DebugRenderer.drawBox(context.matrixStack(), context.consumers(), new BlockPos(1,2,3), new BlockPos(1,2,3), 1.0f, 1.0f, 1.0f, 0.5f);
//        DebugRenderer.drawBox();
//        Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
//        if (camera.isReady()) {
//            Vec3d vec3d = camera.getPos().negate();
//            Box box = new Box(pos).offset(vec3d).expand(1);
//            VertexRendering.drawOutline(context.matrixStack(), context.consumers().getBuffer(RenderLayer.getDebugFilledBox()), box, 1.0f, 1.0f, 1.0f, 1.0f);
//
////            drawBox(matrices, vertexConsumers, box, red, green, blue, alpha);
//        }
    }
}
