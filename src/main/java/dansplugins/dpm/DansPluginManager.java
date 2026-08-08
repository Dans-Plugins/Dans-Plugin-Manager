package dansplugins.dpm;

import dansplugins.dpm.commands.*;
import dansplugins.dpm.controllers.GetController;
import dansplugins.dpm.controllers.RemoveController;
import dansplugins.dpm.controllers.SearchController;
import dansplugins.dpm.controllers.StatsController;
import dansplugins.dpm.controllers.UpdateController;
import dansplugins.dpm.repositories.ChannelRepository;
import dansplugins.dpm.repositories.ConfigRepository;
import dansplugins.dpm.repositories.GitHubReleaseRepository;
import dansplugins.dpm.repositories.PluginFileRepository;
import dansplugins.dpm.repositories.ProjectRecordRepository;
import dansplugins.dpm.repositories.VersionRepository;
import dansplugins.dpm.objects.ProjectRecord;
import dansplugins.dpm.factories.ProjectRecordFactory;
import dansplugins.dpm.services.DependencyResolutionService;
import dansplugins.dpm.services.DiscordNotificationService;
import dansplugins.dpm.services.DownloadService;
import dansplugins.dpm.utils.Logger;
import dansplugins.dpm.utils.ProjectRecordInitializer;
import dansplugins.dpm.utils.TabCompleter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import preponderous.ponder.minecraft.bukkit.abs.AbstractPluginCommand;
import preponderous.ponder.minecraft.bukkit.abs.PonderBukkitPlugin;
import preponderous.ponder.minecraft.bukkit.services.CommandService;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class DansPluginManager extends PonderBukkitPlugin {
    private static final List<String> CONFIRM_COMPLETION = List.of("--confirm");

    private final String pluginVersion = "v" + getDescription().getVersion();

    private final CommandService commandService = new CommandService(getPonder());
    private final ProjectRecordRepository projectRecordRepository = new ProjectRecordRepository();
    private final ProjectRecordFactory projectRecordFactory = new ProjectRecordFactory(projectRecordRepository);
    private final ProjectRecordInitializer projectRecordInitializer = new ProjectRecordInitializer(projectRecordFactory);
    private final ConfigRepository configRepository = new ConfigRepository(this::getConfig, this::getVersion, this::saveConfig);
    private final Logger logger = new Logger(this);
    private final GitHubReleaseRepository gitHubReleaseRepository = new GitHubReleaseRepository(logger);
    private final PluginFileRepository pluginFileRepository = new PluginFileRepository();
    private final DependencyResolutionService dependencyResolutionService = new DependencyResolutionService(projectRecordRepository, pluginFileRepository);
    private final DiscordNotificationService discordNotificationService = new DiscordNotificationService(configRepository);
    private VersionRepository versionRepository;
    private ChannelRepository channelRepository;
    private DownloadService downloadService;
    private GetController getController;
    private UpdateController updateController;
    private RemoveController removeController;
    private final StatsController statsController = new StatsController(projectRecordRepository, pluginFileRepository);
    private SearchController searchController;
    private RemoveCommand removeCommand;
    private UpdateCommand updateCommand;

    @Override
    public void onEnable() {
        initializeConfig();
        gitHubReleaseRepository.setApiToken(configRepository.getStringOrDefault("githubToken", ""));
        gitHubReleaseRepository.setExperimentalTag(configRepository.getStringOrDefault("experimentalReleaseTag",
                GitHubReleaseRepository.DEFAULT_EXPERIMENTAL_TAG));
        versionRepository = new VersionRepository(new File(getDataFolder(), "dpm-versions.properties"), logger);
        channelRepository = new ChannelRepository(new File(getDataFolder(), "dpm-channels.properties"), logger);
        downloadService = new DownloadService(logger, gitHubReleaseRepository, pluginFileRepository, versionRepository);
        getController = new GetController(downloadService, dependencyResolutionService, versionRepository, channelRepository, discordNotificationService, getLogger());
        updateController = new UpdateController(projectRecordRepository, pluginFileRepository, downloadService, versionRepository, channelRepository, discordNotificationService, getLogger());
        removeController = new RemoveController(projectRecordRepository, pluginFileRepository, versionRepository, channelRepository, dependencyResolutionService, getLogger());
        searchController = new SearchController(projectRecordRepository, pluginFileRepository, versionRepository);
        initializeCommandService();
        projectRecordInitializer.initializeProjectRecords();
    }

    @Override
    public void onDisable() {

    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (args.length == 1) {
            return TabCompleter.filterByPrefix(Arrays.asList("help", "list", "get", "clean", "stats", "update", "info", "reload", "remove", "search"), args[0]);
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("get")) {
            return TabCompleter.filterByPrefix(
                    TabCompleter.pluginNamesWithChannelFlags(allPluginNames(), args), args[args.length - 1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("info")) {
            return TabCompleter.filterByPrefix(allPluginNames(), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("list")) {
            return TabCompleter.filterByPrefix(List.of("installed", "available"), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
            return TabCompleter.filterByPrefix(removeCommand.getInstalledPluginNames(), args[1]);
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("update")) {
            return TabCompleter.filterByPrefix(updateCommand.getInstalledPluginNames(), args[args.length - 1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("clean")) {
            return TabCompleter.filterByPrefix(CONFIRM_COMPLETION, args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("remove")) {
            return TabCompleter.filterByPrefix(CONFIRM_COMPLETION, args[2]);
        }
        return Collections.emptyList();
    }

    private List<String> allPluginNames() {
        List<String> names = new ArrayList<>();
        for (ProjectRecord record : projectRecordRepository.getAllProjectRecords()) {
            names.add(record.getName());
        }
        return names;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (args.length == 0) {
            DefaultCommand defaultCommand = new DefaultCommand(this);
            return defaultCommand.execute(sender);
        }

        return commandService.interpretAndExecuteCommand(sender, label, args);
    }

    public String getVersion() {
        return pluginVersion;
    }

    public boolean isVersionMismatched() {
        String configVersion = this.getConfig().getString("version");
        if (configVersion == null) {
            return false;
        } else {
            return !configVersion.equalsIgnoreCase(this.getVersion());
        }
    }

    public boolean isDebugEnabled() {
        return configRepository.getBoolean("debugMode");
    }

    public void reloadDpm() {
        reloadConfig();
        gitHubReleaseRepository.setApiToken(configRepository.getStringOrDefault("githubToken", ""));
        gitHubReleaseRepository.setExperimentalTag(configRepository.getStringOrDefault("experimentalReleaseTag",
                GitHubReleaseRepository.DEFAULT_EXPERIMENTAL_TAG));
        gitHubReleaseRepository.clearCache();
    }

    private void initializeConfig() {
        if (configFileExists()) {
            performCompatibilityChecks();
        }
        else {
            configRepository.saveMissingConfigDefaultsIfNotPresent();
        }
    }

    private boolean configFileExists() {
        return new File("./plugins/" + getName() + "/config.yml").exists();
    }

    private void performCompatibilityChecks() {
        if (isVersionMismatched()) {
            configRepository.saveMissingConfigDefaultsIfNotPresent();
        }
        reloadConfig();
    }

    private void initializeCommandService() {
        ArrayList<AbstractPluginCommand> commands = new ArrayList<>(Arrays.asList(
                new HelpCommand(),
                new GetCommand(projectRecordRepository, getController, this),
                new ListCommand(projectRecordRepository, pluginFileRepository, versionRepository, channelRepository),
                new StatsCommand(statsController),
                new CleanCommand(projectRecordRepository, pluginFileRepository, this),
                updateCommand = new UpdateCommand(updateController, this),
                new InfoCommand(projectRecordRepository, gitHubReleaseRepository, pluginFileRepository, versionRepository, channelRepository, this),
                new ReloadCommand(this),
                removeCommand = new RemoveCommand(projectRecordRepository, removeController),
                new SearchCommand(searchController)
        ));
        commandService.initialize(commands, "That command wasn't found.");
    }
}