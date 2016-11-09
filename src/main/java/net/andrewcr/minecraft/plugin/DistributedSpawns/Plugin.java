package net.andrewcr.minecraft.plugin.DistributedSpawns;

import lombok.Getter;
import net.andrewcr.minecraft.plugin.BasePluginLib.plugin.PluginBase;
import net.andrewcr.minecraft.plugin.BasePluginLib.util.Version;
import net.andrewcr.minecraft.plugin.DistributedSpawns.api.IDistributedSpawnsApi;
import net.andrewcr.minecraft.plugin.DistributedSpawns.command.DistributedSpawnCommand;
import net.andrewcr.minecraft.plugin.DistributedSpawns.command.DistributedSpawnInfoCommand;
import net.andrewcr.minecraft.plugin.DistributedSpawns.command.DistributedSpawnSetCommand;
import net.andrewcr.minecraft.plugin.DistributedSpawns.integration.dynmap.DynmapIntegration;
import net.andrewcr.minecraft.plugin.DistributedSpawns.listener.PlayerSpawnListener;
import net.andrewcr.minecraft.plugin.DistributedSpawns.model.ConfigStore;
import net.andrewcr.minecraft.plugin.DistributedSpawns.model.DistributedSpawnWorldConfig;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class Plugin extends PluginBase implements IDistributedSpawnsApi {
    //region Private Fields

    @Getter private static Plugin instance;

    @Getter private ConfigStore configStore;
    @Getter private DynmapIntegration dynmapIntegration;

    //endregion

    //region PluginBase Implementation

    @Override
    protected Version getRequiredBPLVersion() {
        return new Version(1, 2);
    }

    @Override
    protected void onEnableCore() {
        Plugin.instance = this;

        // Load config
        this.configStore = new ConfigStore();
        this.configStore.load();

        // Register commands
        this.registerCommand(new DistributedSpawnCommand());
        this.registerCommand(new DistributedSpawnSetCommand());
        this.registerCommand(new DistributedSpawnInfoCommand());

        // Register listeners
        this.registerListener(new PlayerSpawnListener());

        // Integration with other plugins
        this.dynmapIntegration = new DynmapIntegration();
    }

    @Override
    protected void onDisableCore() {
        this.configStore.save();

        Plugin.instance = null;
    }

    //endregion

    //region IDistributedSpawnsApi Implementation

    @Override
    public Location getPlayerSpawnLocation(World world, Player player) {
        DistributedSpawnWorldConfig config = this.configStore.getWorldConfig(world, false);
        if (config != null) {
            return config.getPlayerSpawn(player);
        }

        return world.getSpawnLocation();
    }

    @Override
    public void setPlayerSpawnLocation(World world, Player player, Location spawnLocation) {
        DistributedSpawnWorldConfig config = this.configStore.getWorldConfig(world, false);
        if (config != null && config.isPerUserSpawnEnabled()) {
            config.setPlayerSpawn(player, spawnLocation);
        }
    }

    //endregion
}
