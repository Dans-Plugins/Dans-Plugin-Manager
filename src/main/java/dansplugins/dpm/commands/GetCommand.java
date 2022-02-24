package dansplugins.dpm.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import dansplugins.dpm.DansPluginManager;
import dansplugins.dpm.data.EphemeralData;
import dansplugins.dpm.objects.ProjectRecord;
import dansplugins.dpm.services.LocalDownloadService;
import preponderous.ponder.minecraft.bukkit.abs.AbstractPluginCommand;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author Daniel McCoy Stephenson
 */
public class GetCommand extends AbstractPluginCommand {

    public GetCommand() {
        super(new ArrayList<>(Arrays.asList("get")), new ArrayList<>(Arrays.asList("dpm.get")));
    }

    @Override
    public boolean execute(CommandSender commandSender) {
        commandSender.sendMessage(ChatColor.RED + "Usage: /dpm get medievalfactions");
        return false;
    }

    @Override
    public boolean execute(CommandSender commandSender, String[] args) {
        String name = args[0];
        ProjectRecord projectRecord = EphemeralData.getInstance().getProjectRecord(name);
        if (projectRecord == null) {
            commandSender.sendMessage(ChatColor.RED + "A project record wasn't found with that name.");
            return false;
        }
        int bytesRead = LocalDownloadService.getInstance().downloadFromLink(projectRecord);
        if (bytesRead == 0) {
            commandSender.sendMessage(ChatColor.RED + "No bytes were read.");
            return false;
        }
        if (bytesRead == -1) {
            commandSender.sendMessage(ChatColor.RED + "Something went wrong.");
            return false;
        }
        return true;
    }
}