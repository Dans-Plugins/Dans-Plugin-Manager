package dansplugins.dpm;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import dansplugins.dpm.commands.DefaultCommand;
import dansplugins.dpm.commands.GetCommand;
import dansplugins.dpm.commands.HelpCommand;
import dansplugins.dpm.commands.ListCommand;
import dansplugins.dpm.factories.ProjectRecordFactory;
import dansplugins.dpm.services.LocalConfigService;
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
    private static DansPluginManager instance;
    private final String pluginVersion = "v" + getDescription().getVersion();
    private final CommandService commandService = new CommandService(getPonder());

    /**
     * This can be used to get the instance of the main class that is managed by itself.
     * @return The managed instance of the main class.
     */
    public static DansPluginManager getInstance() {
        return instance;
    }

    /**
     * This runs when the server starts.
     */
    @Override
    public void onEnable() {
        instance = this;
        initializeConfig();
        initializeCommandService();
        initializeProjectRecords();
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
            DefaultCommand defaultCommand = new DefaultCommand();
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
        return LocalConfigService.getInstance().getBoolean("debugMode");
    }

    private void initializeConfig() {
        if (configFileExists()) {
            performCompatibilityChecks();
        }
        else {
            LocalConfigService.getInstance().saveMissingConfigDefaultsIfNotPresent();
        }
    }

    private boolean configFileExists() {
        return new File("./plugins/" + getName() + "/config.yml").exists();
    }

    private void performCompatibilityChecks() {
        if (isVersionMismatched()) {
            LocalConfigService.getInstance().saveMissingConfigDefaultsIfNotPresent();
        }
        reloadConfig();
    }

    /**
     * Initializes Ponder's command service with the plugin's commands.
     */
    private void initializeCommandService() {
        ArrayList<AbstractPluginCommand> commands = new ArrayList<>(Arrays.asList(
                new HelpCommand(),
                new GetCommand(),
                new ListCommand()
        ));
        commandService.initialize(commands, "That command wasn't found.");
    }

    private void initializeProjectRecords() {
        createRecord("medievalfactions", "https://github.com/Dans-Plugins/Medieval-Factions/releases/download/v4.6.2/Medieval-Factions-4.6.2.jar");
        createRecord("simpleskills", "https://github.com/Dans-Plugins/SimpleSkills/releases/download/v2.0/SimpleSkills-2.0.jar");
        createRecord("wildpets", "https://github.com/Dans-Plugins/Wild-Pets/releases/download/1.4/WildPets-1.4.jar");
        createRecord("currencies", "https://github.com/Dans-Plugins/Currencies/releases/download/v1.2/Currencies-1.2.jar");
        createRecord("foodspoilage", "https://github.com/Dans-Plugins/FoodSpoilage/releases/download/v2.0/FoodSpoilage-v2.0.jar");
    }

    private void createRecord(String name, String link) {
        ProjectRecordFactory.getInstance().createProjectRecord(name, link);
    }
}
