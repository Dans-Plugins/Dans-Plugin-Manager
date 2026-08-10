package dansplugins.dpm.commands;

import dansplugins.dpm.repositories.ChannelRepository;
import dansplugins.dpm.repositories.GitHubReleaseRepository;
import dansplugins.dpm.repositories.PluginFileRepository;
import dansplugins.dpm.repositories.ProjectRecordRepository;
import dansplugins.dpm.repositories.VersionRepository;
import dansplugins.dpm.objects.ProjectRecord;
import dansplugins.dpm.objects.ReleaseChannel;
import dansplugins.dpm.objects.ReleaseInfo;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import preponderous.ponder.minecraft.bukkit.abs.AbstractPluginCommand;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class InfoCommand extends AbstractPluginCommand {
    private final ProjectRecordRepository projectRecordRepository;
    private final GitHubReleaseRepository gitHubReleaseRepository;
    private final PluginFileRepository pluginFileRepository;
    private final VersionRepository versionRepository;
    private final ChannelRepository channelRepository;
    private final Plugin plugin;

    public InfoCommand(ProjectRecordRepository projectRecordRepository, GitHubReleaseRepository gitHubReleaseRepository,
                       PluginFileRepository pluginFileRepository, VersionRepository versionRepository,
                       ChannelRepository channelRepository, Plugin plugin) {
        super(new ArrayList<>(List.of("info")), new ArrayList<>(List.of("dpm.info")));
        this.projectRecordRepository = projectRecordRepository;
        this.gitHubReleaseRepository = gitHubReleaseRepository;
        this.pluginFileRepository = pluginFileRepository;
        this.versionRepository = versionRepository;
        this.channelRepository = channelRepository;
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender) {
        sender.sendMessage(ChatColor.RED + "Usage: /dpm info <plugin-name>");
        return false;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        String name = args[0];
        ProjectRecord record = projectRecordRepository.getProjectRecord(name);
        if (record == null) {
            sender.sendMessage(ChatColor.RED + "Plugin not found: " + name + ". Use /dpm search <keyword> to find the right name.");
            return false;
        }
        sender.sendMessage(ChatColor.AQUA + "Fetching release info for " + record.getName() + "...");
        // Report against the channel this plugin tracks, so "Update available" reflects what
        // /dpm update would actually install.
        ReleaseChannel channel = channelRepository.getChannel(record.getName());
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            ReleaseInfo release = gitHubReleaseRepository.getReleaseMetadata(record.getOwner(), record.getRepo(), channel);
            Bukkit.getScheduler().runTask(plugin, () -> showInfo(sender, record, release, channel));
        });
        return true;
    }

    private void showInfo(CommandSender sender, ProjectRecord record, ReleaseInfo release, ReleaseChannel channel) {
        sender.sendMessage(ChatColor.AQUA + "=== " + record.getName() + " ===");

        if (record.getDescription() != null) {
            sender.sendMessage(ChatColor.WHITE + record.getDescription());
        }

        sender.sendMessage(ChatColor.WHITE + "Owner: " + ChatColor.AQUA + record.getOwner());
        sender.sendMessage(ChatColor.WHITE + "Repository: " + ChatColor.AQUA + record.getRepo());
        sender.sendMessage(ChatColor.WHITE + "Channel: "
                + (channel == ReleaseChannel.EXPERIMENTAL ? ChatColor.YELLOW : ChatColor.AQUA)
                + channel.getDisplayName());

        String releaseLabel = channel == ReleaseChannel.EXPERIMENTAL ? "Latest experimental build" : "Latest release";
        if (release == ReleaseInfo.NO_RELEASE) {
            sender.sendMessage(ChatColor.YELLOW + releaseLabel + ": None published yet");
        } else if (release == null) {
            sender.sendMessage(ChatColor.RED + releaseLabel + ": (could not fetch — check console for details)");
        } else {
            sender.sendMessage(ChatColor.WHITE + releaseLabel + ": " + ChatColor.GREEN + release.getTagName());
            if (release.getPublishedAt() != null) {
                sender.sendMessage(ChatColor.WHITE + "Published: " + ChatColor.AQUA + formatDate(release.getPublishedAt()));
            }
        }

        Set<String> installedNames = installedNamesFor(record);
        boolean installed = installedNames.contains(record.getName());
        String storedTag = versionRepository.getStoredTag(record.getName());

        if (installed) {
            String version = storedTag != null ? storedTag : "(version unknown)";
            sender.sendMessage(ChatColor.WHITE + "Installed: " + ChatColor.GREEN + "Yes (" + version + ")");
            if (release != null && release != ReleaseInfo.NO_RELEASE) {
                if (storedTag != null && storedTag.equals(release.getTagName())) {
                    sender.sendMessage(ChatColor.WHITE + "Status: " + ChatColor.GREEN + "Up to date");
                } else {
                    sender.sendMessage(ChatColor.WHITE + "Status: " + ChatColor.YELLOW + "Update available");
                }
            }
        } else {
            sender.sendMessage(ChatColor.WHITE + "Installed: " + ChatColor.GRAY + "No");
        }

        showDependencies(sender, record.getHardDependencies(), "Requires", installedNames);
        showDependencies(sender, record.getSoftDependencies(), "Integrates with", installedNames);
    }

    private Set<String> installedNamesFor(ProjectRecord record) {
        List<ProjectRecord> toScan = new ArrayList<>();
        toScan.add(record);
        for (String dep : record.getHardDependencies()) {
            ProjectRecord r = projectRecordRepository.getProjectRecord(dep);
            if (r != null) toScan.add(r);
        }
        for (String dep : record.getSoftDependencies()) {
            ProjectRecord r = projectRecordRepository.getProjectRecord(dep);
            if (r != null) toScan.add(r);
        }
        Set<String> names = new HashSet<>();
        for (ProjectRecord r : pluginFileRepository.filterInstalled(toScan)) {
            names.add(r.getName());
        }
        return names;
    }

    private void showDependencies(CommandSender sender, List<String> deps, String label, Set<String> installedNames) {
        if (deps.isEmpty()) return;
        for (String dep : deps) {
            String status = installedNames.contains(dep)
                    ? ChatColor.GREEN + dep + " (installed)"
                    : ChatColor.RED + dep + " (not installed)";
            sender.sendMessage(ChatColor.WHITE + label + ": " + status);
        }
    }

    /** Trims the time component from an ISO 8601 timestamp, returning just the date. */
    private String formatDate(String iso8601) {
        int tIndex = iso8601.indexOf('T');
        return tIndex > 0 ? iso8601.substring(0, tIndex) : iso8601;
    }
}
