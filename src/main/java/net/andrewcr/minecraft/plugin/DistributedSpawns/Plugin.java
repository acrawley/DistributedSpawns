package net.andrewcr.minecraft.plugin.DistributedSpawns;

import net.andrewcr.minecraft.plugin.BasePluginLib.plugin.PluginBase;
import net.andrewcr.minecraft.plugin.BasePluginLib.util.Version;

public class Plugin extends PluginBase {
    @Override
    protected Version getRequiredBPLVersion() {
        return new Version(1, 0);
    }
}
