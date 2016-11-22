package net.andrewcr.minecraft.plugin.DistributedSpawns.internal.model;

import lombok.Getter;
import lombok.Synchronized;
import net.andrewcr.minecraft.plugin.BasePluginLib.distributions.poissondisc.IPoissonDiscPoint;
import net.andrewcr.minecraft.plugin.BasePluginLib.util.LocationUtil;
import net.andrewcr.minecraft.plugin.DistributedSpawns.internal.integration.dynmap.DynmapIntegration;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.UUID;

public class PlayerSpawn implements IPoissonDiscPoint {
    //region Private Fields

    private static final String SPAWN_LOCATION_KEY = "SpawnLocation";
    private static final String IS_SEARCHED_KEY = "IsSearched";

    private final Object configLock = ConfigStore.getInstance().getSyncObj();

    private Location spawnLocation;
    @Getter private final UUID playerId;
    private boolean isSearched;

    //endregion

    //region Constructors

    PlayerSpawn(UUID playerId) {
        this(playerId, null, false);
    }

    private PlayerSpawn(UUID playerId, Location spawnLocation, boolean isSearched) {
        this.playerId = playerId;
        this.spawnLocation = spawnLocation;
        this.isSearched = isSearched;
    }

    //endregion

    //region Serialization

    static PlayerSpawn loadFrom(World world, ConfigurationSection playerSpawn) {
        if (!playerSpawn.contains(SPAWN_LOCATION_KEY)) {
            return null;
        }

        Location location = LocationUtil.locationFromString(playerSpawn.getString(SPAWN_LOCATION_KEY));
        if (location == null) {
            return null;
        }

        location.setWorld(world);

        boolean isSearched = false;
        if (playerSpawn.contains(IS_SEARCHED_KEY)) {
            isSearched = playerSpawn.getBoolean(IS_SEARCHED_KEY);
        }

        return new PlayerSpawn(UUID.fromString(playerSpawn.getName()), location, isSearched);
    }

    @Synchronized("configLock")
    void save(ConfigurationSection playerSpawns) {
        ConfigurationSection playerSpawn = playerSpawns.createSection(this.playerId.toString());

        playerSpawn.set(SPAWN_LOCATION_KEY, LocationUtil.locationToDecimalString(this.spawnLocation, false, true, false));
        playerSpawn.set(IS_SEARCHED_KEY, this.isSearched);
    }

    //endregion

    //region Getters / Setters

    public Location getSpawnLocation() {
        if (this.spawnLocation == null) {
            return null;
        }

        return this.spawnLocation.clone();
    }

    @Synchronized("configLock")
    public void setSpawnLocation(Location location) {
        this.spawnLocation = location;

        DynmapIntegration.getInstance().notifySpawnChanged(this);
        ConfigStore.getInstance().notifyChanged();
    }

    //endregion

    //region IPoissonDiscPoint Implementation

    public boolean getIsSearched() {
        return this.isSearched;
    }

    public void setIsSearched(boolean value) {
        this.isSearched = value;
        ConfigStore.getInstance().notifyChanged();
    }

    @Override
    public int getX() {
        return this.spawnLocation.getBlockX();
    }

    @Override
    public int getY() {
        return this.spawnLocation.getBlockZ();
    }

    //endregion
}
