package net.andrewcr.minecraft.plugin.DistributedSpawns.internal.integration.dynmap;

import net.andrewcr.minecraft.plugin.BasePluginLib.util.StringUtil;
import net.andrewcr.minecraft.plugin.DistributedSpawns.internal.Plugin;
import net.andrewcr.minecraft.plugin.DistributedSpawns.internal.model.ConfigStore;
import net.andrewcr.minecraft.plugin.DistributedSpawns.internal.model.DistributedSpawnWorldConfig;
import net.andrewcr.minecraft.plugin.DistributedSpawns.internal.model.PlayerSpawn;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;
import org.dynmap.DynmapAPI;
import org.dynmap.markers.Marker;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerIcon;
import org.dynmap.markers.MarkerSet;

import java.util.List;
import java.util.stream.Collectors;

public class DynmapIntegration {
    //region Private Fields

    private static final String SPAWN_MARKER_SET_ID = "DistributedSpawns.Markers.1.0";

    private MarkerAPI markerApi = null;
    private MarkerSet spawnMarkerSet;

    //endregion

    //region Singleton Instance

    public static DynmapIntegration getInstance() {
        return Plugin.getInstance().getDynmapIntegration();
    }

    //endregion

    //region Constructor

    public DynmapIntegration() {
        if (!ConfigStore.getInstance().isDynmapIntegrationEnabled()) {
            Plugin.getInstance().getLogger().info("Dynmap integration disabled in config.yml!");
            return;
        }

        org.bukkit.plugin.Plugin dynmapPlugin = Bukkit.getPluginManager().getPlugin("dynmap");

        if (dynmapPlugin == null) {
            Plugin.getInstance().getLogger().info("Dynmap integration disabled - failed to get plugin instance!");
            return;
        }

        if (!dynmapPlugin.isEnabled()) {
            Plugin.getInstance().getLogger().info("Dynmap integration disabled - Dynmap plugin is not enabled!");
            return;
        }

        if (!(dynmapPlugin instanceof DynmapAPI)) {
            Plugin.getInstance().getLogger().info("Dynmap integration disabled - plugin instance not of expected type!");
            return;
        }

        DynmapAPI dynmapApi = (DynmapAPI) dynmapPlugin;

        if (!dynmapApi.markerAPIInitialized()) {
            Plugin.getInstance().getLogger().info("Dynmap integration disabled - Marker API is not ready!");
            return;
        }

        this.markerApi = dynmapApi.getMarkerAPI();
        if (this.markerApi == null) {
            Plugin.getInstance().getLogger().info("Dynmap integration disabled - failed to get Marker API!");
            return;
        }

        Plugin.getInstance().getLogger().info("Dynmap integration enabled - Dynmap version: " + dynmapApi.getDynmapVersion()
            + ", Dynmap-Core version: " + dynmapApi.getDynmapCoreVersion());

        for (World world : Bukkit.getWorlds()) {
            this.enable(world, true);
        }
    }

    // endregion

    //region Public API

    public void notifySpawnChanged(PlayerSpawn spawn) {
        if (this.markerApi == null || spawn.getSpawnLocation() == null) {
            return;
        }

        MarkerSet markerSet = this.getSpawnMarkerSet();
        if (markerSet == null) {
            return;
        }

        String markerId = this.getSpawnMarkerId(spawn);
        Location spawnLoc = spawn.getSpawnLocation();

        Marker marker = markerSet.findMarker(markerId);
        if (marker == null) {
            String markerTitle;
            if (spawn.getPlayerId().equals(DistributedSpawnWorldConfig.GLOBAL_SPAWN_ID)) {
                markerTitle = "Default spawn point";
            } else {
                OfflinePlayer player = Bukkit.getOfflinePlayer(spawn.getPlayerId());
                if (player != null && !StringUtil.isNullOrEmpty(player.getName())) {
                    markerTitle = player.getName() + "'s spawn point";
                }
                else {
                    markerTitle = "Unknown player's spawn point";
                }
            }

            // Create marker
            markerSet.createMarker(
                markerId,
                markerTitle,
                spawnLoc.getWorld().getName(),
                spawnLoc.getX(),
                spawnLoc.getY(),
                spawnLoc.getZ(),
                markerApi.getMarkerIcon(MarkerIcon.WORLD),
                false);
        } else {
            if (spawn.getPlayerId().equals(DistributedSpawnWorldConfig.GLOBAL_SPAWN_ID)) {
                // Remove the default spawn marker on the next tick, in case we're called before Dynmap
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        removeDefaultSpawnMarker(spawn.getSpawnLocation().getWorld());
                    }
                }.runTaskLater(Plugin.getInstance(), 1);
            }

            // Update location
            marker.setLocation(
                spawnLoc.getWorld().getName(),
                spawnLoc.getX(),
                spawnLoc.getY(),
                spawnLoc.getZ());
        }
    }

    public void enable(World world, boolean enable) {
        if (this.markerApi == null) {
            return;
        }

        if (enable) {
            // Enable - create markers for all spawns in the specified world
            DistributedSpawnWorldConfig config = ConfigStore.getInstance().getWorldConfig(world, false);
            if (config == null || !config.isPerUserSpawnEnabled()) {
                return;
            }

            for (PlayerSpawn spawn : config.getSpawns()) {
                this.notifySpawnChanged(spawn);
            }

            // Remove the default spawn marker in favor of our own
            this.removeDefaultSpawnMarker(world);

        } else {
            // Disable - remove all markers in the specified world
            MarkerSet markerSet = this.getSpawnMarkerSet();
            if (markerSet == null) {
                return;
            }

            List<Marker> markersInWorld = markerSet.getMarkers().stream()
                .filter(m -> StringUtil.equals(m.getWorld(), world.getName()))
                .collect(Collectors.toList());

            for (Marker marker : markersInWorld) {
                marker.deleteMarker();
            }

            // Put the default spawn marker back
            MarkerSet defaultMarkers = this.markerApi.getMarkerSet("markers");
            if (defaultMarkers != null) {
                defaultMarkers.createMarker(
                    "_spawn_" + world.getName(),
                    "Spawn",
                    world.getName(),
                    world.getSpawnLocation().getX(),
                    world.getSpawnLocation().getY(),
                    world.getSpawnLocation().getZ(),
                    markerApi.getMarkerIcon(MarkerIcon.WORLD),
                    false);
            }
        }
    }

    //endregion

    //region Helpers

    private MarkerSet getSpawnMarkerSet() {
        if (this.spawnMarkerSet == null) {
            this.spawnMarkerSet = markerApi.getMarkerSet(SPAWN_MARKER_SET_ID);

            if (this.spawnMarkerSet == null) {
                this.spawnMarkerSet = markerApi.createMarkerSet(SPAWN_MARKER_SET_ID, "Player Spawns", null, false);
            }
        }

        return this.spawnMarkerSet;
    }

    private String getSpawnMarkerId(PlayerSpawn spawn) {
        return "spawn-" + spawn.getSpawnLocation().getWorld().getName() + "-" + spawn.getPlayerId().toString();
    }

    private void removeDefaultSpawnMarker(World world) {
        MarkerSet defaultMarkers = this.markerApi.getMarkerSet("markers");
        if (defaultMarkers != null) {
            Marker worldSpawnMarker = defaultMarkers.findMarker("_spawn_" + world.getName());
            if (worldSpawnMarker != null) {
                worldSpawnMarker.deleteMarker();
            }
        }
    }

    //endregion
}

