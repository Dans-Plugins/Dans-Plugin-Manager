package dansplugins.dpm.commands;

import dansplugins.dpm.controllers.StatsController;
import dansplugins.dpm.controllers.StatsController.Stats;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import preponderous.ponder.minecraft.bukkit.abs.AbstractPluginCommand;

import java.util.ArrayList;
import java.util.List;

public class StatsCommand extends AbstractPluginCommand {
    private final StatsController statsController;

    public StatsCommand(StatsController statsController) {
        super(new ArrayList<>(List.of("stats")), new ArrayList<>(List.of("dpm.stats")));
        this.statsController = statsController;
    }

    @Override
    public boolean execute(CommandSender commandSender) {
        Stats stats = statsController.getStats();
        commandSender.sendMessage(ChatColor.AQUA + "=== DPM Stats ===");
        commandSender.sendMessage(ChatColor.AQUA + "Registered plugins: " + stats.getTotal());
        commandSender.sendMessage(ChatColor.AQUA + "Installed plugins: " + stats.getInstalled());
        commandSender.sendMessage(ChatColor.AQUA + "Available plugins: " + stats.getAvailable());
        return true;
    }

    @Override
    public boolean execute(CommandSender commandSender, String[] strings) {
        return execute(commandSender);
    }
}
