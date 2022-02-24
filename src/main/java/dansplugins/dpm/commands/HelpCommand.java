package dansplugins.dpm.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import preponderous.ponder.minecraft.bukkit.abs.AbstractPluginCommand;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Daniel McCoy Stephenson
 */
public class HelpCommand extends AbstractPluginCommand {

    public HelpCommand() {
        super(new ArrayList<>(List.of("help")), new ArrayList<>(List.of("dpm.help")));
    }

    @Override
    public boolean execute(CommandSender commandSender) {
        commandSender.sendMessage("=== DPM Commands ===");
        commandSender.sendMessage(ChatColor.AQUA + "/dpm help - View a list of helpful commands.");
        commandSender.sendMessage(ChatColor.AQUA + "/dpm list - List project records.");
        commandSender.sendMessage(ChatColor.AQUA + "/dpm get <project-record-name> - Download a project");
        commandSender.sendMessage(ChatColor.AQUA + "/dpm stats - View relevant stats.");
        return true;
    }

    @Override
    public boolean execute(CommandSender commandSender, String[] strings) {
        return execute(commandSender);
    }
}
