package net.andrewcr.minecraft.plugin.DistributedSpawns.command;

import net.andrewcr.minecraft.plugin.BasePluginLib.command.CommandBase;
import net.andrewcr.minecraft.plugin.BasePluginLib.command.CommandExecutorBase;
import net.andrewcr.minecraft.plugin.BasePluginLib.command.GroupCommandExecutorBase;

public class DistributedSpawnCommand extends CommandBase {
    @Override
    public CommandExecutorBase getExecutor() {
        return new DistributedSpawnCommandExecutor();
    }

    private class DistributedSpawnCommandExecutor extends GroupCommandExecutorBase {
        DistributedSpawnCommandExecutor() {
            super("distributedspawn");
        }
    }
}
