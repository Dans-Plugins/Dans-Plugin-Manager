package dansplugins.dpm.commands;

import dansplugins.dpm.repositories.PluginFileRepository;
import dansplugins.dpm.repositories.ProjectRecordRepository;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import preponderous.ponder.minecraft.bukkit.abs.AbstractPluginCommand;

import java.util.ArrayList;
import java.util.List;

public class StatsCommand extends AbstractPluginCommand {
    private final ProjectRecordRepository projectRecordRepository;
    private final PluginFileRepository pluginFileRepository;

    public StatsCommand(ProjectRecordRepository projectRecordRepository, PluginFileRepository pluginFileRepository) {
        super(new ArrayList<>(List.of("stats")), new ArrayList<>(List.of("dpm.stats")));
        this.projectRecordRepository = projectRecordRepository;
        this.pluginFileRepository = pluginFileRepository;
    }

    @Override
    public boolean execute(CommandSender commandSender) {
        int total = projectRecordRepository.getNumProjectRecords();
        int installed = pluginFileRepository.filterInstalled(projectRecordRepository.getAllProjectRecords()).size();
        int available = total - installed;
        commandSender.sendMessage(ChatColor.AQUA + "=== DPM Stats ===");
        commandSender.sendMessage(ChatColor.AQUA + "Registered plugins: " + total);
        commandSender.sendMessage(ChatColor.AQUA + "Installed plugins: " + installed);
        commandSender.sendMessage(ChatColor.AQUA + "Available plugins: " + available);
        return true;
    }

    @Override
    public boolean execute(CommandSender commandSender, String[] strings) {
        return execute(commandSender);
    }
}
