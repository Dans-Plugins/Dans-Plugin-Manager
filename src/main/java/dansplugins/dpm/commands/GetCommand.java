package dansplugins.dpm.commands;

import dansplugins.dpm.data.EphemeralData;
import dansplugins.dpm.objects.ProjectRecord;
import dansplugins.dpm.services.DownloadService;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import preponderous.ponder.minecraft.bukkit.abs.AbstractPluginCommand;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Daniel McCoy Stephenson
 */
public class GetCommand extends AbstractPluginCommand {
    private final EphemeralData ephemeralData;
    private final DownloadService downloadService;

    public GetCommand(EphemeralData ephemeralData, DownloadService downloadService) {
        super(new ArrayList<>(List.of("get")), new ArrayList<>(List.of("dpm.get")));
        this.ephemeralData = ephemeralData;
        this.downloadService = downloadService;
    }

    @Override
    public boolean execute(CommandSender commandSender) {
        commandSender.sendMessage(ChatColor.RED + "Usage: /dpm get <project-record-name>");
        return false;
    }

    @Override
    public boolean execute(CommandSender commandSender, String[] args) {
        String name = args[0];
        ProjectRecord projectRecord = ephemeralData.getProjectRecord(name);
        if (projectRecord == null) {
            commandSender.sendMessage(ChatColor.RED + "A project record wasn't found with that name.");
            return false;
        }
        int result = downloadService.downloadLatest(projectRecord);
        if (result == DownloadService.NO_RELEASE) {
            commandSender.sendMessage(ChatColor.YELLOW + projectRecord.getName() + " has no published release yet. Try again later.");
            return false;
        }
        if (result == 0) {
            commandSender.sendMessage(ChatColor.RED + "No bytes were read.");
            return false;
        }
        if (result < 0) {
            commandSender.sendMessage(ChatColor.RED + "Something went wrong downloading " + projectRecord.getName() + ".");
            return false;
        }
        commandSender.sendMessage(ChatColor.GREEN + "Success! " + result + " chunks retrieved. Restart the server to enable " + projectRecord.getName() + ".");
        return true;
    }
}
