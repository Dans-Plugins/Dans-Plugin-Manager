package dansplugins.dpm.commands;

import dansplugins.dpm.controllers.SearchController;
import dansplugins.dpm.controllers.SearchController.SearchResult;
import dansplugins.dpm.objects.ProjectRecord;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import preponderous.ponder.minecraft.bukkit.abs.AbstractPluginCommand;

import java.util.ArrayList;
import java.util.List;

public class SearchCommand extends AbstractPluginCommand {
    private final SearchController searchController;

    public SearchCommand(SearchController searchController) {
        super(new ArrayList<>(List.of("search")), new ArrayList<>(List.of("dpm.list")));
        this.searchController = searchController;
    }

    @Override
    public boolean execute(CommandSender sender) {
        sender.sendMessage(ChatColor.RED + "Usage: /dpm search <keyword>");
        return false;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        String keyword = String.join(" ", args);
        List<SearchResult> results = searchController.search(keyword);
        if (results.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "No plugins found matching \"" + keyword + "\".");
            return true;
        }
        sender.sendMessage(ChatColor.AQUA + "=== Search Results (" + results.size() + ") ===");
        for (SearchResult result : results) {
            ProjectRecord record = result.getRecord();
            String desc = record.getDescription() != null ? ChatColor.GRAY + " — " + record.getDescription() : "";
            if (result.isInstalled()) {
                String version = result.getStoredTag() != null ? " " + result.getStoredTag() : "";
                sender.sendMessage(ChatColor.GREEN + record.getName() + version + desc);
            } else {
                sender.sendMessage(ChatColor.GRAY + record.getName() + desc);
            }
        }
        return true;
    }
}
