package com.riburitu.regionvisualizer.util;

import com.google.gson.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class RegionManager {
    private static final String FOLDER_NAME = "regionvisualizer";
    private static final String FILE_NAME = "regions.json";

    private final List<Region> regions = new ArrayList<>();

    public List<Region> getRegions() {
        return Collections.unmodifiableList(regions);
    }

    public void addRegion(Region region) {
        regions.add(region);
    }

    public void removeRegion(String name) {
        regions.removeIf(r -> r.getName().equalsIgnoreCase(name));
    }

    public Optional<Region> getRegionContaining(BlockPos pos) {
        return regions.stream().filter(r -> r.contains(pos)).findFirst();
    }

    // --- Guardar y cargar ---
    private Path getSaveFolder(ServerLevel level) {
        ResourceLocation dim = level.dimension().location();
        String dimFolderName = dim.getPath();
        LevelResource levelResource = new LevelResource(dimFolderName);
        Path worldPath = level.getServer().getWorldPath(levelResource);
        return worldPath.resolve(FOLDER_NAME);
    }

    public void loadRegions(ServerLevel level) {
        // Reiniciar la lista de regiones al cargar un nuevo mundo
        regions.clear();

        Path folder = getSaveFolder(level);
        if (folder == null) return;

        Path file = folder.resolve(FILE_NAME);
        if (!Files.exists(file)) {
            System.out.println("[RegionVisualizer] No se encontró regions.json en " + folder + ", iniciando con lista vacía.");
            return;
        }

        try (Reader reader = Files.newBufferedReader(file)) {
            JsonElement element = JsonParser.parseReader(reader);
            if (!element.isJsonArray()) {
                System.out.println("[RegionVisualizer] El archivo regions.json no es un array válido.");
                return;
            }

            JsonArray array = element.getAsJsonArray();
            for (JsonElement e : array) {
                Region r = Region.fromJson(e.getAsJsonObject());
                regions.add(r);
            }
            System.out.println("[RegionVisualizer] Regiones cargadas: " + regions.size() + " desde " + file);
        } catch (IOException e) {
            System.err.println("[RegionVisualizer] Error al cargar regions.json: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void saveRegions(ServerLevel level) {
        if (regions.isEmpty()) {
            System.out.println("[RegionVisualizer] No hay regiones para guardar.");
            return;
        }

        Path folder = getSaveFolder(level);
        if (folder == null) return;

        try {
            if (!Files.exists(folder)) Files.createDirectories(folder);
            Path file = folder.resolve(FILE_NAME);

            JsonArray array = new JsonArray();
            for (Region r : regions) {
                array.add(r.toJson());
            }

            try (Writer writer = Files.newBufferedWriter(file)) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                gson.toJson(array, writer);
            }
            System.out.println("[RegionVisualizer] Regiones guardadas: " + regions.size() + " en " + file);
        } catch (IOException e) {
            System.err.println("[RegionVisualizer] Error al guardar regions.json: " + e.getMessage());
            e.printStackTrace();
        }
    }
}