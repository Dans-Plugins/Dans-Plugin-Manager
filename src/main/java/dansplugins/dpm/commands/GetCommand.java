package dansplugins.dpm.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import dansplugins.dpm.data.EphemeralData;
import dansplugins.dpm.objects.ProjectRecord;
import dansplugins.dpm.services.LocalDownloadService;
import preponderous.ponder.minecraft.bukkit.abs.AbstractPluginCommand;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Daniel McCoy Stephenson
 */
public class GetCommand extends AbstractPluginCommand {

    public GetCommand() {
        super(new ArrayList<>(List.of("get")), new ArrayList<>(List.of("dpm.get")));
    }

    @Override
    public boolean execute(CommandSender commandSender) {
        commandSender.sendMessage(ChatColor.RED + "Usage: /dpm get <project-record-name>");
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

        commandSender.sendMessage(ChatColor.GREEN + "Success! " + bytesRead + " bytes were retrieved. Restart the server in order to enable " + projectRecord.getName() + ".");
        return true;
    }
}