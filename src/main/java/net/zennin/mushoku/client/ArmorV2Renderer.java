package net.zennin.mushoku.client;

import org.joml.Matrix4f;

import net.minecraft.resources.ResourceLocation;
import net.zennin.mushoku.MushokuMod;
import net.zennin.mushoku.item.ArmorV2Item;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/** Renderer de las armaduras v2: geo/armor_v2/<modelo>.geo.json y textures/armor_v2/<modelo>.png (+ brillo). */
public class ArmorV2Renderer extends GeoArmorRenderer<ArmorV2Item> {
    private final boolean fullBody;
    private final boolean cloak;

    public ArmorV2Renderer(String model, boolean glows, boolean fullBody) {
        super(new GeoModel<>() {
            @Override
            public ResourceLocation getModelResource(ArmorV2Item a) { return MushokuMod.id("geo/armor_v2/" + model + ".geo.json"); }

            @Override
            public ResourceLocation getTextureResource(ArmorV2Item a) { return MushokuMod.id("textures/armor_v2/" + model + ".png"); }

            @Override
            public ResourceLocation getAnimationResource(ArmorV2Item a) { return MushokuMod.id("animations/mk2model.animation.json"); }
        });
        this.fullBody = fullBody;
        this.cloak = model.contains("cloak");
        if (glows) addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    /** La capa con capucha se pone en el torso pero tambien muestra la capucha (hueso de la cabeza). */
    @Override
    protected void applyBoneVisibilityBySlot(net.minecraft.world.entity.EquipmentSlot slot) {
        super.applyBoneVisibilityBySlot(slot);
        if (fullBody && slot == net.minecraft.world.entity.EquipmentSlot.CHEST) setBoneVisible(this.head, true);
    }

    /**
     * Ropa que acompania a las piernas: los panos de faldas, tunicas y capas van en huesos de tela (hijos del torso)
     * que se abren hacia donde va cada pierna (adelante, atras o al costado), asi no las atraviesan al caminar.
     */
    @Override
    protected void applyBaseTransformations(net.minecraft.client.model.HumanoidModel<?> base) {
        super.applyBaseTransformations(base);
        var model = getGeoModel();
        // Giro de cada pierna respecto del torso (al agacharse el torso se inclina y las piernas quedan "atras")
        float r = base.rightLeg.xRot - base.body.xRot, l = base.leftLeg.xRot - base.body.xRot;
        float fwdR = Math.max(0, -r), fwdL = Math.max(0, -l), backR = Math.max(0, r), backL = Math.max(0, l);
        cloth(model, "clothF", Math.max(fwdR, fwdL), 1, 0);
        cloth(model, "clothFR", fwdR, 1, 0);
        cloth(model, "clothFL", fwdL, 1, 0);
        cloth(model, "clothB", Math.max(backR, backL), -1, 0);
        // Tela por zonas (ring_*_ANG): cada pano sigue a la pierna de su lado
        // (los panos ring_* ya no giran: se deforman vertice a vertice con las piernas, ver renderCube)
        skinSetup(base);
        // Costados: van sobre la pierna de su lado, asi que se balancean adelante y atras con ella (misma tela)
        side(model, "clothR", r, base.rightLeg.zRot);
        side(model, "clothL", l, base.leftLeg.zRot);
        // Hombros (Capa Atrapallamas): la capa sube y baja con cada hombro
        float dyR = cloak ? base.rightArm.y - base.body.y - 2f : 0, dyL = cloak ? base.leftArm.y - base.body.y - 2f : 0;
        shoulder(model, "clothR", dyR); shoulder(model, "clothFR", dyR);
        shoulder(model, "clothL", dyL); shoulder(model, "clothFL", dyL);
        shoulder(model, "clothF", (dyR + dyL) / 2); shoulder(model, "clothB", (dyR + dyL) / 2);
    }

    private final java.util.Map<String, Float> ringAngle = new java.util.HashMap<>();

    /**
     * Panos por zona alrededor de la cintura (0 adelante, 90 derecha, 180 atras): la pierna que va adelante empuja la
     * tela de adelante de su lado, la que va atras la de atras, y los costados acompanan a su pierna en los dos
     * sentidos. Cada pierna pesa mas en su lado y la mitad en el centro, asi los panos vecinos se mueven parecido y la
     * tela se ve continua. sR/sL: angulo de cada pierna (positivo = adelante).
     */
    private void rings(GeoModel<ArmorV2Item> model, float sR, float sL, float zR, float zL) {
        for (software.bernie.geckolib.cache.object.GeoBone b : model.getAnimationProcessor().getRegisteredBones()) {
            String n = b.getName();
            if (!n.startsWith("ring_")) continue;
            Float ang = ringAngle.get(n);
            if (ang == null) {
                try { ang = (float) Math.toRadians(Integer.parseInt(n.substring(n.lastIndexOf('_') + 1))); }
                catch (NumberFormatException e) { ang = 0f; }
                ringAngle.put(n, ang);
            }
            float side = (float) Math.sin(ang), front = (float) Math.cos(ang);
            float wR = Math.max(0, Math.min(1, 0.7f + 0.9f * side)), wL = Math.max(0, Math.min(1, 0.7f - 0.9f * side));
            float pushR = push(sR, front) * wR, pushL = push(sL, front) * wL;
            float fwd = Math.max(0, Math.max(pushR, pushL)), bwd = Math.min(0, Math.min(pushR, pushL));
            float d = fwd + bwd;
            float span = Math.max(6.5f, b.getPivotY() - 6);
            float k = Math.min(Math.abs(d), 1.4f);
            float a = (float) Math.asin(Math.min(1, 6 * Math.sin(k) / span)) * Math.signum(d);
            b.setRotX(a);
            b.setRotY(0);
            b.setRotZ(Math.abs(side) > 0.5f ? (side > 0 ? zR : zL) * Math.abs(side) : 0);
        }
    }

    /** Cuanto mueve una pierna (angulo s, + adelante) la tela que esta en 'front' (1 adelante, -1 atras). */
    private static float push(float s, float front) {
        if (s > 0) return s * (front > -0.35f ? 1f : 0.15f);
        return s * (front < 0.35f ? 1f : 0.15f);
    }

    /** Pano del costado: gira sobre X siguiendo el angulo de la pierna (con signo: adelante o atras). */
    private static void side(GeoModel<ArmorV2Item> model, String name, float legRel, float z) {
        model.getBone(name).ifPresent(b -> {
            float span = Math.max(6.5f, b.getPivotY() - 6);
            float k = Math.min(Math.abs(legRel), 1.4f);
            float a = (float) Math.asin(Math.min(1, 6 * Math.sin(k) / span)) * Math.signum(legRel);
            b.setRotX(-a);
            b.setRotY(0);
            b.setRotZ(z);
        });
    }

    private static void shoulder(GeoModel<ArmorV2Item> model, String name, float dy) {
        model.getBone(name).ifPresent(b -> b.setPosY(Math.abs(dy) < 1e-3f ? 0 : -dy));
    }

    /**
     * Angulo del pano para seguir a la pierna: la rodilla (6 px bajo la cadera) se corre 6*sen(a); el pano, que gira
     * desde su borde de arriba (cintura u hombros), tiene que correrse lo mismo a esa altura.
     */
    private static void cloth(GeoModel<ArmorV2Item> model, String name, float legAngle, int dir, float z) {
        model.getBone(name).ifPresent(b -> {
            float span = Math.max(6.5f, b.getPivotY() - 6);
            float a = (float) Math.asin(Math.min(1, 6 * Math.sin(Math.min(legAngle, 1.4f)) / span)) * 1.1f;
            b.setRotX(dir * a);
            b.setRotY(0);
            b.setRotZ(z);
        });
    }

    public static Object create(String model, boolean glows, boolean fullBody) { return new ArmorV2Renderer(model, glows, fullBody); }

    // ================================================================ tela pegada a las piernas (pesos por vertice)
    // Como bendy-lib / Epic Fight: cada vertice de los panos ring_* esta atado al cuerpo y a cada pierna con un peso
    // (mas pierna cuanto mas abajo, y mas de la pierna de su lado). Se mueve con la misma cuenta que la pierna, asi que
    // nunca se separa; el ruedo ademas sigue a una copia "atrasada" de las piernas (balanceo de tela).
    private static final Matrix4f F = new Matrix4f().translate(0, 24, 0).scale(-1, -1, 1);
    private static final Matrix4f F_INV = new Matrix4f(F).invert();
    private final Matrix4f skinR = new Matrix4f(), skinL = new Matrix4f(), lagR = new Matrix4f(), lagL = new Matrix4f();
    /** Del marco del torso (geo, px) al marco propio de cada pierna (px del modelo vanilla) y de vuelta. */
    private final Matrix4f[] toLeg = {new Matrix4f(), new Matrix4f()}, fromLeg = {new Matrix4f(), new Matrix4f()};
    /** Medio ancho de la pierna con la media/pantalon puesto, mas el grosor de la tela. */
    private static final float LEG_HALF = 2.85f;
    /** Rodillas (Player Animator): giro rigido de la mitad de arriba y de abajo de cada pierna, en el marco de la pierna. */
    private final Matrix4f[] kneeTop = {new Matrix4f(), new Matrix4f()}, kneeBot = {new Matrix4f(), new Matrix4f()};
    private final Matrix4f[] kneeTopInv = {new Matrix4f(), new Matrix4f()}, kneeBotInv = {new Matrix4f(), new Matrix4f()};
    private final boolean[] knee = new boolean[2];
    /** Caida: 0 parado, 1 cayendo rapido (suavizado por entidad). La tela se levanta y se abre. */
    private float fall;
    private final java.util.Map<Integer, float[]> falls = new java.util.HashMap<>();
    private static final String[] LEGS = {"rightLeg", "leftLeg"};
    private final java.util.Map<Integer, float[]> springs = new java.util.HashMap<>();
    private String curBone;

    private static Matrix4f partMatrix(net.minecraft.client.model.geom.ModelPart p) {
        return new Matrix4f().translate(p.x, p.y, p.z).rotateZYX(p.zRot, p.yRot, p.xRot);
    }

    /** Del reposo al estado actual de una pierna, en el marco del torso (pixeles del modelo de GeckoLib). */
    private static Matrix4f legSkin(Matrix4f bodyInv, net.minecraft.client.model.geom.ModelPart leg, float restX) {
        Matrix4f t = new Matrix4f(bodyInv).mul(partMatrix(leg)).mul(new Matrix4f().translate(-restX, -12, 0));
        return new Matrix4f(F).mul(t).mul(F_INV);
    }

    private static Matrix4f legAngle(float restX, float xRot, float zRot) {
        Matrix4f t = new Matrix4f().translate(restX, 12, 0).rotateZYX(zRot, 0, xRot).translate(-restX, -12, 0);
        return new Matrix4f(F).mul(t).mul(F_INV);
    }

    private static int dbgCount;

    private void skinSetup(net.minecraft.client.model.HumanoidModel<?> base) {
        if (System.getProperty("mushoku.legdebug") != null && currentEntity instanceof net.minecraft.client.player.AbstractClientPlayer pl
                && (dbgCount++ % 15) == 0
                && net.minecraft.client.Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(pl) instanceof net.minecraft.client.renderer.entity.player.PlayerRenderer pr) {
            var pm = pr.getModel();
            org.slf4j.LoggerFactory.getLogger("remushoku").info("legdebug armor R({},{},{} rx{} rz{}) body({},{} rx{} ry{}) | player R({},{},{} rx{} rz{}) body({},{} rx{} ry{}) model={}",
                    f(base.rightLeg.x), f(base.rightLeg.y), f(base.rightLeg.z), f(base.rightLeg.xRot), f(base.rightLeg.zRot),
                    f(base.body.y), f(base.body.z), f(base.body.xRot), f(base.body.yRot),
                    f(pm.rightLeg.x), f(pm.rightLeg.y), f(pm.rightLeg.z), f(pm.rightLeg.xRot), f(pm.rightLeg.zRot),
                    f(pm.body.y), f(pm.body.z), f(pm.body.xRot), f(pm.body.yRot), pm.rightLeg.getClass().getSimpleName());
        }
        Matrix4f bodyInv = partMatrix(base.body).invert();
        skinR.set(legSkin(bodyInv, base.rightLeg, -1.9f));
        skinL.set(legSkin(bodyInv, base.leftLeg, 1.9f));
        Matrix4f body = partMatrix(base.body);
        net.minecraft.client.model.geom.ModelPart[] legs = {base.rightLeg, base.leftLeg};
        for (int i = 0; i < 2; i++) {
            toLeg[i].set(partMatrix(legs[i])).invert().mul(body).mul(F);
            fromLeg[i].set(toLeg[i]).invert();
            boolean b1 = kneeFit(LEGS[i], 1.5f, kneeTop[i]), b2 = kneeFit(LEGS[i], 10.5f, kneeBot[i]);
            knee[i] = b1 || b2;
            kneeTopInv[i].set(kneeTop[i]).invert();
            kneeBotInv[i].set(kneeBot[i]).invert();
        }
        // copia atrasada (resorte) de las piernas para el ruedo
        float rx = base.rightLeg.xRot - base.body.xRot, lx = base.leftLeg.xRot - base.body.xRot;
        int id = currentEntity == null ? 0 : currentEntity.getId();
        float[] st = springs.computeIfAbsent(id, k -> new float[]{rx, 0, lx, 0, net.minecraft.Util.getMillis()});
        float now = net.minecraft.Util.getMillis();
        float dt = Math.min(0.1f, Math.max(0f, (now - st[4]) / 1000f));
        st[4] = now;
        for (int i = 0; i < 2; i++) {
            float target = i == 0 ? rx : lx;
            int a = i * 2;
            for (int k = 0; k < 4; k++) {
                float h = dt / 4;
                st[a + 1] += ((target - st[a]) * 55f - st[a + 1] * 9f) * h;
                st[a] += st[a + 1] * h;
            }
        }
        lagR.set(legAngle(-1.9f, st[0] + base.body.xRot * 0, base.rightLeg.zRot));
        lagL.set(legAngle(1.9f, st[2], base.leftLeg.zRot));
        // caida: cuanto baja por tick (los otros jugadores no mandan su velocidad: se usa el cambio de altura)
        fall = 0;
        if (currentEntity != null && !currentEntity.onGround()) {
            float[] fs = falls.computeIfAbsent(id, k -> new float[]{0, now});
            float fdt = Math.min(0.1f, Math.max(0f, (now - fs[1]) / 1000f));
            fs[1] = now;
            double dy = currentEntity.getY() - currentEntity.yOld;
            float target = (float) Math.max(0, Math.min(1, (-dy - 0.18) / 0.7));
            fs[0] += (target - fs[0]) * Math.min(1f, fdt * 6f);
            fall = fs[0];
        } else if (currentEntity != null) {
            float[] fs = falls.get(id);
            if (fs != null) { fs[0] = 0; fs[1] = now; }
        }
        if (falls.size() > 64) falls.clear();
        // las piernas reales de este cuadro, para mezclar el angulo atrasado con su posicion
        if (springs.size() > 64) springs.clear();
    }

    @Override
    public void renderRecursively(com.mojang.blaze3d.vertex.PoseStack poseStack, ArmorV2Item animatable,
                                  software.bernie.geckolib.cache.object.GeoBone bone, net.minecraft.client.renderer.RenderType renderType,
                                  net.minecraft.client.renderer.MultiBufferSource bufferSource, com.mojang.blaze3d.vertex.VertexConsumer buffer,
                                  boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        String prev = curBone;
        curBone = bone.getName();
        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
        curBone = prev;
    }

    @Override
    public void renderCube(com.mojang.blaze3d.vertex.PoseStack poseStack, software.bernie.geckolib.cache.object.GeoCube cube,
                           com.mojang.blaze3d.vertex.VertexConsumer buffer, int packedLight, int packedOverlay, int colour) {
        if (curBone != null && !curBone.startsWith("ring_")) {
            String limb = limbOf(curBone);
            if (limb != null && currentEntity != null && net.zennin.mushoku.client.LimbBend.BENDER != null) {
                renderLimbCube(poseStack, cube, buffer, packedLight, packedOverlay, colour, limb);
                return;
            }
        }
        if (curBone == null || !curBone.startsWith("ring_")) {
            super.renderCube(poseStack, cube, buffer, packedLight, packedOverlay, colour);
            return;
        }
        Matrix4f bodyPose = new Matrix4f(poseStack.last().pose());
        org.joml.Matrix3f bodyNormal = new org.joml.Matrix3f(poseStack.last().normal());
        software.bernie.geckolib.util.RenderUtil.translateToPivotPoint(poseStack, cube);
        software.bernie.geckolib.util.RenderUtil.rotateMatrixAroundCube(poseStack, cube);
        software.bernie.geckolib.util.RenderUtil.translateAwayFromPivotPoint(poseStack, cube);
        // giro propio del pano (vuelo de la falda), en el marco del torso
        Matrix4f cubeRot = new Matrix4f(bodyPose).invert().mul(poseStack.last().pose());
        org.joml.Matrix3f nrm = poseStack.last().normal();
        org.joml.Vector3f p = new org.joml.Vector3f(), a = new org.joml.Vector3f(), b = new org.joml.Vector3f();
        for (software.bernie.geckolib.cache.object.GeoQuad quad : cube.quads()) {
            if (quad == null) continue;
            org.joml.Vector3f normal = nrm.transform(new org.joml.Vector3f(quad.normal()));
            for (software.bernie.geckolib.cache.object.GeoVertex v : quad.vertices()) {
                // a pixeles en el marco del torso
                p.set(v.position());
                cubeRot.transformPosition(p);   // (en bloques: el giro del pano incluye su traslado al pivote)
                p.mul(16f);
                float dh = 12f - p.y;                      // cuanto esta por debajo de la cadera
                // la tela va con la pierna al mismo tiempo (sin atrasarse): pegada del todo a partir de ~6 px bajo la cadera
                float w = smoothstep((dh + 1f) / 7f);
                org.joml.Vector3f out = new org.joml.Vector3f(p).add(disp(p, skinR, skinL, w, a, b));
                // ruedo: parte del movimiento viene de las piernas atrasadas
                // (sin balanceo atrasado del ruedo: hacia que la tela fuera mas lenta que los pies y se atravesaran)
                // rodillas: la tela de abajo sigue a la canilla
                if (knee[0] || knee[1]) {
                    float dr = Math.abs(p.x - 1.9f), dl = Math.abs(p.x + 1.9f);
                    kneeSkin(out, w, Math.max(0, Math.min(1, 0.5f + (dl - dr) / 6.5f)), a);
                }
                // cayendo: la tela se levanta y se abre (mas cuanto mas abajo cuelga), con un aleteo
                if (fall > 0.01f && dh > 0) {
                    float k = fall * Math.min(dh, 16f);
                    float ang = (float) Math.atan2(p.z, p.x);
                    float flap = 1f + 0.18f * (float) Math.sin(net.minecraft.Util.getMillis() / 55.0 + ang * 3);
                    out.y += k * 0.5f * flap;
                    float r = (float) Math.sqrt(p.x * p.x + p.z * p.z);
                    if (r > 0.5f) { out.x += p.x / r * k * 0.32f * flap; out.z += p.z / r * k * 0.32f * flap; }
                }
                // choque: si la tela quedo adentro de una pierna, sale por la cara mas cercana (adelante/atras o costado)
                collide(out, a);
                out.div(16f);
                org.joml.Vector4f w4 = bodyPose.transform(new org.joml.Vector4f(out, 1f));
                buffer.addVertex(w4.x(), w4.y(), w4.z(), colour, v.texU(), v.texV(), packedOverlay, packedLight, normal.x(), normal.y(), normal.z());
            }
        }
        if (bodyNormal == null) return;
    }

    /**
     * Cuanto se mueve un vertice de tela con las piernas. De costado sigue a la pierna de su lado; adelante y atras
     * la tela "envuelve" a las dos piernas (adelante va con la pierna mas adelantada, atras con la mas atrasada), asi
     * los panos vecinos se mueven igual, no se cruzan y la pierna nunca la atraviesa.
     */
    private static org.joml.Vector3f disp(org.joml.Vector3f p, Matrix4f mR, Matrix4f mL, float w,
                                         org.joml.Vector3f a, org.joml.Vector3f b) {
        mR.transformPosition(a.set(p)).sub(p);
        mL.transformPosition(b.set(p)).sub(p);
        float dr = Math.abs(p.x - 1.9f), dl = Math.abs(p.x + 1.9f);
        float sR = Math.max(0, Math.min(1, 0.5f + (dl - dr) / 6.5f));
        org.joml.Vector3f d = new org.joml.Vector3f(a).mul(sR).add(new org.joml.Vector3f(b).mul(1 - sR));
        // envolvente adelante/atras (adelante = -Z)
        float cR = Math.min(1, 0.3f + sR * 1.4f), cL = Math.min(1, 0.3f + (1 - sR) * 1.4f);
        float zr = a.z * cR, zl = b.z * cL;
        float env = p.z < 0 ? Math.min(zr, zl) : Math.max(zr, zl);
        float k = smoothstep(Math.abs(p.z) / (Math.abs(p.z) + Math.abs(p.x) + 0.01f) * 2f - 0.4f);
        d.z = d.z + (env - d.z) * k;
        return d.mul(w);
    }

    /** Giro rigido que le hace la rodilla a la altura y de la pierna (probando tres puntos con el doblez real). */
    private boolean kneeFit(String part, float y, Matrix4f out) {
        out.identity();
        if (currentEntity == null) return false;
        org.joml.Vector3f p0 = new org.joml.Vector3f(0, y, 0), px = new org.joml.Vector3f(1, y, 0), pz = new org.joml.Vector3f(0, y, 1);
        if (!net.zennin.mushoku.client.LimbBend.bend(currentEntity, part, p0)) return false;
        net.zennin.mushoku.client.LimbBend.bend(currentEntity, part, px);
        net.zennin.mushoku.client.LimbBend.bend(currentEntity, part, pz);
        org.joml.Vector3f cx = new org.joml.Vector3f(px).sub(p0).normalize(), cz = new org.joml.Vector3f(pz).sub(p0);
        cz.sub(new org.joml.Vector3f(cx).mul(cz.dot(cx))).normalize();
        org.joml.Vector3f cy = new org.joml.Vector3f(cz).cross(cx).normalize();
        Matrix4f r = new Matrix4f(cx.x, cx.y, cx.z, 0, cy.x, cy.y, cy.z, 0, cz.x, cz.y, cz.z, 0, 0, 0, 0, 1);
        org.joml.Vector3f t = new org.joml.Vector3f(p0).sub(r.transformPosition(new org.joml.Vector3f(0, y, 0)));
        out.translation(t).mul(r);
        return true;
    }

    /** La ropa de esa pierna tambien se dobla en la rodilla (arriba como el muslo, abajo como la canilla). */
    private void kneeSkin(org.joml.Vector3f out, float w, float sR, org.joml.Vector3f q) {
        for (int i = 0; i < 2; i++) {
            if (!knee[i]) continue;
            // a medias: la canilla que patea para atras no levanta la tela (la empuja el choque si hace falta)
            float wi = w * (i == 0 ? sR : 1 - sR) * 0.4f;
            if (wi < 1e-3f) continue;
            toLeg[i].transformPosition(q.set(out));
            float low = smoothstep((q.y - 4f) / 5f);
            org.joml.Vector3f a = kneeTop[i].transformPosition(new org.joml.Vector3f(q)), b = kneeBot[i].transformPosition(new org.joml.Vector3f(q));
            a.lerp(b, low);
            fromLeg[i].transformPosition(a);
            out.lerp(a, wi);
        }
    }

    /** Piezas de la armadura de brazos y piernas: se doblan en codos y rodillas igual que la piel. */
    private static String limbOf(String bone) {
        return switch (bone) {
            case "armorRightLeg", "armorRightBoot" -> "rightLeg";
            case "armorLeftLeg", "armorLeftBoot" -> "leftLeg";
            case "armorRightArm" -> "rightArm";
            case "armorLeftArm" -> "leftArm";
            default -> null;
        };
    }

    private static final java.util.Map<String, float[]> REST = java.util.Map.of(
            "rightLeg", new float[]{-1.9f, 12, 0}, "leftLeg", new float[]{1.9f, 12, 0},
            "rightArm", new float[]{-5, 2, 0}, "leftArm", new float[]{5, 2, 0});

    private void renderLimbCube(com.mojang.blaze3d.vertex.PoseStack poseStack, software.bernie.geckolib.cache.object.GeoCube cube,
                                com.mojang.blaze3d.vertex.VertexConsumer buffer, int packedLight, int packedOverlay, int colour, String limb) {
        Matrix4f bonePose = new Matrix4f(poseStack.last().pose());
        software.bernie.geckolib.util.RenderUtil.translateToPivotPoint(poseStack, cube);
        software.bernie.geckolib.util.RenderUtil.rotateMatrixAroundCube(poseStack, cube);
        software.bernie.geckolib.util.RenderUtil.translateAwayFromPivotPoint(poseStack, cube);
        Matrix4f cubeRot = new Matrix4f(bonePose).invert().mul(poseStack.last().pose());
        org.joml.Matrix3f nrm = poseStack.last().normal();
        float[] rest = REST.get(limb);
        org.joml.Vector3f p = new org.joml.Vector3f(), q = new org.joml.Vector3f();
        for (software.bernie.geckolib.cache.object.GeoQuad quad : cube.quads()) {
            if (quad == null) continue;
            org.joml.Vector3f normal = nrm.transform(new org.joml.Vector3f(quad.normal()));
            for (software.bernie.geckolib.cache.object.GeoVertex v : quad.vertices()) {
                p.set(v.position());
                cubeRot.transformPosition(p);
                p.mul(16f);
                // al marco de la pieza vanilla (px, y hacia abajo), se dobla y se vuelve
                q.set(-p.x - rest[0], 24f - p.y - rest[1], p.z - rest[2]);
                if (net.zennin.mushoku.client.LimbBend.bend(currentEntity, limb, q))
                    p.set(-(q.x + rest[0]), 24f - (q.y + rest[1]), q.z + rest[2]);
                p.div(16f);
                org.joml.Vector4f w4 = bonePose.transform(new org.joml.Vector4f(p, 1f));
                buffer.addVertex(w4.x(), w4.y(), w4.z(), colour, v.texU(), v.texV(), packedOverlay, packedLight, normal.x(), normal.y(), normal.z());
            }
        }
    }

    private void collide(org.joml.Vector3f p, org.joml.Vector3f q) {
        for (int i = 0; i < 2; i++) {
            toLeg[i].transformPosition(q.set(p));
            if (!knee[i]) {
                if (pushOut(q, -0.5f, 13f)) { fromLeg[i].transformPosition(q); p.set(q); }
                continue;
            }
            // pierna doblada: el muslo y la canilla son dos cajas, cada una con su giro
            org.joml.Vector3f u = kneeTopInv[i].transformPosition(new org.joml.Vector3f(q));
            if (pushOut(u, -0.5f, 6.5f)) { kneeTop[i].transformPosition(u); q.set(u); }
            u = kneeBotInv[i].transformPosition(new org.joml.Vector3f(q));
            if (pushOut(u, 5.5f, 13f)) { kneeBot[i].transformPosition(u); q.set(u); }
            fromLeg[i].transformPosition(q);
            p.set(q);
        }
    }

    /** Si el punto (marco de la pierna) esta dentro de la caja entre y0 e y1, sale por la cara mas cercana. */
    private static boolean pushOut(org.joml.Vector3f q, float y0, float y1) {
        if (q.y < y0 || q.y > y1) return false;
        float ax = Math.abs(q.x), az = Math.abs(q.z);
        if (ax >= LEG_HALF || az >= LEG_HALF) return false;
        // adelante/atras se prefiere (asi la tela del medio no se va hacia la otra pierna)
        if (LEG_HALF - az <= (LEG_HALF - ax) * 1.6f) q.z = q.z > 0 ? LEG_HALF : -LEG_HALF;
        else q.x = q.x > 0 ? LEG_HALF : -LEG_HALF;
        return true;
    }

    private static String f(float v) { return String.format(java.util.Locale.ROOT, "%.2f", v); }

    private static float smoothstep(float t) {
        t = Math.max(0, Math.min(1, t));
        return t * t * (3 - 2 * t);
    }
}
