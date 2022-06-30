package dansplugins.dpm.commands;

import dansplugins.dpm.data.EphemeralData;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import preponderous.ponder.minecraft.bukkit.abs.AbstractPluginCommand;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Daniel McCoy Stephenson
 */
public class StatsCommand extends AbstractPluginCommand {
    private final EphemeralData ephemeralData;

    public StatsCommand(EphemeralData ephemeralData) {
        super(new ArrayList<>(List.of("stats")), new ArrayList<>(List.of("dpm.stats")));
        this.ephemeralData = ephemeralData;
    }

    @Override
    public boolean execute(CommandSender commandSender) {
        commandSender.sendMessage(ChatColor.AQUA + "=== DPM Stats ===");
        commandSender.sendMessage(ChatColor.AQUA + "Number of project records: " + ephemeralData.getNumProjectRecords());
        return true;
    }

    @Override
    public boolean execute(CommandSender commandSender, String[] strings) {
        return execute(commandSender);
    }
}
