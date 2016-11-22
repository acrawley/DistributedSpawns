package net.andrewcr.minecraft.plugin.DistributedSpawns.internal.model;

import lombok.Getter;
import net.andrewcr.minecraft.plugin.BasePluginLib.config.ConfigurationFileBase;
import net.andrewcr.minecraft.plugin.DistributedSpawns.internal.Plugin;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.HashMap;
import java.util.Map;

public class ConfigStore extends ConfigurationFileBase {
    //region Private Fields

    private static final String CONFIGURATION_VERSION_KEY = "ConfigurationVersion";
    private static final String ENABLE_DYNMAP_INTEGRATION_KEY = "EnableDynmapIntegration";
    private static final String SPAWN_CONFIGS_KEY = "SpawnConfigs";

    @Getter private boolean dynmapIntegrationEnabled = true;

    private final Map<String, DistributedSpawnWorldConfig> worldConfigs;

    //endregion

    //region Constructor

    public ConfigStore() {
        super(Plugin.getInstance());

        this.worldConfigs = new HashMap<>();
    }

    //endregion

    //region Singleton

    public static ConfigStore getInstance() {
        return Plugin.getInstance().getConfigStore();
    }

    //endregion

    //region Serialization

    @Override
    protected String getFileName() {
        return "config.yml";
    }

    @Override
    protected void loadCore(YamlConfiguration configuration) {
        String version = configuration.getString(CONFIGURATION_VERSION_KEY);
        switch (version) {
            case "1.0":
                this.loadV1_0Config(configuration);
                return;

            default:
                Plugin.getInstance().getLogger().severe("Unknown DistributedSpawn configuration version '" + version + "'!");
        }
    }

    private void loadV1_0Config(YamlConfiguration config) {
        this.dynmapIntegrationEnabled = config.getBoolean(ENABLE_DYNMAP_INTEGRATION_KEY, this.dynmapIntegrationEnabled);

        ConfigurationSection spawnConfigs = config.getConfigurationSection(SPAWN_CONFIGS_KEY);
        if (spawnConfigs != null) {
            for (String world : spawnConfigs.getKeys(false)) {
                this.worldConfigs.put(world, DistributedSpawnWorldConfig.loadFrom(spawnConfigs.getConfigurationSection(world)));
            }
        }

        Plugin.getInstance().getLogger().info("Loaded player spawn settings for " + this.worldConfigs.size() + " world(s)!");
    }

    @Override
    protected void saveCore(YamlConfiguration configuration) {
        configuration.set(CONFIGURATION_VERSION_KEY, "1.0");
        configuration.set(ENABLE_DYNMAP_INTEGRATION_KEY, this.dynmapIntegrationEnabled);

        ConfigurationSection spawnConfigs = configuration.createSection(SPAWN_CONFIGS_KEY);
        for (DistributedSpawnWorldConfig worldConfig : this.worldConfigs.values()) {
            worldConfig.save(spawnConfigs);
        }

        Plugin.getInstance().getLogger().info("Saved player spawn settings for " + this.worldConfigs.size() + " world(s)!");
    }

    //endregion

    //region Public API

    public DistributedSpawnWorldConfig getWorldConfig(World world, boolean shouldCreate) {
        if (this.worldConfigs.containsKey(world.getName())) {
            return this.worldConfigs.get(world.getName());
        }

        if (shouldCreate) {
            DistributedSpawnWorldConfig newConfig = new DistributedSpawnWorldConfig(world);

            this.worldConfigs.put(world.getName(), newConfig);

            this.notifyChanged();

            return newConfig;
        }

        return null;
    }

    //endregion
}
