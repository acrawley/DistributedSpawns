package net.andrewcr.minecraft.plugin.DistributedSpawns.internal.command;

import net.andrewcr.minecraft.plugin.BasePluginLib.command.CommandBase;
import net.andrewcr.minecraft.plugin.BasePluginLib.command.CommandExecutorBase;
import net.andrewcr.minecraft.plugin.BasePluginLib.util.LocationUtil;
import net.andrewcr.minecraft.plugin.DistributedSpawns.internal.Constants;
import net.andrewcr.minecraft.plugin.DistributedSpawns.internal.Plugin;
import net.andrewcr.minecraft.plugin.DistributedSpawns.internal.model.ConfigStore;
import net.andrewcr.minecraft.plugin.DistributedSpawns.internal.model.DistributedSpawnWorldConfig;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;

public class DistributedSpawnInfoCommand extends CommandBase {
    @Override
    public CommandExecutorBase getExecutor() {
        return new DistributedSpawnInfoCommandExecutor();
    }

    private class DistributedSpawnInfoCommandExecutor extends CommandExecutorBase {
        DistributedSpawnInfoCommandExecutor() {
            super("distributedspawn info", Constants.InfoPermission);
        }

        @Override
        protected boolean invoke(String[] args) {
            World world;
            if (args.length == 0) {
                if (this.getPlayer() == null) {
                    this.error("Console invocation requires world to be specified!");
                    return false;
                }

                world = this.getPlayer().getWorld();
            } else if (args.length == 1) {
                world = Bukkit.getWorld(args[0]);

                if (world == null) {
                    this.error("Unknown world '" + args[0] + "'!");
                    return false;
                }
            } else {
                return false;
            }

            DistributedSpawnWorldConfig config = ConfigStore.getInstance().getWorldConfig(world, false);

            this.sendMessage("Spawn configuration for world '" + world.getName() + "':");
            this.sendMessage("  Per-player spawns enabled : " + (config != null && config.isPerUserSpawnEnabled() ? (ChatColor.GREEN + "YES") : (ChatColor.RED + "NO")));
            if (config != null && config.isPerUserSpawnEnabled()) {
                this.sendMessage("    Number of configured spawns: " + config.getSpawnCount());
            }

            this.sendMessage("  Distributed spawns enabled: " + (config != null && config.isDistributedSpawnEnabled() ? (ChatColor.GREEN + "YES") : (ChatColor.RED + "NO")));
            if (config != null && config.isDistributedSpawnEnabled()) {
                this.sendMessage("    Minimum spawn point spacing: " + config.getMinimumSpawnSpacing() + " blocks");
            }

            if (this.getPlayer() != null) {
                this.sendMessage("  Your world spawn location: " +
                    LocationUtil.locationToIntString(Plugin.getInstance().getPlayerSpawnLocation(world, this.getPlayer()), false, false, false));
                this.sendMessage("  Your bed spawn location  : " +
                    (this.getPlayer().getBedSpawnLocation() == null ?
                        "(none)" :
                        LocationUtil.locationToIntString(this.getPlayer().getBedSpawnLocation(), false, false, false)));

            }

            return true;
        }
    }
}
