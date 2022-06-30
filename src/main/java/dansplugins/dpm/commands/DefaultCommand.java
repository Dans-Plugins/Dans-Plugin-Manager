package dansplugins.dpm.commands;

import dansplugins.dpm.DansPluginManager;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import preponderous.ponder.minecraft.bukkit.abs.AbstractPluginCommand;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author Daniel McCoy Stephenson
 */
public class DefaultCommand extends AbstractPluginCommand {
    private final DansPluginManager dansPluginManager;

    public DefaultCommand(DansPluginManager dansPluginManager) {
        super(new ArrayList<>(Arrays.asList("default")), new ArrayList<>(Arrays.asList("dpm.default")));
        this.dansPluginManager = dansPluginManager;
    }

    @Override
    public boolean execute(CommandSender commandSender) {
        commandSender.sendMessage(ChatColor.AQUA + "Dan's Plugin Manager " + dansPluginManager.getVersion());
        commandSender.sendMessage(ChatColor.AQUA + "Developed by: Daniel McCoy Stephenson, Deej");
        commandSender.sendMessage(ChatColor.AQUA + "Wiki: https://github.com/Dans-Plugins/Dans-Plugin-Manager/wiki");
        return true;
    }

    @Override
    public boolean execute(CommandSender commandSender, String[] strings) {
        return execute(commandSender);
    }
}