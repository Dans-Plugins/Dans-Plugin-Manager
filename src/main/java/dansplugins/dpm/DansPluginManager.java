package dansplugins.dpm;

import dansplugins.dpm.commands.*;
import dansplugins.dpm.data.EphemeralData;
import dansplugins.dpm.objects.ProjectRecord;
import dansplugins.dpm.factories.ProjectRecordFactory;
import dansplugins.dpm.services.ConfigService;
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

/**
 * @author Daniel McCoy Stephenson
 */
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
    private VersionStore versionStore;
    private DownloadService downloadService;
    private RemoveCommand removeCommand;

    /**
     * This runs when the server starts.
     */
    @Override
    public void onEnable() {
        initializeConfig();
        gitHubReleaseService.setApiToken(configService.getStringOrDefault("githubToken", ""));
        versionStore = new VersionStore(new File(getDataFolder(), "dpm-versions.properties"));
        downloadService = new DownloadService(logger, gitHubReleaseService, pluginFolderService, versionStore);
        initializeCommandService();
        projectRecordInitializer.initializeProjectRecords();
    }

    /**
     * This runs when the server stops.
     */
    @Override
    public void onDisable() {

    }

    /**
     * This method handles commands sent to the minecraft server and interprets them if the label matches one of the core commands.
     * @param sender The sender of the command.
     * @param cmd The command that was sent. This is unused.
     * @param label The core command that has been invoked.
     * @param args Arguments of the core command. Often sub-commands.
     * @return A boolean indicating whether the execution of the command was successful.
     */
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (args.length == 1) {
            return TabCompleter.filterByPrefix(Arrays.asList("help", "list", "get", "clean", "stats", "update", "info", "reload", "remove"), args[0]);
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("get")) {
            List<String> names = new ArrayList<>();
            for (ProjectRecord record : ephemeralData.getAllProjectRecords()) {
                names.add(record.getName());
            }
            return TabCompleter.filterByPrefix(names, args[args.length - 1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("info")) {
            List<String> names = new ArrayList<>();
            for (ProjectRecord record : ephemeralData.getAllProjectRecords()) {
                names.add(record.getName());
            }
            return TabCompleter.filterByPrefix(names, args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("list")) {
            return TabCompleter.filterByPrefix(List.of("installed", "available"), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
            return TabCompleter.filterByPrefix(removeCommand.getInstalledPluginNames(), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("clean")) {
            return TabCompleter.filterByPrefix(CONFIRM_COMPLETION, args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("remove")) {
            return TabCompleter.filterByPrefix(CONFIRM_COMPLETION, args[2]);
        }
        return Collections.emptyList();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (args.length == 0) {
            DefaultCommand defaultCommand = new DefaultCommand(this);
            return defaultCommand.execute(sender);
        }

        return commandService.interpretAndExecuteCommand(sender, label, args);
    }

    /**
     * This can be used to get the version of the plugin.
     * @return A string containing the version preceded by 'v'
     */
    public String getVersion() {
        return pluginVersion;
    }

    /**
     * Checks if the version is mismatched.
     * @return A boolean indicating if the version is mismatched.
     */
    public boolean isVersionMismatched() {
        String configVersion = this.getConfig().getString("version");
        if (configVersion == null) {
            return false;
        } else {
            return !configVersion.equalsIgnoreCase(this.getVersion());
        }
    }

    /**
     * Checks if debug is enabled.
     * @return Whether debug is enabled.
     */
    public boolean isDebugEnabled() {
        return configService.getBoolean("debugMode");
    }

    public void reloadDpm() {
        reloadConfig();
        gitHubReleaseService.setApiToken(configService.getStringOrDefault("githubToken", ""));
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

    /**
     * Initializes Ponder's command service with the plugin's commands.
     */
    private void initializeCommandService() {
        ArrayList<AbstractPluginCommand> commands = new ArrayList<>(Arrays.asList(
                new HelpCommand(),
                new GetCommand(ephemeralData, downloadService, pluginFolderService, versionStore, this),
                new ListCommand(ephemeralData, pluginFolderService, versionStore),
                new StatsCommand(ephemeralData),
                new CleanCommand(ephemeralData, pluginFolderService, this),
                new UpdateCommand(ephemeralData, downloadService, pluginFolderService, versionStore, this),
                new InfoCommand(ephemeralData, gitHubReleaseService, pluginFolderService, versionStore, this),
                new ReloadCommand(this),
                removeCommand = new RemoveCommand(ephemeralData, pluginFolderService, versionStore)
        ));
        commandService.initialize(commands, "That command wasn't found.");
    }
}