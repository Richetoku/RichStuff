package com.richetoku.richstuff.rikumimita.ai.schematic;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.richetoku.richstuff.RichStuff;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.Nullable;

/**
 * Data-driven schematic registry for Rikumi's block-at-a-time builder.
 *
 * <p>Supported sources are built-in templates, datapack JSON under
 * {@code data/<namespace>/rikumi_schematics}, vanilla/Create-style structure NBT and compressed
 * Structurize/MineColonies blueprint files that expose a palette plus block-position list. External
 * files are discovered in world {@code schematics}, {@code create/schematics}, {@code blueprints},
 * and {@code structurize/blueprints} folders.</p>
 */
public final class RikumiSchematicRegistry {
    private static final Map<ResourceLocation, RikumiSchematic> SCHEMATICS = new LinkedHashMap<>();

    static {
        register(RikumiStarterHouse.create());
    }

    private RikumiSchematicRegistry() {}

    public static synchronized void reload(MinecraftServer server) {
        SCHEMATICS.clear();
        register(RikumiStarterHouse.create());
        loadDatapackJson(server);
        loadExternalFiles(server);
        RichStuff.LOGGER.info("Rikumi schematic registry loaded {} template(s).", SCHEMATICS.size());
    }

    public static synchronized void register(RikumiSchematic schematic) {
        if (schematic != null && !schematic.placements().isEmpty()) SCHEMATICS.put(schematic.id(), schematic);
    }

    public static synchronized Optional<RikumiSchematic> get(ResourceLocation id) {
        return Optional.ofNullable(SCHEMATICS.get(id));
    }

    public static synchronized List<RikumiSchematic> all() {
        return List.copyOf(SCHEMATICS.values());
    }

    private static void loadDatapackJson(MinecraftServer server) {
        try {
            Map<ResourceLocation, net.minecraft.server.packs.resources.Resource> resources =
                    server.getResourceManager().listResources("rikumi_schematics",
                            id -> id.getPath().endsWith(".json"));
            resources.forEach((id, resource) -> {
                try (var reader = resource.openAsReader()) {
                    JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                    ResourceLocation schematicId = ResourceLocation.fromNamespaceAndPath(id.getNamespace(),
                            id.getPath().substring("rikumi_schematics/".length(), id.getPath().length() - 5));
                    RikumiSchematic parsed = parseJson(schematicId, root, "datapack_json");
                    register(parsed);
                } catch (Exception exception) {
                    RichStuff.LOGGER.warn("Could not load Rikumi schematic {}: {}", id, exception.getMessage());
                }
            });
        } catch (RuntimeException exception) {
            RichStuff.LOGGER.warn("Could not enumerate Rikumi datapack schematics: {}", exception.getMessage());
        }
    }

    private static RikumiSchematic parseJson(ResourceLocation id, JsonObject root, String source) {
        String name = root.has("name") ? root.get("name").getAsString() : title(id.getPath());
        JsonArray blocks = root.getAsJsonArray("blocks");
        if (blocks == null) throw new IllegalArgumentException("Missing blocks array");
        List<RikumiSchematic.Placement> placements = new ArrayList<>();
        for (JsonElement element : blocks) {
            JsonObject block = element.getAsJsonObject();
            ResourceLocation blockId = ResourceLocation.tryParse(block.get("block").getAsString());
            if (blockId == null) continue;
            BlockState state = BuiltInRegistries.BLOCK.get(blockId).defaultBlockState();
            if (state.isAir() && !blockId.equals(ResourceLocation.withDefaultNamespace("air"))) continue;
            if (block.has("properties")) state = applyProperties(state, block.getAsJsonObject("properties"));
            boolean consume = !block.has("consume") || block.get("consume").getAsBoolean();
            placements.add(new RikumiSchematic.Placement(new BlockPos(
                    block.get("x").getAsInt(), block.get("y").getAsInt(), block.get("z").getAsInt()), state, consume));
        }
        sort(placements);
        return new RikumiSchematic(id, name, source, placements);
    }

    private static void loadExternalFiles(MinecraftServer server) {
        Path root = server.getWorldPath(LevelResource.ROOT);
        for (Path folder : List.of(root.resolve("schematics"), root.resolve("create/schematics"),
                root.resolve("blueprints"), root.resolve("structurize/blueprints"),
                root.resolve("minecolonies/blueprints"))) {
            if (!Files.isDirectory(folder)) continue;
            try (Stream<Path> stream = Files.walk(folder, 8)) {
                stream.filter(Files::isRegularFile).filter(RikumiSchematicRegistry::supportedFile)
                        .forEach(path -> loadExternalFile(root, path));
            } catch (Exception exception) {
                RichStuff.LOGGER.warn("Could not scan Rikumi schematic folder {}: {}", folder, exception.getMessage());
            }
        }
    }

