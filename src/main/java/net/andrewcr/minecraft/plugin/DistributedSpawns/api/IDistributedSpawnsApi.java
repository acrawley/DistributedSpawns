package net.andrewcr.minecraft.plugin.DistributedSpawns.api;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public interface IDistributedSpawnsApi {
    /**
     * Gets the player's spawn location in the given world.
     * @param world The world
     * @param player The player
     * @return The location where the player will spawn in the world
     */
    Location getPlayerSpawnLocation(World world, Player player);

    /**
     * Sets the location where the player will spawn in the given world.  If per-player spawn points are
     * not enabled in the world, this has no effect.
     * @param world The world
     * @param player The player
     * @param spawnLocation The desired spawn location
     */
    void setPlayerSpawnLocation(World world, Player player, Location spawnLocation);
}
