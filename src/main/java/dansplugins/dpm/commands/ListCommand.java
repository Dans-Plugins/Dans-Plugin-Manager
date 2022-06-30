package dansplugins.dpm.commands;

import dansplugins.dpm.data.EphemeralData;
import org.bukkit.command.CommandSender;
import preponderous.ponder.minecraft.bukkit.abs.AbstractPluginCommand;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Daniel McCoy Stephenson
 */
public class ListCommand extends AbstractPluginCommand {
    private final EphemeralData ephemeralData;

    public ListCommand(EphemeralData ephemeralData) {
        super(new ArrayList<>(List.of("list")), new ArrayList<>(List.of("dpm.list")));
        this.ephemeralData = ephemeralData;
    }

    @Override
    public boolean execute(CommandSender commandSender) {
        ephemeralData.sendListOfProjectRecordsToCommandSender(commandSender);
        return true;
    }

    @Override
    public boolean execute(CommandSender commandSender, String[] strings) {
        return execute(commandSender);
    }
}