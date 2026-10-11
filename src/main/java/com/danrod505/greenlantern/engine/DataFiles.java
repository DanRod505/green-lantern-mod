package com.danrod505.greenlantern.engine;

import com.danrod505.greenlantern.GreenLantern;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModFileInfo;

/**
 * Reads the engine's data files from the mod itself (the jar, or the build folder in development).
 * They are read once while the mod loads, before anything is registered: the characters decide which
 * items exist, so they can't wait for a data pack reload.
 */
public final class DataFiles {
    private DataFiles() {}

    public static JsonElement readJson(String path) {
        try (InputStream in = open(path)) {
            if (in == null) throw new IllegalStateException("Missing data file " + path);
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + path, e);
        }
    }

    private static InputStream open(String path) throws IOException {
        Path file = modPath(path);
        if (file != null && Files.isRegularFile(file)) return Files.newInputStream(file);
        InputStream in = DataFiles.class.getResourceAsStream("/" + path);
        if (in == null) in = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
        return in;
    }

    /** The {@code .json} files directly inside a folder of the mod's resources, as paths like {@code "data/greenlantern/characters/superman.json"}, sorted. */
    public static List<String> listJson(String folder) {
        List<String> files = new ArrayList<>();
        Path dir = modPath(folder);
        if (dir != null && Files.isDirectory(dir)) {
            try (Stream<Path> stream = Files.list(dir)) {
                stream.map(p -> p.getFileName().toString()).filter(name -> name.endsWith(".json")).sorted()
                        .forEach(name -> files.add(folder + "/" + name));
            } catch (IOException e) {
                throw new UncheckedIOException("Could not list " + folder, e);
            }
        }
        return files;
    }

    private static Path modPath(String path) {
        ModList mods = ModList.get();
        if (mods == null) return null;
        IModFileInfo info = mods.getModFileById(GreenLantern.MODID);
        if (info == null) return null;
        return info.getFile().findResource(path.split("/"));
    }
}
