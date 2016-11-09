package net.andrewcr.minecraft.plugin.DistributedSpawns.command;

import net.andrewcr.minecraft.plugin.BasePluginLib.command.CommandBase;
import net.andrewcr.minecraft.plugin.BasePluginLib.command.CommandExecutorBase;
import net.andrewcr.minecraft.plugin.BasePluginLib.util.LocationUtil;
import net.andrewcr.minecraft.plugin.DistributedSpawns.Constants;
import net.andrewcr.minecraft.plugin.DistributedSpawns.model.ConfigStore;
import net.andrewcr.minecraft.plugin.DistributedSpawns.model.DistributedSpawnWorldConfig;
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
            if (config == null) {
                this.sendMessage("No distributed spawn configuration set for world '" + world.getName() + "'!");
                return true;
            }

            this.sendMessage("Spawn configuration for world '" + world.getName() + "':");
            this.sendMessage("  Per-player spawns: " + (config.isPerUserSpawnEnabled() ? (ChatColor.GREEN + "YES") : (ChatColor.RED + "NO")));
            if (config.isPerUserSpawnEnabled()) {
                this.sendMessage("    Number of configured spawns: " + config.getSpawnCount());
            }

            this.sendMessage("  Distributed spawns: " + (config.isDistributedSpawnEnabled() ? (ChatColor.GREEN + "YES") : (ChatColor.RED + "NO")));
            if (config.isDistributedSpawnEnabled()) {
                this.sendMessage("    Minimum spawn point spacing: " + config.getMinimumSpawnSpacing() + " blocks");
            }

            if (this.getPlayer() != null) {
                this.sendMessage("  Your spawn location: " + LocationUtil.locationToIntString(config.getPlayerSpawn(this.getPlayer()), false, false, false));
            }


            return true;
        }
    }
}
