package dansplugins.dpm.commands;

import dansplugins.dpm.data.EphemeralData;
import dansplugins.dpm.objects.ProjectRecord;
import dansplugins.dpm.services.DownloadService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import preponderous.ponder.minecraft.bukkit.abs.AbstractPluginCommand;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Daniel McCoy Stephenson
 */
public class GetCommand extends AbstractPluginCommand {
    private final EphemeralData ephemeralData;
    private final DownloadService downloadService;
    private final Plugin plugin;

    public GetCommand(EphemeralData ephemeralData, DownloadService downloadService, Plugin plugin) {
        super(new ArrayList<>(List.of("get")), new ArrayList<>(List.of("dpm.get")));
        this.ephemeralData = ephemeralData;
        this.downloadService = downloadService;
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender commandSender) {
        commandSender.sendMessage(ChatColor.RED + "Usage: /dpm get <plugin-name>");
        return false;
    }

    @Override
    public boolean execute(CommandSender commandSender, String[] args) {
        String name = args[0];
        ProjectRecord projectRecord = ephemeralData.getProjectRecord(name);
        if (projectRecord == null) {
            commandSender.sendMessage(ChatColor.RED + "Plugin not found: " + name);
            return false;
        }
        commandSender.sendMessage(ChatColor.AQUA + "Fetching " + projectRecord.getName() + "...");
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int result = downloadService.downloadLatest(projectRecord);
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (result == DownloadService.NO_RELEASE) {
                    commandSender.sendMessage(ChatColor.YELLOW + projectRecord.getName() + " has no published release yet. Try again later.");
                } else if (result <= 0) {
                    commandSender.sendMessage(ChatColor.RED + "Something went wrong downloading " + projectRecord.getName() + ".");
                } else {
                    commandSender.sendMessage(ChatColor.GREEN + "Downloaded " + (result / 1024) + " KB. Restart the server to enable " + projectRecord.getName() + ".");
                }
            });
        });
        return true;
    }
}
