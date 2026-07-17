package powie.powhax.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import meteordevelopment.meteorclient.utils.Utils;
import net.fabricmc.loader.api.FabricLoader;
import powie.powhax.modules.PriceScraper;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static powie.powhax.Powhax.LOG;

public class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Path CONFIG_FOLDER = FabricLoader.getInstance().getGameDir().resolve("powhax");
    private static final Path SHOP_FOLDER = CONFIG_FOLDER.resolve("shop");
    private static final Path BEDROCK_FOLDER = CONFIG_FOLDER.resolve("bedrock");

    public static void initializeConfig() {
        try {
            Files.createDirectories(CONFIG_FOLDER);
            Files.createDirectories(SHOP_FOLDER);
            Files.createDirectories(BEDROCK_FOLDER);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create config folder", e);
        }
    }

    // shop

    public static Path writeNewShopData(Map<String, PriceScraper.ItemForSale> itemsForSale) {
        Path file = nextAvailableFile(LocalDate.now(), Utils.getWorldName(), SHOP_FOLDER, "json");
        try (BufferedWriter writer = Files.newBufferedWriter(file)) {
            GSON.toJson(itemsForSale, writer);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write shop data to " + file, e);
        }
        LOG.info("Wrote shop data to: {}", file);
        return file;
    }

    // bedrock

    public static Path newBedrockFile() {
        return nextAvailableFile(LocalDate.now(), Utils.getWorldName(), BEDROCK_FOLDER, "txt");
    }

    private static Path nextAvailableFile(LocalDate date, String worldName, Path folder, String extension) {
        String safeWorldName = worldName.replaceAll("[^a-zA-Z0-9._-]", "_");
        String datePrefix = date.format(DateTimeFormatter.ISO_LOCAL_DATE);

        int index = 1;
        Path file;
        do {
            file = folder.resolve("%s_%s_%d.%s".formatted(datePrefix, safeWorldName, index++, extension));
        } while (Files.exists(file));
        return file;
    }
}
