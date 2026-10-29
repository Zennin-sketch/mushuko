package net.zennin.mushoku.client.fp;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.zennin.mushoku.MushokuMod;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

/**
 * Animaciones de primera persona cargadas de assets/<mod>/fp_animations/*.json (de cualquier mod o pack de recursos;
 * el nombre es el del archivo sin la extension). F3+T las recarga.
 */
public final class FpAnimations {
    private static final Map<String, FpAnim> ANIMS = new HashMap<>();

    private FpAnimations() {}

    public static FpAnim get(String name) { return name == null ? null : ANIMS.get(name); }

    public static void init() {
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public ResourceLocation getFabricId() { return MushokuMod.id("fp_animations"); }

            @Override
            public void onResourceManagerReload(ResourceManager manager) {
                ANIMS.clear();
                for (Map.Entry<ResourceLocation, Resource> e : manager.listResources("fp_animations", id -> id.getPath().endsWith(".json")).entrySet()) {
                    String path = e.getKey().getPath();
                    String name = path.substring(path.lastIndexOf('/') + 1, path.length() - 5);
                    try (Reader r = e.getValue().openAsReader()) {
                        JsonObject j = JsonParser.parseReader(r).getAsJsonObject();
                        ANIMS.put(name, FpAnim.parse(name, j));
                    } catch (Exception ex) {
                        org.slf4j.LoggerFactory.getLogger("remushoku").warn("Animacion de primera persona invalida: {} ({})", e.getKey(), ex.toString());
                    }
                }
                org.slf4j.LoggerFactory.getLogger("remushoku").info("Animaciones de primera persona: {}", ANIMS.size());
            }
        });
    }
}
