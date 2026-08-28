package org.nia.niamod.gui.render;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.nia.niamod.NiamodClient;
import org.nia.niamod.util.WebUtils;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class WynntilsMapTileManager {
    private static final Gson GSON = new Gson();
    private static final Type TILE_LIST_TYPE = new TypeToken<List<MapTile>>() {
    }.getType();
    private static final String MAPS_URL = "https://cdn.wynntils.com/static/Reference/maps.json";
    private static final long MANIFEST_RECHECK_MILLIS = 10L * 60_000L;

    private final Object lock = new Object();
    private final Map<String, TileTexture> textures = new HashMap<>();
    private final Set<String> loadingTiles = new HashSet<>();
    private List<MapTile> tiles = List.of();
    private boolean manifestInFlight;
    private long lastManifestRequestMillis;

    public void requestManifest() {
        long now = System.currentTimeMillis();
        synchronized (lock) {
            if (manifestInFlight || now - lastManifestRequestMillis < MANIFEST_RECHECK_MILLIS) {
                return;
            }
            manifestInFlight = true;
            lastManifestRequestMillis = now;
        }

        WebUtils.queryAPIAsync(MAPS_URL).whenComplete((body, throwable) -> {
            if (throwable != null) {
                synchronized (lock) {
                    manifestInFlight = false;
                }
                NiamodClient.LOGGER.warn("Failed to load Wynntils map manifest", throwable);
                return;
            }
            List<MapTile> parsed = parseTiles(body);
            synchronized (lock) {
                tiles = parsed;
                manifestInFlight = false;
            }
        });
    }

    public List<MapTile> tiles() {
        requestManifest();
        synchronized (lock) {
            return tiles;
        }
    }

    public Identifier texture(MapTile tile) {
        if (tile == null || tile.md5() == null || tile.md5().isBlank()) {
            return null;
        }

        TileTexture texture;
        synchronized (lock) {
            texture = textures.get(tile.key());
            if (texture != null && texture.readyFor(tile)) {
                return texture.identifier();
            }
        }

        requestTile(tile);
        return null;
    }

    public int textureWidth(MapTile tile) {
        synchronized (lock) {
            TileTexture texture = textures.get(tile.key());
            return texture == null ? tile.worldWidth() : texture.width();
        }
    }

    public int textureHeight(MapTile tile) {
        synchronized (lock) {
            TileTexture texture = textures.get(tile.key());
            return texture == null ? tile.worldHeight() : texture.height();
        }
    }

    private void requestTile(MapTile tile) {
        synchronized (lock) {
            TileTexture texture = textures.get(tile.key());
            if (texture != null && texture.readyFor(tile)) {
                return;
            }
            if (!loadingTiles.add(tile.key())) {
                return;
            }
        }

        CompletableFuture.supplyAsync(() -> cachedTilePath(tile))
                .thenAccept(path -> Minecraft.getInstance().execute(() -> registerTexture(tile, path)))
                .exceptionally(throwable -> {
                    synchronized (lock) {
                        loadingTiles.remove(tile.key());
                    }
                    NiamodClient.LOGGER.warn("Failed to load Wynntils map tile {}", tile.name(), throwable);
                    return null;
                });
    }

    private void registerTexture(MapTile tile, Path path) {
        Identifier identifier = Identifier.fromNamespaceAndPath("niamod", "dynamic_maps/" + tile.md5().toLowerCase(Locale.ROOT));
        try (InputStream input = Files.newInputStream(path)) {
            NativeImage image = NativeImage.read(input);
            int width = image.getWidth();
            int height = image.getHeight();
            DynamicTexture texture = new DynamicTexture(() -> "Wynntils map tile " + tile.name(), image);
            Minecraft.getInstance().getTextureManager().register(identifier, texture);
            synchronized (lock) {
                TileTexture previous = textures.get(tile.key());
                if (previous != null && !previous.identifier().equals(identifier)) {
                    Minecraft.getInstance().getTextureManager().release(previous.identifier());
                }
                textures.put(tile.key(), new TileTexture(tile.md5(), identifier, width, height));
                loadingTiles.remove(tile.key());
            }
        } catch (Exception e) {
            synchronized (lock) {
                loadingTiles.remove(tile.key());
            }
            NiamodClient.LOGGER.warn("Failed to register Wynntils map tile {}", tile.name(), e);
        }
    }

    private Path cachedTilePath(MapTile tile) {
        try {
            Path target = cacheRoot().resolve(tile.path().replace('/', java.io.File.separatorChar)).normalize();
            Path cacheRoot = cacheRoot().normalize();
            if (!target.startsWith(cacheRoot)) {
                throw new IllegalArgumentException("Invalid map tile path: " + tile.path());
            }

            if (Files.isRegularFile(target) && tile.md5().equalsIgnoreCase(md5Hex(target))) {
                return target;
            }

            byte[] bytes = WebUtils.queryBytesAsync(tile.url()).join();
            String actualHash = md5Hex(bytes);
            if (!tile.md5().equalsIgnoreCase(actualHash)) {
                throw new IllegalStateException("MD5 mismatch for " + tile.name() + ": expected " + tile.md5() + ", got " + actualHash);
            }

            Files.createDirectories(target.getParent());
            Path temp = Files.createTempFile(target.getParent(), "map-tile-", ".tmp");
            Files.write(temp, bytes);
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            return target;
        } catch (Exception e) {
            throw new RuntimeException("Failed to cache map tile " + tile.name(), e);
        }
    }

    private Path cacheRoot() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("nyahmod").resolve("map-cache");
    }

    private List<MapTile> parseTiles(String body) {
        try {
            List<MapTile> parsed = GSON.fromJson(body, TILE_LIST_TYPE);
            if (parsed == null || parsed.isEmpty()) {
                return List.of();
            }
            List<MapTile> valid = new ArrayList<>();
            for (MapTile tile : parsed) {
                if (tile != null && tile.valid()) {
                    valid.add(tile);
                }
            }
            return List.copyOf(valid);
        } catch (Exception e) {
            NiamodClient.LOGGER.warn("Failed to parse Wynntils map manifest", e);
            return List.of();
        }
    }

    private String md5Hex(Path path) {
        try {
            return md5Hex(Files.readAllBytes(path));
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash " + path, e);
        }
    }

    private String md5Hex(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("MD5").digest(bytes);
            StringBuilder builder = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                builder.append(String.format(Locale.ROOT, "%02x", value & 0xFF));
            }
            return builder.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate MD5", e);
        }
    }

    public record MapTile(String name, String url, String path, int x1, int z1, int x2, int z2, String md5) {
        private boolean valid() {
            return name != null && !name.isBlank()
                    && url != null && !url.isBlank()
                    && path != null && !path.isBlank()
                    && md5 != null && !md5.isBlank()
                    && x2 >= x1
                    && z2 >= z1;
        }

        public String key() {
            return path.toLowerCase(Locale.ROOT);
        }

        public int worldWidth() {
            return Math.max(1, x2 - x1 + 1);
        }

        public int worldHeight() {
            return Math.max(1, z2 - z1 + 1);
        }
    }

    private record TileTexture(String md5, Identifier identifier, int width, int height) {
        private boolean readyFor(MapTile tile) {
            return tile != null && md5.equalsIgnoreCase(tile.md5());
        }
    }
}