package net.zennin.mushoku.hitogami;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Pair;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.zennin.mushoku.MushokuMod;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Las profecias de Hitogami: busca una estructura interesante cerca de tu cama y te da sus coordenadas con una pista.
 * La lista sale de data/<mod>/hitogami/*.json (ver data/remushoku/hitogami/oracle.json): para sumar las estructuras
 * propias del mod alcanza con agregar entradas ahi, sin tocar codigo.
 */
public final class Oracle implements SimpleSynchronousResourceReloadListener {
    public record Entry(ResourceLocation structure, ResourceKey<Level> dimension, String name, String hint) {}

    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static final String[] OPENINGS = {
            "Ah, viniste a verme mientras dormías. Qué conveniente... Te daré un consejo, gratis. Por ahora.",
            "Shh... no hagas ruido, tu cuerpo sigue en la cama. Escucha con atención, que no lo voy a repetir.",
            "¿Otra vez por aquí? Bien, bien. Hoy me siento generoso, así que te voy a contar un secreto.",
            "Tengo una buena noticia para ti. Bueno, buena para ti... seguramente.",
    };

    @Override
    public ResourceLocation getFabricId() { return MushokuMod.id("hitogami_oracle"); }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        ENTRIES.clear();
        for (Map.Entry<ResourceLocation, Resource> e : manager.listResources("hitogami", l -> l.getPath().endsWith(".json")).entrySet()) {
            try (Reader reader = e.getValue().openAsReader()) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                for (JsonElement el : root.getAsJsonArray("entries")) {
                    JsonObject o = el.getAsJsonObject();
                    ENTRIES.add(new Entry(ResourceLocation.parse(o.get("structure").getAsString()),
                            ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(o.has("dimension") ? o.get("dimension").getAsString() : "minecraft:overworld")),
                            o.get("name").getAsString(), o.has("hint") ? o.get("hint").getAsString() : ""));
                }
            } catch (Exception ex) {
                MushokuMod.LOG.error("No se pudo leer {}", e.getKey(), ex);
            }
        }
        MushokuMod.LOG.info("Hitogami conoce {} lugares", ENTRIES.size());
    }

    private static String dimName(ResourceKey<Level> dim) {
        if (dim == Level.OVERWORLD) return "el Overworld";
        if (dim == Level.NETHER) return "el Nether";
        if (dim == Level.END) return "el End";
        return dim.location().getPath();
    }

    /** Arma la profecia buscando cerca de 'origin' (la cama) en la dimension 'fromDim'. Devuelve null si no encontro nada. */
    public static String prophecy(ServerPlayer p, ResourceKey<Level> fromDim, BlockPos origin) {
        List<Entry> shuffled = new ArrayList<>(ENTRIES);
        Collections.shuffle(shuffled, new java.util.Random(p.getRandom().nextLong()));
        int tries = 0;
        for (Entry e : shuffled) {
            if (tries++ >= 4) break;
            ServerLevel level = p.server.getLevel(e.dimension());
            if (level == null) continue;
            Optional<Holder.Reference<Structure>> holder = level.registryAccess().registryOrThrow(Registries.STRUCTURE)
                    .getHolder(ResourceKey.create(Registries.STRUCTURE, e.structure()));
            if (holder.isEmpty()) continue;
            BlockPos from = origin;
            if (fromDim == Level.OVERWORLD && e.dimension() == Level.NETHER) from = new BlockPos(origin.getX() / 8, 64, origin.getZ() / 8);
            else if (fromDim == Level.NETHER && e.dimension() == Level.OVERWORLD) from = new BlockPos(origin.getX() * 8, 64, origin.getZ() * 8);
            else if (e.dimension() == Level.END) from = BlockPos.ZERO;
            Pair<BlockPos, Holder<Structure>> found;
            try {
                found = level.getChunkSource().getGenerator().findNearestMapStructure(level, HolderSet.direct(holder.get()), from, 64, false);
            } catch (Exception ex) {
                continue;
            }
            if (found == null) continue;
            BlockPos at = found.getFirst();
            int dist = (int) Math.sqrt(Math.pow(at.getX() - from.getX(), 2) + Math.pow(at.getZ() - from.getZ(), 2));
            String opening = OPENINGS[p.getRandom().nextInt(OPENINGS.length)];
            return opening + "\n\nEn " + dimName(e.dimension()) + ", en X " + at.getX() + ", Z " + at.getZ()
                    + " (a unos " + dist + " bloques de donde duermes), hay " + e.name() + ".\n\n"
                    + (e.hint().isEmpty() ? "" : e.hint() + "\n\n") + "Ve. Te va a convenir... créeme.";
        }
        return null;
    }
}
