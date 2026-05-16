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
        commandSender.sendMessage(ChatColor.AQUA + "/dpm list - List plugins with install status.");
        commandSender.sendMessage(ChatColor.AQUA + "/dpm get <plugin-name> - Download a plugin.");
        commandSender.sendMessage(ChatColor.AQUA + "/dpm clean - Remove duplicate plugin JARs.");
        commandSender.sendMessage(ChatColor.AQUA + "/dpm stats - View plugin statistics.");
        return true;
    }

    @Override
    public boolean execute(CommandSender commandSender, String[] strings) {
        return execute(commandSender);
    }
}
