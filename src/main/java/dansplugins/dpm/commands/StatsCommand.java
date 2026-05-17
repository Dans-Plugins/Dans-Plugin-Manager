package dansplugins.dpm.commands;

import dansplugins.dpm.data.EphemeralData;
import dansplugins.dpm.services.PluginFolderService;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import preponderous.ponder.minecraft.bukkit.abs.AbstractPluginCommand;

import java.util.ArrayList;
import java.util.List;

public class StatsCommand extends AbstractPluginCommand {
    private final EphemeralData ephemeralData;
    private final PluginFolderService pluginFolderService;

    public StatsCommand(EphemeralData ephemeralData, PluginFolderService pluginFolderService) {
        super(new ArrayList<>(List.of("stats")), new ArrayList<>(List.of("dpm.stats")));
        this.ephemeralData = ephemeralData;
        this.pluginFolderService = pluginFolderService;
    }

    @Override
    public boolean execute(CommandSender commandSender) {
        int installed = pluginFolderService.filterInstalled(ephemeralData.getAllProjectRecords()).size();
        commandSender.sendMessage(ChatColor.AQUA + "=== DPM Stats ===");
        commandSender.sendMessage(ChatColor.AQUA + "Registered plugins: " + ephemeralData.getNumProjectRecords());
        commandSender.sendMessage(ChatColor.AQUA + "Installed plugins: " + installed);
        return true;
    }

    @Override
    public boolean execute(CommandSender commandSender, String[] strings) {
        return execute(commandSender);
    }
}
