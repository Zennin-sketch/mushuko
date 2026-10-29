package net.zennin.mushoku.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.zennin.mushoku.block.ChalkLineBlock;
import net.zennin.mushoku.block.MagicCircleBlockEntity;
import net.zennin.mushoku.registry.ModBlocks;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Ojo de Poder Magico: con el ojo prendido se ve el mana en las cosas. La tiza azul (infundida) y los circulos cargados
 * y listos para activarse brillan con un resplandor que late (y sueltan chispas).
 */
public final class ManaSightRenderer {
    private static final int R = 20, H = 10;
    private static final List<BlockPos> chalk = new ArrayList<>(), circles = new ArrayList<>();
    private static int tick;

    private ManaSightRenderer() {}

    private static boolean active() {
        return ClientExtraState.data.active("magic_power");
    }

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.level == null || mc.player == null) return;
            tick++;
            if (!active()) { chalk.clear(); circles.clear(); return; }
            if (tick % 10 == 0) scan(mc.level, mc.player.blockPosition());
            // chispas de mana
            var r = mc.level.random;
            for (BlockPos p : circles)
                if (r.nextInt(4) == 0) mc.level.addParticle(ParticleTypes.END_ROD, p.getX() + r.nextDouble(), p.getY() + 0.15, p.getZ() + r.nextDouble(), 0, 0.03, 0);
            for (BlockPos p : chalk)
                if (r.nextInt(30) == 0) mc.level.addParticle(ParticleTypes.GLOW, p.getX() + r.nextDouble(), p.getY() + 0.1, p.getZ() + r.nextDouble(), 0, 0.02, 0);
        });
        WorldRenderEvents.AFTER_TRANSLUCENT.register(ManaSightRenderer::render);
    }

    private static void scan(Level level, BlockPos c) {
        chalk.clear();
        circles.clear();
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-R, -H, -R), c.offset(R, H, R))) {
            BlockState s = level.getBlockState(p);
            if (s.is(ModBlocks.CHALK_LINE)) {
                if (s.getValue(ChalkLineBlock.CHARGED)) chalk.add(p.immutable());
            } else if (s.is(ModBlocks.MAGIC_CIRCLE) && level.getBlockEntity(p) instanceof MagicCircleBlockEntity be && be.ready()) {
                circles.add(p.immutable());
            }
        }
    }

    private static void render(WorldRenderContext ctx) {
        if (!active() || (chalk.isEmpty() && circles.isEmpty())) return;
        MultiBufferSource buffers = ctx.consumers();
        if (buffers == null) return;
        PoseStack pose = ctx.matrixStack();
        Vec3 cam = ctx.camera().getPosition();
        float t = (tick + ctx.tickCounter().getGameTimeDeltaPartialTick(false)) / 20f;
        float pulse = 0.55f + 0.45f * (float) Math.sin(t * 3.2f);
        VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f m = pose.last().pose();
        // tiza azul: un velo celeste sobre la linea
        for (BlockPos p : chalk) {
            float y = p.getY() + 0.03f;
            quad(vc, m, p.getX() + 0.1f, y, p.getZ() + 0.1f, p.getX() + 0.9f, p.getZ() + 0.9f, 0.35f, 0.75f, 1f, 0.35f * pulse);
            quad(vc, m, p.getX() - 0.05f, y + 0.01f, p.getZ() - 0.05f, p.getX() + 1.05f, p.getZ() + 1.05f, 0.25f, 0.55f, 1f, 0.15f * pulse);
        }
        // circulos listos: disco dorado que late y una columna tenue
        for (BlockPos p : circles) {
            float y = p.getY() + 0.04f;
            float grow = 0.15f * pulse;
            quad(vc, m, p.getX() - grow, y, p.getZ() - grow, p.getX() + 1 + grow, p.getZ() + 1 + grow, 1f, 0.85f, 0.35f, 0.45f * pulse);
            quad(vc, m, p.getX() - 0.4f, y + 0.01f, p.getZ() - 0.4f, p.getX() + 1.4f, p.getZ() + 1.4f, 0.6f, 0.4f, 1f, 0.2f * pulse);
            column(vc, m, p.getX() + 0.5f, y, p.getZ() + 0.5f, 0.32f, 1.6f + 0.4f * pulse, 1f, 0.9f, 0.5f, 0.22f * pulse);
        }
        pose.popPose();
        if (buffers instanceof MultiBufferSource.BufferSource bs) bs.endBatch(RenderType.lightning());
    }

    private static void quad(VertexConsumer vc, Matrix4f m, float x0, float y, float z0, float x1, float z1, float r, float g, float b, float a) {
        vc.addVertex(m, x0, y, z0).setColor(r, g, b, a);
        vc.addVertex(m, x0, y, z1).setColor(r, g, b, a);
        vc.addVertex(m, x1, y, z1).setColor(r, g, b, a);
        vc.addVertex(m, x1, y, z0).setColor(r, g, b, a);
        // de abajo tambien (por si se mira desde abajo)
        vc.addVertex(m, x1, y, z0).setColor(r, g, b, a);
        vc.addVertex(m, x1, y, z1).setColor(r, g, b, a);
        vc.addVertex(m, x0, y, z1).setColor(r, g, b, a);
        vc.addVertex(m, x0, y, z0).setColor(r, g, b, a);
    }

    /** Columna de luz (4 caras que se apagan hacia arriba). */
    private static void column(VertexConsumer vc, Matrix4f m, float cx, float y, float cz, float rad, float h, float r, float g, float b, float a) {
        float[][] c = {{-rad, -rad}, {rad, -rad}, {rad, rad}, {-rad, rad}};
        for (int i = 0; i < 4; i++) {
            float[] p0 = c[i], p1 = c[(i + 1) % 4];
            for (int side = 0; side < 2; side++) {
                float[] a0 = side == 0 ? p0 : p1, a1 = side == 0 ? p1 : p0;
                vc.addVertex(m, cx + a0[0], y, cz + a0[1]).setColor(r, g, b, a);
                vc.addVertex(m, cx + a1[0], y, cz + a1[1]).setColor(r, g, b, a);
                vc.addVertex(m, cx + a1[0], y + h, cz + a1[1]).setColor(r, g, b, 0f);
                vc.addVertex(m, cx + a0[0], y + h, cz + a0[1]).setColor(r, g, b, 0f);
            }
        }
    }
}