    private static boolean supportedFile(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".nbt") || name.endsWith(".schematic") || name.endsWith(".blueprint");
    }

    private static void loadExternalFile(Path worldRoot, Path path) {
        try {
            CompoundTag root = readCompressedNbt(path);
            RikumiSchematic schematic = parseNbt(path, worldRoot, root);
            if (schematic != null) register(schematic);
        } catch (Exception exception) {
            RichStuff.LOGGER.debug("Ignoring unsupported schematic {}: {}", path, exception.getMessage());
        }
    }

    private static CompoundTag readCompressedNbt(Path path) throws Exception {
        Class<?> nbtIo = Class.forName("net.minecraft.nbt.NbtIo");
        Object accounter = null;
        Class<?> accounterType = Class.forName("net.minecraft.nbt.NbtAccounter");
        for (String methodName : List.of("unlimitedHeap", "unlimitedHeap")) {
            try {
                accounter = accounterType.getMethod(methodName).invoke(null);
                break;
            } catch (ReflectiveOperationException ignored) { }
        }
        for (Method method : nbtIo.getMethods()) {
            if (!Modifier.isStatic(method.getModifiers()) || !method.getName().equals("readCompressed")) continue;
            Class<?>[] parameters = method.getParameterTypes();
            Object result;
            if (parameters.length == 2 && parameters[0] == Path.class && accounter != null
                    && parameters[1].isInstance(accounter)) {
                result = method.invoke(null, path, accounter);
            } else if (parameters.length == 1 && parameters[0] == Path.class) {
                result = method.invoke(null, path);
            } else if (parameters.length == 2 && InputStream.class.isAssignableFrom(parameters[0])
                    && accounter != null && parameters[1].isInstance(accounter)) {
                try (InputStream input = Files.newInputStream(path)) {
                    result = method.invoke(null, input, accounter);
                }
            } else if (parameters.length == 1 && InputStream.class.isAssignableFrom(parameters[0])) {
                try (InputStream input = Files.newInputStream(path)) {
                    result = method.invoke(null, input);
                }
            } else continue;
            if (result instanceof CompoundTag compound) return compound;
        }
        throw new IllegalStateException("No compatible NbtIo.readCompressed method");
    }

    private static RikumiSchematic parseNbt(Path path, Path worldRoot, CompoundTag root) {
        CompoundTag structure = findStructure(root, 0);
        if (structure == null) return null;
        ListTag paletteTag = structure.getList("palette", Tag.TAG_COMPOUND);
        if (paletteTag.isEmpty()) return null;

        List<BlockState> palette = new ArrayList<>();
        for (int index = 0; index < paletteTag.size(); index++) palette.add(parsePaletteState(paletteTag.getCompound(index)));
        List<RikumiSchematic.Placement> placements = structure.contains("blocks", Tag.TAG_INT_ARRAY)
                ? parseStructurizeBlocks(structure, palette)
                : parseStructureBlocks(structure, palette);
        if (placements.isEmpty()) return null;
        sort(placements);
        String file = path.getFileName().toString();
        int dot = file.lastIndexOf('.');
        String clean = (dot > 0 ? file.substring(0, dot) : file).replaceAll("[^a-zA-Z0-9_./-]", "_");
        String source = file.toLowerCase(Locale.ROOT).endsWith(".blueprint") ? "minecolonies_structurize"
                : file.toLowerCase(Locale.ROOT).endsWith(".schematic") ? "create_or_legacy_nbt" : "structure_nbt";
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("external", sanitizePath(clean));
        return new RikumiSchematic(id, title(clean), source, placements);
    }


    /** Vanilla/Create structure format: one compound per placed block. */
    private static List<RikumiSchematic.Placement> parseStructureBlocks(CompoundTag structure,
                                                                         List<BlockState> palette) {
        List<RikumiSchematic.Placement> placements = new ArrayList<>();
        ListTag blocksTag = structure.getList("blocks", Tag.TAG_COMPOUND);
        for (int index = 0; index < blocksTag.size(); index++) {
            CompoundTag block = blocksTag.getCompound(index);
            int stateIndex = block.getInt("state");
            if (stateIndex < 0 || stateIndex >= palette.size()) continue;
            ListTag pos = block.getList("pos", Tag.TAG_INT);
            if (pos.size() < 3) continue;
            BlockState state = palette.get(stateIndex);
            if (state.isAir()) continue;
            placements.add(new RikumiSchematic.Placement(
                    new BlockPos(pos.getInt(0), pos.getInt(1), pos.getInt(2)), state, true));
        }
        return placements;
    }

    /** Structurize/MineColonies Blueprint V1 format: two packed palette shorts per int, Y/Z/X order. */
    private static List<RikumiSchematic.Placement> parseStructurizeBlocks(CompoundTag structure,
                                                                           List<BlockState> palette) {
        int sizeX = Math.max(0, structure.getShort("size_x"));
        int sizeY = Math.max(0, structure.getShort("size_y"));
        int sizeZ = Math.max(0, structure.getShort("size_z"));
        if (sizeX == 0 || sizeY == 0 || sizeZ == 0) return List.of();
        long volume = (long) sizeX * sizeY * sizeZ;
        if (volume > 16_777_216L) throw new IllegalArgumentException("Blueprint exceeds safe block limit");
        int[] packed = structure.getIntArray("blocks");
        int expectedPacked = (int) ((volume + 1L) / 2L);
        if (packed.length < expectedPacked) return List.of();

        List<RikumiSchematic.Placement> placements = new ArrayList<>((int) Math.min(volume, 65_536L));
        int flatIndex = 0;
        for (int y = 0; y < sizeY; y++) {
            for (int z = 0; z < sizeZ; z++) {
                for (int x = 0; x < sizeX; x++, flatIndex++) {
                    int value = packed[flatIndex / 2];
                    int stateIndex = (flatIndex & 1) == 0 ? (short) (value >> 16) : (short) value;
                    if (stateIndex < 0 || stateIndex >= palette.size()) continue;
                    BlockState state = palette.get(stateIndex);
                    if (state.isAir()) continue;
                    placements.add(new RikumiSchematic.Placement(new BlockPos(x, y, z), state, true));
                }
            }
        }
        return placements;
    }

    @Nullable
    private static CompoundTag findStructure(CompoundTag root, int depth) {
        if (root == null || depth > 4) return null;
        if (root.contains("palette", Tag.TAG_LIST)
                && (root.contains("blocks", Tag.TAG_LIST) || root.contains("blocks", Tag.TAG_INT_ARRAY))) return root;
        for (String key : List.of("structure", "template", "blueprint", "data", "tag")) {
            if (root.contains(key, Tag.TAG_COMPOUND)) {
                CompoundTag found = findStructure(root.getCompound(key), depth + 1);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static BlockState parsePaletteState(CompoundTag tag) {
        ResourceLocation id = ResourceLocation.tryParse(tag.getString("Name"));
        if (id == null) id = ResourceLocation.tryParse(tag.getString("name"));
        if (id == null) return Blocks.AIR.defaultBlockState();
        BlockState state = BuiltInRegistries.BLOCK.get(id).defaultBlockState();
        if (tag.contains("Properties", Tag.TAG_COMPOUND)) state = applyProperties(state, tag.getCompound("Properties"));
        return state;
    }

    private static BlockState applyProperties(BlockState state, JsonObject properties) {
        for (Map.Entry<String, JsonElement> entry : properties.entrySet()) {
            Property<?> property = state.getBlock().getStateDefinition().getProperty(entry.getKey());
            if (property != null) state = applyProperty(state, property, entry.getValue().getAsString());
        }
        return state;
    }

    private static BlockState applyProperties(BlockState state, CompoundTag properties) {
        for (String key : properties.getAllKeys()) {
            Property<?> property = state.getBlock().getStateDefinition().getProperty(key);
            if (property != null) state = applyProperty(state, property, properties.getString(key));
        }
        return state;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockState applyProperty(BlockState state, Property property, String value) {
        Optional parsed = property.getValue(value);
        return parsed.isPresent() ? state.setValue(property, (Comparable) parsed.get()) : state;
    }

    private static void sort(List<RikumiSchematic.Placement> placements) {
        placements.sort(Comparator.comparingInt((RikumiSchematic.Placement p) -> p.offset().getY())
                .thenComparingInt(p -> p.offset().getZ()).thenComparingInt(p -> p.offset().getX()));
    }

    private static String sanitizePath(String value) {
        String path = value.toLowerCase(Locale.ROOT).replace('\\', '/').replaceAll("[^a-z0-9_./-]", "_");
        while (path.startsWith("/")) path = path.substring(1);
        return path.isBlank() ? "schematic" : path;
    }

    private static String title(String value) {
        String[] parts = value.replace('/', ' ').replace('_', ' ').split("\\s+");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) continue;
            if (!result.isEmpty()) result.append(' ');
            result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return result.toString();
    }
}
