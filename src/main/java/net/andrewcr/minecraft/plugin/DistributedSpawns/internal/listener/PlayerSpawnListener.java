package net.andrewcr.minecraft.plugin.DistributedSpawns.internal.listener;

import net.andrewcr.minecraft.plugin.DistributedSpawns.internal.model.ConfigStore;
import net.andrewcr.minecraft.plugin.DistributedSpawns.internal.model.DistributedSpawnWorldConfig;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.world.SpawnChangeEvent;

public class PlayerSpawnListener implements Listener {
    //region Event Handlers

    @EventHandler(ignoreCancelled = true)
    private void onPlayerRespawn(PlayerRespawnEvent event) {
        if (event.isBedSpawn()) {
            // Ignore bed spawns
            return;
        }

        DistributedSpawnWorldConfig config = ConfigStore.getInstance().getWorldConfig(event.getRespawnLocation().getWorld(), false);
        if (config == null || !config.isPerUserSpawnEnabled()) {
            // No custom spawn
            return;
        }

        Location spawn = config.getPlayerSpawn(event.getPlayer());
        if (spawn == null) {
            // No spawn for player
            return;
        }

        // Move respawn to stored location
        event.setRespawnLocation(spawn);
    }

    @EventHandler
    private void onWorldSpawnpointChange(SpawnChangeEvent event) {
        DistributedSpawnWorldConfig config = ConfigStore.getInstance().getWorldConfig(event.getWorld(), false);

        if (config != null) {
            // Update the global spawn location in the config
            config.setGlobalSpawnLocation(event.getWorld().getSpawnLocation());
        }
    }

    //endregion
}
