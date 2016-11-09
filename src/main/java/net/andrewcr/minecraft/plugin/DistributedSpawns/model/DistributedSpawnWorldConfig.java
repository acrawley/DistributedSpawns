package net.andrewcr.minecraft.plugin.DistributedSpawns.model;

import lombok.Getter;
import lombok.Synchronized;
import net.andrewcr.minecraft.plugin.BasePluginLib.distributions.poissondisc.PoissonDiscDistribution;
import net.andrewcr.minecraft.plugin.BasePluginLib.util.LocationUtil;
import net.andrewcr.minecraft.plugin.BasePluginLib.util.RandomUtil;
import net.andrewcr.minecraft.plugin.DistributedSpawns.Plugin;
import net.andrewcr.minecraft.plugin.DistributedSpawns.integration.dynmap.DynmapIntegration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DistributedSpawnWorldConfig {
    //region Private Fields

    public static final UUID GLOBAL_SPAWN_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private static final String DISTRIBUTED_SPAWN_ENABLED_KEY = "DistributedSpawnEnabled";
    private static final String PER_USER_SPAWN_ENABLED_KEY = "PerUserSpawnEnabled";
    private static final String MINIMUM_SPAWN_SPACING_KEY = "MinimumSpawnSpacing";
    private static final String PLAYER_SPAWNS_KEY = "PlayerSpawns";

    private final Object configLock = ConfigStore.getInstance().getSyncObj();

    @Getter private boolean perUserSpawnEnabled;
    @Getter private boolean distributedSpawnEnabled;
    @Getter private int minimumSpawnSpacing;
    private World world;

    private final Map<UUID, PlayerSpawn> playerSpawns;
    private PoissonDiscDistribution poissonDiscDistribution;

    //endregion

    //region Constructors

    DistributedSpawnWorldConfig(World world) {
        this(world, false, false, 0);

        this.setGlobalSpawnLocation(this.world.getSpawnLocation());
    }

    private DistributedSpawnWorldConfig(World world, boolean perUserSpawnEnabled, boolean distributedSpawnEnabled, int minimumSpawnSpacing) {
        this.world = world;
        this.perUserSpawnEnabled = perUserSpawnEnabled;
        this.distributedSpawnEnabled = distributedSpawnEnabled;
        this.minimumSpawnSpacing = minimumSpawnSpacing;

        this.playerSpawns = new HashMap<>();
        this.poissonDiscDistribution = new PoissonDiscDistribution(this.playerSpawns.values(), this.minimumSpawnSpacing);
    }

    //endregion

    //region Serialization

    static DistributedSpawnWorldConfig loadFrom(ConfigurationSection worldConfig) {
        boolean perUserSpawnEnabledValue = false;
        boolean distributedSpawnEnabledValue = false;
        int minimumSpawnSpacing = 0;
        World world = Bukkit.getWorld(worldConfig.getName());

        if (worldConfig.contains(PER_USER_SPAWN_ENABLED_KEY)) {
            perUserSpawnEnabledValue = worldConfig.getBoolean(PER_USER_SPAWN_ENABLED_KEY);
        }

        if (worldConfig.contains(DISTRIBUTED_SPAWN_ENABLED_KEY)) {
            distributedSpawnEnabledValue = worldConfig.getBoolean(DISTRIBUTED_SPAWN_ENABLED_KEY);
        }

        if (worldConfig.contains(MINIMUM_SPAWN_SPACING_KEY)) {
            minimumSpawnSpacing = worldConfig.getInt(MINIMUM_SPAWN_SPACING_KEY);
        }

        DistributedSpawnWorldConfig config = new DistributedSpawnWorldConfig(
            world, perUserSpawnEnabledValue, distributedSpawnEnabledValue, minimumSpawnSpacing);

        ConfigurationSection playerSpawns = worldConfig.getConfigurationSection(PLAYER_SPAWNS_KEY);
        if (playerSpawns != null) {
            for (String playerUuid : playerSpawns.getKeys(false)) {
                config.registerPlayerSpawn(PlayerSpawn.loadFrom(world, playerSpawns.getConfigurationSection(playerUuid)));
            }
        }

        return config;
    }

    @Synchronized("configLock")
    void save(ConfigurationSection worldConfigs) {
        ConfigurationSection worldConfig = worldConfigs.createSection(this.world.getName());

        worldConfig.set(PER_USER_SPAWN_ENABLED_KEY, this.perUserSpawnEnabled);
        worldConfig.set(DISTRIBUTED_SPAWN_ENABLED_KEY, this.distributedSpawnEnabled);

        if (this.minimumSpawnSpacing != 0) {
            worldConfig.set(MINIMUM_SPAWN_SPACING_KEY, this.minimumSpawnSpacing);
        }

        ConfigurationSection playerSpawnsSection = worldConfig.createSection(PLAYER_SPAWNS_KEY);
        for (PlayerSpawn spawn : this.playerSpawns.values()) {
            spawn.save(playerSpawnsSection);
        }
    }

    //endregion

    //region Getters / Setters

    @Synchronized("configLock")
    public void setPerUserSpawnEnabled(boolean value) {
        if (this.perUserSpawnEnabled == value) {
            return;
        }

        this.perUserSpawnEnabled = value;
        DynmapIntegration.getInstance().enable(this.world, value);

        ConfigStore.getInstance().notifyChanged();
    }

    @Synchronized("configLock")
    public void setDistributedSpawnEnabled(boolean value) {
        if (this.distributedSpawnEnabled == value) {
            return;
        }

        this.distributedSpawnEnabled = value;
        ConfigStore.getInstance().notifyChanged();
    }

    @Synchronized("configLock")
    public void setMinimumSpawnSpacing(int value) {
        if (this.minimumSpawnSpacing == value) {
            return;
        }

        this.minimumSpawnSpacing = value;
        this.poissonDiscDistribution.setSpacing(value);

        ConfigStore.getInstance().notifyChanged();
    }

    //endregion

    //region Public API

    public Location getPlayerSpawn(Player player) {
        if (this.perUserSpawnEnabled) {
            // Player has a spawn point defined
            PlayerSpawn spawn = this.getPlayerSpawn(player.getUniqueId(), false);
            if (spawn != null) {
                return spawn.getSpawnLocation();
            }
        }

        if (this.distributedSpawnEnabled) {
            // Player doesn't have a spawn point defined, but distributed spawns are enabled, so generate one now
            Point point = this.poissonDiscDistribution.getNextPoint();
            Location spawnLoc = this.getSafeSpawnLocation(this.world, point);

            PlayerSpawn spawn = this.getPlayerSpawn(player.getUniqueId(), true);
            spawn.setSpawnLocation(spawnLoc);

            this.poissonDiscDistribution.addPoint(spawn);

            Plugin.getInstance().getLogger().info("Generated new spawn location for player '" + player.getName() + "' in world '"
                + world.getName() + "' - (" + LocationUtil.locationToIntString(spawnLoc, false, false, false) + ")!");

            return spawnLoc;
        }

        // Player doesn't have a spawn point, use the global spawn for the world
        return this.world.getSpawnLocation();
    }

    public void setPlayerSpawn(Player player, Location location) {
        PlayerSpawn spawn = this.getPlayerSpawn(player.getUniqueId(), true);

        if (location != spawn.getSpawnLocation()) {
            spawn.setSpawnLocation(location);
            this.poissonDiscDistribution.notifyPointChanged(spawn);
        }
    }

    public void setGlobalSpawnLocation(Location location) {
        // This entry is used to maintain spacing around the global spawn when distributed spawning
        //  is enabled - the actual spawn point is always read from the world when necessary.
        PlayerSpawn globalSpawn = this.getPlayerSpawn(GLOBAL_SPAWN_ID, true);

        if (location != globalSpawn.getSpawnLocation()) {
            globalSpawn.setSpawnLocation(location);
            this.poissonDiscDistribution.notifyPointChanged(globalSpawn);
        }
    }

    public int getSpawnCount() {
        return this.playerSpawns.size();
    }

    public Iterable<PlayerSpawn> getSpawns() {
        return this.playerSpawns.values();
    }

    //endregion

    //region Helpers

    private Location getSafeSpawnLocation(World world, Point point) {
        // Do a random walk around the initial point looking for a valid spawn location - this is basically
        //  what vanilla does, plus some extra checks for air blocks
        int x = (int)point.getX();
        int z = (int)point.getY();

        for (int attempts = 0; attempts < 10000; attempts++) {
            Location location = LocationUtil.findSafeLocationInColumn(this.world, x, this.world.getSeaLevel(), z);
            if (location != null) {
                return location;
            }

            x += RandomUtil.getRandomInt(8) - RandomUtil.getRandomInt(8);
            z += RandomUtil.getRandomInt(8) - RandomUtil.getRandomInt(8);
        }

        // Give up and hope for the best
        Plugin.getInstance().getLogger().warning("Gave up trying to find a suitable spawn block, hold onto your butts...");
        return new Location(world, x, world.getHighestBlockYAt(x, z), z);
    }

    private PlayerSpawn getPlayerSpawn(UUID playerId, boolean create) {
        if (this.playerSpawns.containsKey(playerId)) {
            return this.playerSpawns.get(playerId);
        }

        if (create) {
            PlayerSpawn spawn = new PlayerSpawn(playerId);
            this.registerPlayerSpawn(spawn);

            return spawn;
        }

        return null;
    }

    private void registerPlayerSpawn(PlayerSpawn spawn) {
        // Not calling "notifyChanged" here, as player spawn is not valid until configured
        this.playerSpawns.put(spawn.getPlayerId(), spawn);
    }

    //endregion
}
