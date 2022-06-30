package dansplugins.dpm;

import dansplugins.dpm.commands.*;
import dansplugins.dpm.data.EphemeralData;
import dansplugins.dpm.factories.ProjectRecordFactory;
import dansplugins.dpm.services.ConfigService;
import dansplugins.dpm.services.DownloadService;
import dansplugins.dpm.utils.Logger;
import dansplugins.dpm.utils.ProjectRecordInitializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import preponderous.ponder.minecraft.bukkit.abs.AbstractPluginCommand;
import preponderous.ponder.minecraft.bukkit.abs.PonderBukkitPlugin;
import preponderous.ponder.minecraft.bukkit.services.CommandService;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author Daniel McCoy Stephenson
 */
public final class DansPluginManager extends PonderBukkitPlugin {
    private final String pluginVersion = "v" + getDescription().getVersion();

    private final CommandService commandService = new CommandService(getPonder());
    private final EphemeralData ephemeralData = new EphemeralData();
    private final ProjectRecordFactory projectRecordFactory = new ProjectRecordFactory(ephemeralData);
    private final ProjectRecordInitializer projectRecordInitializer = new ProjectRecordInitializer(projectRecordFactory);
    private final ConfigService configService = new ConfigService(this);
    private final Logger logger = new Logger(this);
    private final DownloadService downloadService = new DownloadService(logger);

    /**
     * This runs when the server starts.
     */
    @Override
    public void onEnable() {
        initializeConfig();
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
                new GetCommand(ephemeralData, downloadService),
                new ListCommand(ephemeralData),
                new StatsCommand(ephemeralData)
        ));
        commandService.initialize(commands, "That command wasn't found.");
    }
}