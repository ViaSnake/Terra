package com.dfsek.terra.fabric;

import com.dfsek.tectonic.loading.TypeRegistry;
import com.dfsek.terra.TerraWorld;
import com.dfsek.terra.api.GenericLoaders;
import com.dfsek.terra.api.gaea.lang.Language;
import com.dfsek.terra.api.generic.TerraPlugin;
import com.dfsek.terra.api.generic.inventory.ItemHandle;
import com.dfsek.terra.api.generic.world.World;
import com.dfsek.terra.api.generic.world.WorldHandle;
import com.dfsek.terra.api.generic.world.block.BlockData;
import com.dfsek.terra.api.generic.world.block.MaterialData;
import com.dfsek.terra.config.base.PluginConfig;
import com.dfsek.terra.config.lang.LangUtil;
import com.dfsek.terra.fabric.inventory.FabricItemHandle;
import com.dfsek.terra.fabric.mixin.GeneratorTypeAccessor;
import com.dfsek.terra.fabric.world.FabricBiome;
import com.dfsek.terra.fabric.world.FabricWorldHandle;
import com.dfsek.terra.fabric.world.TerraBiomeSource;
import com.dfsek.terra.fabric.world.generator.FabricChunkGeneratorWrapper;
import com.dfsek.terra.fabric.world.generator.TerraChunkGeneratorCodec;
import com.dfsek.terra.registry.ConfigRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.world.GeneratorType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.BuiltinRegistries;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TerraFabricPlugin implements TerraPlugin, ModInitializer {
    private static TerraFabricPlugin instance;
    private final TerraChunkGeneratorCodec chunkGeneratorCodec = new TerraChunkGeneratorCodec(this);

    public static TerraFabricPlugin getInstance() {
        return instance;
    }

    private final GenericLoaders genericLoaders = new GenericLoaders(this);
    private final Logger logger = Logger.getLogger("Terra");
    private final ItemHandle itemHandle = new FabricItemHandle();
    private final WorldHandle worldHandle = new FabricWorldHandle();
    private final ConfigRegistry registry = new ConfigRegistry();
    private File config;

    {
        logger.setLevel(Level.INFO);
    }

    @Override
    public WorldHandle getWorldHandle() {
        return worldHandle;
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public TerraWorld getWorld(World world) {
        return new TerraWorld(world, getRegistry().get("DEFAULT"), this);
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public PluginConfig getTerraConfig() {
        return null;
    }

    @Override
    public File getDataFolder() {
        return config;
    }

    @Override
    public boolean isDebug() {
        return false;
    }

    @Override
    public Language getLanguage() {
        try {
            return new Language(new File(getDataFolder(), "lang/en_us/yml"));
        } catch(IOException e) {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public ConfigRegistry getRegistry() {
        return registry;
    }

    @Override
    public void reload() {

    }

    @Override
    public ItemHandle getItemHandle() {
        return itemHandle;
    }

    @Override
    public void register(TypeRegistry registry) {
        genericLoaders.register(registry);
        registry
                .registerLoader(BlockData.class, (t, o, l) -> worldHandle.createBlockData((String) o))
                .registerLoader(MaterialData.class, (t, o, l) -> worldHandle.createMaterialData((String) o))
                .registerLoader(com.dfsek.terra.api.generic.world.Biome.class, (t, o, l) -> {
                    String id = (String) o;
                    if(!id.contains(":")) id = "minecraft:" + id.toLowerCase();
                    Identifier identifier = new Identifier(id);
                    Biome biome = BuiltinRegistries.BIOME.get(identifier);
                    return new FabricBiome(biome);
                });
    }

    @Override
    public void onInitialize() {
        instance = this;
        config = new File(FabricLoader.getInstance().getConfigDir().toFile(), "Terra");
        LangUtil.load("en_us", this);
        logger.info("Initializing Terra...");
        registry.loadAll(this);

        if(FabricLoader.getInstance().getEnvironmentType().equals(EnvType.CLIENT)) {
            GeneratorTypeAccessor.getValues().add(new GeneratorType("terra") {
                @Override
                protected ChunkGenerator getChunkGenerator(Registry<Biome> biomeRegistry, Registry<ChunkGeneratorSettings> chunkGeneratorSettingsRegistry, long seed) {
                    return new FabricChunkGeneratorWrapper(new TerraBiomeSource(biomeRegistry, seed), seed);
                }
            });
        }
        Registry.register(Registry.CHUNK_GENERATOR, new Identifier("terra:terra"), FabricChunkGeneratorWrapper.CODEC);
        Registry.register(Registry.BIOME_SOURCE, new Identifier("terra:terra"), TerraBiomeSource.CODEC);
    }

    public TerraChunkGeneratorCodec getChunkGeneratorCodec() {
        return chunkGeneratorCodec;
    }
}
