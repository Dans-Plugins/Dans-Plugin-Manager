package dansplugins.dpm;

import dansplugins.dpm.commands.*;
import dansplugins.dpm.data.EphemeralData;
import dansplugins.dpm.objects.ProjectRecord;
import dansplugins.dpm.factories.ProjectRecordFactory;
import dansplugins.dpm.services.ConfigService;
import dansplugins.dpm.services.DependencyResolutionService;
import dansplugins.dpm.services.DownloadService;
import dansplugins.dpm.services.GitHubReleaseService;
import dansplugins.dpm.services.PluginFolderService;
import dansplugins.dpm.services.VersionStore;
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
    private final EphemeralData ephemeralData = new EphemeralData();
    private final ProjectRecordFactory projectRecordFactory = new ProjectRecordFactory(ephemeralData);
    private final ProjectRecordInitializer projectRecordInitializer = new ProjectRecordInitializer(projectRecordFactory);
    private final ConfigService configService = new ConfigService(this);
    private final Logger logger = new Logger(this);
    private final GitHubReleaseService gitHubReleaseService = new GitHubReleaseService(logger);
    private final PluginFolderService pluginFolderService = new PluginFolderService();
    private final DependencyResolutionService dependencyResolutionService = new DependencyResolutionService(ephemeralData, pluginFolderService);
    private VersionStore versionStore;
    private DownloadService downloadService;
    private RemoveCommand removeCommand;
    private UpdateCommand updateCommand;

    @Override
    public void onEnable() {
        initializeConfig();
        gitHubReleaseService.setApiToken(configService.getStringOrDefault("githubToken", ""));
        versionStore = new VersionStore(new File(getDataFolder(), "dpm-versions.properties"));
        downloadService = new DownloadService(logger, gitHubReleaseService, pluginFolderService, versionStore);
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
            return TabCompleter.filterByPrefix(allPluginNames(), args[args.length - 1]);
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
        for (ProjectRecord record : ephemeralData.getAllProjectRecords()) {
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
        return configService.getBoolean("debugMode");
    }

    public void reloadDpm() {
        reloadConfig();
        gitHubReleaseService.setApiToken(configService.getStringOrDefault("githubToken", ""));
        gitHubReleaseService.clearCache();
    }

    private void initializeConfig() {
        if (configFileExists()) {
            performCompatibilityChecks();
        }
        else {
            configService.saveMissingConfigDefaultsIfNotPresent();
        }
    }

    private boolean configFileExists() {
        return new File("./plugins/" + getName() + "/config.yml").exists();
    }

    private void performCompatibilityChecks() {
        if (isVersionMismatched()) {
            configService.saveMissingConfigDefaultsIfNotPresent();
        }
        reloadConfig();
    }

    private void initializeCommandService() {
        ArrayList<AbstractPluginCommand> commands = new ArrayList<>(Arrays.asList(
                new HelpCommand(),
                new GetCommand(ephemeralData, downloadService, dependencyResolutionService, versionStore, this),
                new ListCommand(ephemeralData, pluginFolderService, versionStore),
                new StatsCommand(ephemeralData, pluginFolderService),
                new CleanCommand(ephemeralData, pluginFolderService, this),
                updateCommand = new UpdateCommand(ephemeralData, downloadService, pluginFolderService, versionStore, this),
                new InfoCommand(ephemeralData, gitHubReleaseService, pluginFolderService, versionStore, this),
                new ReloadCommand(this),
                removeCommand = new RemoveCommand(ephemeralData, pluginFolderService, versionStore, dependencyResolutionService),
                new SearchCommand(ephemeralData, pluginFolderService, versionStore)
        ));
        commandService.initialize(commands, "That command wasn't found.");
    }
}