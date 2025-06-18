package com.parkertenbroeck.remotestorage;


import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.*;
import com.parkertenbroeck.remotestorage.packets.s2c.StorageSystemResyncS2C;
import com.parkertenbroeck.remotestorage.system.StorageSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.*;
import net.minecraft.client.render.debug.DebugRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.inventory.Inventory;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.OptionalDouble;
import java.util.function.DoubleFunction;


@Environment(EnvType.CLIENT)
public class OutlineRenderer {

    public static final RenderPipeline.Snippet RENDERTYPE_LINES_SNIPPET = RenderPipeline.builder(RenderPipelines.MATRICES_COLOR_FOG_SNIPPET)
            .withVertexShader("core/rendertype_lines")
            .withFragmentShader("core/rendertype_lines")
            .withUniform("LineWidth", UniformType.FLOAT)
            .withUniform("ScreenSize", UniformType.VEC2)
            .withCull(false)
            .withVertexFormat(VertexFormats.POSITION_COLOR_NORMAL, VertexFormat.DrawMode.LINES)
            .buildSnippet();

    public static final RenderPipeline LINES_NO_DEPTH_PIPE = RenderPipelines.register(
            RenderPipeline.builder(RENDERTYPE_LINES_SNIPPET)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withLocation("pipeline/lines").build()
    );

    public static final RenderPipeline LINES_PIPE = RenderPipelines.register(
            RenderPipeline.builder(RENDERTYPE_LINES_SNIPPET)
                    .withLocation("pipeline/lines").build()
    );

    public static final DoubleFunction<RenderLayer.MultiPhase> LINES = thickness -> RenderLayer.of(
            "lines_mine",
            1536,
            thickness<0?LINES_NO_DEPTH_PIPE:LINES_PIPE,
            RenderLayer.MultiPhaseParameters.builder()
                    .lineWidth(new RenderPhase.LineWidth(OptionalDouble.of(Math.abs(thickness))))
                    .layering(RenderPhase.VIEW_OFFSET_Z_LAYERING_FORWARD)
                    .target(RenderPhase.TRANSLUCENT_TARGET)
                    .build(false)
    );

    public static BlockPos targetBlock(){
        if (MinecraftClient.getInstance().crosshairTarget.getType() == HitResult.Type.BLOCK) {
            return ((BlockHitResult)MinecraftClient.getInstance().crosshairTarget).getBlockPos();
        }
        return new BlockPos(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    public static synchronized void render(WorldRenderContext context, StorageSystem system) {
        var target = targetBlock();
        var inv = MinecraftClient.getInstance().player.clientWorld.getBlockEntity(target) instanceof Inventory;
        if(inv){
            DebugRenderer.drawBox(context.matrixStack(), context.consumers(), target, 0.0f, 1.0f, 1.0f, 1.0f, 0.5f);
        }

        var world = MinecraftClient.getInstance().player.getWorld().getRegistryKey().getValue();

        var members = system.unorderedMembers().stream()
                .filter(p -> getManhattanDistance(p.pos().pos(), context.camera().getPos())<80.0)
                .filter(p -> p.pos().world().equals(world))
                .toList();

        var link = RemoteStorageClient.linkTarget;
        if(link!=null){
            DebugRenderer.drawBox(context.matrixStack(), context.consumers(), link.pos(), 0.0f, 1.0f, 0.0f, 0.0f, 0.5f);
        }

        for(var member : members){
            drawBox(context.matrixStack(), context.consumers(), member.pos().pos(), 1.0f, 1.0f, member.linked().isEmpty()?1.0f:0.0f, 1.0f);

            if(member.linked().isPresent()){
                drawPathLines(context.matrixStack(), context.consumers(), List.of(member.pos().pos(), member.linked().get().pos()));
            }
        }
    }

    public static void drawBox(MatrixStack matrices, VertexConsumerProvider vertexConsumers, BlockPos pos, float red, float green, float blue, float alpha) {
        Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
        if (camera.isReady()) {
            Vec3d vec3d = camera.getPos().negate();
            Box box = (new Box(pos)).offset(vec3d);
            drawBox(matrices, vertexConsumers, box, red, green, blue, alpha);
        }
    }

    public static void drawBox(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Box box, float red, float green, float blue, float alpha) {
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(LINES.apply(3));
        VertexRendering.drawBox(matrices, vertexConsumer, box, red, green, blue, alpha);
    }

    private static float getManhattanDistance(BlockPos pos, Vec3d camera){
        return getManhattanDistance(pos, camera.x, camera.y, camera.z);
    }

    private static float getManhattanDistance(BlockPos pos, double x, double y, double z) {
        return (float)(Math.abs(pos.getX() - x) + Math.abs(pos.getY() - y) + Math.abs(pos.getZ() - z));
    }

    public static void drawPathLines(MatrixStack matrices, VertexConsumerProvider vertexConsumers, List<BlockPos> positions){
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(LINES.apply(-5));
        Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
        if (camera.isReady()) {
            drawPathLines(matrices, vertexConsumer, positions, camera.getPos().x, camera.getPos().y, camera.getPos().z);
        }
    }

    public static void drawPathLines(MatrixStack matrices, VertexConsumer vertexConsumers, List<BlockPos> positions, double cameraX, double cameraY, double cameraZ) {
        for (int i = 0; i < positions.size()-1; i++) {
            var p1 = positions.get(i);
            var p2 = positions.get(i+1);
            float h1 = (float)i / positions.size() * 0.33F;
            float h2 = (float)(i+1) / positions.size() * 0.33F;
            int c1 = MathHelper.hsvToRgb(h1, 1.0F, 1.0F);
            int c2 = MathHelper.hsvToRgb(h2, 1.0F, 1.0F);

            var m1 = new Vec3d(p1.getX(), p1.getY(), p1.getZ()).add(0.5);
            var m2 = new Vec3d(p2.getX(), p2.getY(), p2.getZ()).add(0.5);

            var direction = m2.subtract(m1).normalize();

            vertexConsumers.vertex(matrices.peek(), (float)(p1.getX() - cameraX + 0.5), (float)(p1.getY() - cameraY + 0.5), (float)(p1.getZ() - cameraZ + 0.5))
                    .color(c1 >> 16 & 0xFF, c1 >> 8 & 0xFF, c1 & 0xFF, 255)
                    .normal((float) direction.x, (float) direction.y, (float) direction.z);

            vertexConsumers.vertex(matrices.peek(), (float)(p2.getX() - cameraX + 0.5), (float)(p2.getY() - cameraY + 0.5), (float)(p2.getZ() - cameraZ + 0.5))
                    .color(c2 >> 16 & 0xFF, c2 >> 8 & 0xFF, c2 & 0xFF, 255)
                    .normal((float) direction.x, (float) direction.y, (float) direction.z);
        }
    }
}
