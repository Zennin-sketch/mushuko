package net.zennin.mushoku.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
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
 * y listos para activarse brillan con un resplandor que late (y sueltan chispas). Se ve a traves de las paredes, asi
 * sirve para encontrar trampas escondidas; tambien marca a los seres vivos cercanos (rojo los hostiles) y las minas
 * armadas, aunque esten detras de un muro.
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
        WorldRenderEvents.LAST.register(ManaSightRenderer::render);
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
        Minecraft mc = Minecraft.getInstance();
        if (!active() || mc.level == null || mc.player == null) return;
        PoseStack pose = ctx.matrixStack();
        Vec3 cam = ctx.camera().getPosition();
        float partial = ctx.tickCounter().getGameTimeDeltaPartialTick(false);
        float t = (tick + partial) / 20f;
        float pulse = 0.55f + 0.45f * (float) Math.sin(t * 3.2f);
        // seres vivos y minas cercanas (tambien detras de las paredes)
        List<net.minecraft.world.entity.Entity> ents = mc.level.getEntities(mc.player, mc.player.getBoundingBox().inflate(R, H, R),
                e -> (e instanceof net.minecraft.world.entity.LivingEntity l && l.isAlive() && !l.isInvisible())
                        || e instanceof net.zennin.mushoku.entity.MineEntity);
        if (chalk.isEmpty() && circles.isEmpty() && ents.isEmpty()) return;
        // sin prueba de profundidad: se dibuja por encima de los bloques (se ve a traves de las paredes)
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
                com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);
        com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
        com.mojang.blaze3d.systems.RenderSystem.depthMask(false);
        com.mojang.blaze3d.systems.RenderSystem.disableCull();
        com.mojang.blaze3d.systems.RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionColorShader);
        com.mojang.blaze3d.vertex.BufferBuilder vc = com.mojang.blaze3d.vertex.Tesselator.getInstance().begin(
                com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS, com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_COLOR);
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
        // aura de mana de cada ser vivo: rojo los hostiles, celeste el resto; las minas, dorado
        for (net.minecraft.world.entity.Entity e : ents) {
            double x = net.minecraft.util.Mth.lerp(partial, e.xo, e.getX()), y = net.minecraft.util.Mth.lerp(partial, e.yo, e.getY()),
                    z = net.minecraft.util.Mth.lerp(partial, e.zo, e.getZ());
            float hw = e.getBbWidth() / 2f + 0.08f, h = e.getBbHeight() + 0.1f;
            float r, g, b;
            if (e instanceof net.zennin.mushoku.entity.MineEntity) { r = 1f; g = 0.75f; b = 0.2f; }
            else if (e instanceof net.minecraft.world.entity.monster.Enemy) { r = 1f; g = 0.25f; b = 0.3f; }
            else { r = 0.4f; g = 0.75f; b = 1f; }
            box(vc, m, (float) x, (float) y, (float) z, hw, h, r, g, b, 0.16f * pulse + 0.06f);
        }
        pose.popPose();
        var mesh = vc.build();
        if (mesh != null) com.mojang.blaze3d.vertex.BufferUploader.drawWithShader(mesh);
        com.mojang.blaze3d.systems.RenderSystem.enableCull();
        com.mojang.blaze3d.systems.RenderSystem.depthMask(true);
        com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    }

    /** Caja translucida alrededor de una entidad (las 4 caras de los costados y la de arriba). */
    private static void box(VertexConsumer vc, Matrix4f m, float cx, float y, float cz, float hw, float h, float r, float g, float b, float a) {
        float x0 = cx - hw, x1 = cx + hw, z0 = cz - hw, z1 = cz + hw, y1 = y + h;
        float[][] sides = {{x0, z0, x1, z0}, {x1, z0, x1, z1}, {x1, z1, x0, z1}, {x0, z1, x0, z0}};
        for (float[] s : sides) {
            vc.addVertex(m, s[0], y, s[1]).setColor(r, g, b, a);
            vc.addVertex(m, s[2], y, s[3]).setColor(r, g, b, a);
            vc.addVertex(m, s[2], y1, s[3]).setColor(r, g, b, a * 0.35f);
            vc.addVertex(m, s[0], y1, s[1]).setColor(r, g, b, a * 0.35f);
        }
        vc.addVertex(m, x0, y1, z0).setColor(r, g, b, a * 0.5f);
        vc.addVertex(m, x0, y1, z1).setColor(r, g, b, a * 0.5f);
        vc.addVertex(m, x1, y1, z1).setColor(r, g, b, a * 0.5f);
        vc.addVertex(m, x1, y1, z0).setColor(r, g, b, a * 0.5f);
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
