package net.andrewcr.minecraft.plugin.DistributedSpawns.command;

import net.andrewcr.minecraft.plugin.BasePluginLib.command.CommandBase;
import net.andrewcr.minecraft.plugin.BasePluginLib.command.CommandExecutorBase;
import net.andrewcr.minecraft.plugin.BasePluginLib.util.ArrayUtil;
import net.andrewcr.minecraft.plugin.BasePluginLib.util.LocationUtil;
import net.andrewcr.minecraft.plugin.BasePluginLib.util.StringUtil;
import net.andrewcr.minecraft.plugin.DistributedSpawns.Constants;
import net.andrewcr.minecraft.plugin.DistributedSpawns.model.ConfigStore;
import net.andrewcr.minecraft.plugin.DistributedSpawns.model.DistributedSpawnWorldConfig;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Locale;

public class DistributedSpawnSetCommand extends CommandBase {
    @Override
    public CommandExecutorBase getExecutor() {
        return new DistributedSpawnSetCommandExecutor();
    }

    private class DistributedSpawnSetCommandExecutor extends CommandExecutorBase {
        public DistributedSpawnSetCommandExecutor() {
            super("distributedspawn set");
        }

        @Override
        protected boolean invoke(String[] args) {
            if (args.length == 1 && StringUtil.equalsIgnoreCase(args[0], "spawnpoint")) {
                // Setting our own spawn point
                Player player = this.getPlayer();
                if (player == null) {
                    this.error("Player must be specified when invoking from console!");
                    return false;
                }

                if (!this.hasPermission(Constants.SetOwnSpawnPermission)) {
                    this.error("You do not have permission to change your spawn point!");
                    return false;
                }

                return this.setPlayerSpawn(player);
            }

            if (args.length == 2 && StringUtil.equalsIgnoreCase(args[1], "spawnpoint")) {
                // Setting another player's spawnpoint
                Player player = Bukkit.getPlayer(args[0]);
                if (player == null) {
                    this.error("Unknown player '" + args[0] + "'!");
                    return false;
                }

                if (!player.isOnline()) {
                    this.error("Player '" + args[0] + "' is not online!");
                    return false;
                }

                if (!this.hasPermission(Constants.SetOtherSpawnPermission)) {
                    this.error("You do not have permission to change another player's spawn point!");
                    return false;
                }

                return this.setPlayerSpawn(player);
            }

            if (args.length < 3) {
                return false;
            }

            World world = Bukkit.getWorld(args[0]);
            if (world == null) {
                this.error("Unknown world '" + args[0] + "'!");
                return false;
            }

            String cmd = args[1].toLowerCase(Locale.ENGLISH);
            String[] propertyArgs = ArrayUtil.removeFirst(args, 2);
            switch (cmd) {
                case "perplayer":
                    return this.setPerPlayer(world, propertyArgs);

                case "distributed":
                    return this.setDistributed(world, propertyArgs);
            }

            this.error("Unknown property '" + cmd + "'!");
            return false;
        }

        private boolean setPlayerSpawn(Player player) {
            DistributedSpawnWorldConfig config = ConfigStore.getInstance().getWorldConfig(player.getWorld(), false);
            if (config == null || !config.isPerUserSpawnEnabled()) {
                this.error("Per-user spawning is not enabled in world '" + player.getWorld().getName() + "'!");
                return true;
            }

            config.setPlayerSpawn(player, player.getLocation());

            this.sendMessage("Spawn for player '" + player.getName() + "' set to (" +
                LocationUtil.locationToIntString(player.getLocation(), false, false, false) + ")!");

            return true;
        }

        private boolean setPerPlayer(World world, String[] args) {
            if (args.length != 1) {
                return false;
            }

            Boolean enable = this.parseEnableDisable(args[0]);
            if (enable == null) {
                return false;
            }

            DistributedSpawnWorldConfig config = ConfigStore.getInstance().getWorldConfig(world, true);

            if (!enable && config.isDistributedSpawnEnabled()) {
                this.error("Per-user spawns cannot be disabled when distributed spawns are enabled!");
                return true;
            }

            config.setPerUserSpawnEnabled(enable);

            this.sendMessage("Per-user spawns are " + (enable ? "enabled" : "disabled") + " in world '" + world.getName() + "'!");

            return true;
        }

        private boolean setDistributed(World world, String[] args) {
            if (args.length != 1 && args.length != 2) {
                return false;
            }

            Boolean enable = this.parseEnableDisable(args[0]);
            if (enable == null) {
                return false;
            }

            DistributedSpawnWorldConfig config = ConfigStore.getInstance().getWorldConfig(world, true);

            if (enable) {
                if (args.length != 2) {
                    return false;
                }

                try {
                    int minDistance = Integer.parseInt(args[1]);

                    config.setMinimumSpawnSpacing(minDistance);
                    config.setPerUserSpawnEnabled(true);
                } catch (NumberFormatException ex) {
                    this.error("Unable to parse '" + args[1] + "' as a number!");
                    return false;
                }
            }

            config.setDistributedSpawnEnabled(enable);

            if (enable) {
                this.sendMessage("Distributed spawn point generation is enabled in world '" + world.getName()
                    + "' with a minimum separation distance of " + config.getMinimumSpawnSpacing() + " blocks!");
            } else {
                this.sendMessage("Distributed spawn point generation is disabled in world '" + world.getName() + "'!");
            }

            return true;
        }

        private Boolean parseEnableDisable(String text) {
            if (StringUtil.equalsIgnoreCase(text, "enable")) {
                return true;
            } else if (StringUtil.equalsIgnoreCase(text, "disable")) {
                return false;
            }

            return null;
        }
    }
}
