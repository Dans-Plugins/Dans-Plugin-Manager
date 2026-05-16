package dansplugins.dpm.commands;

import dansplugins.dpm.DansPluginManager;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import preponderous.ponder.minecraft.bukkit.abs.AbstractPluginCommand;

import java.util.ArrayList;
import java.util.List;

public class ReloadCommand extends AbstractPluginCommand {
    private final DansPluginManager plugin;

    public ReloadCommand(DansPluginManager plugin) {
        super(new ArrayList<>(List.of("reload")), new ArrayList<>(List.of("dpm.reload")));
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender) {
        plugin.reloadDpm();
        sender.sendMessage(ChatColor.GREEN + "DPM config reloaded.");
        return true;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        return execute(sender);
    }
}
