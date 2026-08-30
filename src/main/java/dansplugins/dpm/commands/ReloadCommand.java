package dansplugins.dpm.commands;

import dansplugins.dpm.controllers.ReloadController;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import preponderous.ponder.minecraft.bukkit.abs.AbstractPluginCommand;

import java.util.ArrayList;
import java.util.List;

public class ReloadCommand extends AbstractPluginCommand {
    private final ReloadController reloadController;

    public ReloadCommand(ReloadController reloadController) {
        super(new ArrayList<>(List.of("reload")), new ArrayList<>(List.of("dpm.reload")));
        this.reloadController = reloadController;
    }

    @Override
    public boolean execute(CommandSender sender) {
        reloadController.reload();
        sender.sendMessage(ChatColor.GREEN + "DPM config reloaded.");
        return true;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        return execute(sender);
    }
}
