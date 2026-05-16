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
        commandSender.sendMessage(ChatColor.AQUA + "=== DPM Commands ===");
        commandSender.sendMessage(ChatColor.AQUA + "/dpm help - View this list of commands.");
        commandSender.sendMessage(ChatColor.AQUA + "/dpm list [installed|available] - List plugins, optionally filtered.");
        commandSender.sendMessage(ChatColor.AQUA + "/dpm get <plugin-name> [plugin-name ...] - Download one or more plugins.");
        commandSender.sendMessage(ChatColor.AQUA + "/dpm clean [--confirm] - Preview or remove duplicate plugin JARs.");
        commandSender.sendMessage(ChatColor.AQUA + "/dpm stats - View plugin statistics.");
        commandSender.sendMessage(ChatColor.AQUA + "/dpm update - Update all installed plugins.");
        commandSender.sendMessage(ChatColor.AQUA + "/dpm info <plugin-name> - Show description, release info, install status, and dependencies for a plugin.");
        commandSender.sendMessage(ChatColor.AQUA + "/dpm reload - Reload the DPM config.");
        commandSender.sendMessage(ChatColor.AQUA + "/dpm remove <plugin-name> [--confirm] - Preview or remove an installed plugin.");
        return true;
    }

    @Override
    public boolean execute(CommandSender commandSender, String[] strings) {
        return execute(commandSender);
    }
}
