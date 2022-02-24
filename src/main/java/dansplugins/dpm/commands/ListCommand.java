package dansplugins.dpm.commands;

import org.bukkit.command.CommandSender;

import dansplugins.dpm.data.EphemeralData;
import preponderous.ponder.minecraft.bukkit.abs.AbstractPluginCommand;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Daniel McCoy Stephenson
 */
public class ListCommand extends AbstractPluginCommand {

    public ListCommand() {
        super(new ArrayList<>(List.of("list")), new ArrayList<>(List.of("dpm.list")));
    }

    @Override
    public boolean execute(CommandSender commandSender) {
        EphemeralData.getInstance().sendListOfProjectRecordsToCommandSender(commandSender);
        return true;
    }

    @Override
    public boolean execute(CommandSender commandSender, String[] strings) {
        return execute(commandSender);
    }
}