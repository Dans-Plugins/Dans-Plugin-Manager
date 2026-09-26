package dansplugins.dpm.commands;

import dansplugins.dpm.controllers.CleanController;
import dansplugins.dpm.controllers.CleanController.CleanResult;
import dansplugins.dpm.controllers.CleanController.Conflict;
import dansplugins.dpm.utils.ResultMessenger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import preponderous.ponder.minecraft.bukkit.abs.AbstractPluginCommand;

import java.util.ArrayList;
import java.util.List;

public class CleanCommand extends AbstractPluginCommand {
    private final CleanController cleanController;
    private final Plugin plugin;
    private final ResultMessenger messenger;

    public CleanCommand(CleanController cleanController, Plugin plugin) {
        super(new ArrayList<>(List.of("clean")), new ArrayList<>(List.of("dpm.clean")));
        this.cleanController = cleanController;
        this.plugin = plugin;
        this.messenger = new ResultMessenger(plugin.getLogger());
    }

    @Override
    public boolean execute(CommandSender sender) {
        sender.sendMessage(ChatColor.AQUA + "Scanning for duplicate plugin JARs...");
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<Conflict> conflicts = cleanController.findConflicts();
            Bukkit.getScheduler().runTask(plugin, () -> sendPreview(sender, conflicts));
        });
        return true;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("--confirm")) {
            sender.sendMessage(ChatColor.AQUA + "Removing duplicate plugin JARs...");
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                CleanResult result = cleanController.clean();
                Bukkit.getScheduler().runTask(plugin, () -> sendResult(sender, result));
            });
            return true;
        }
        return execute(sender);
    }

    private void sendPreview(CommandSender sender, List<Conflict> conflicts) {
        if (conflicts.isEmpty()) {
            messenger.send(sender, ChatColor.GREEN + "No duplicate JARs found.");
            return;
        }
        messenger.send(sender, ChatColor.YELLOW + "Found " + conflicts.size() + " duplicate JAR(s) to remove:");
        for (Conflict conflict : conflicts) {
            messenger.send(sender, ChatColor.AQUA + "  - " + label(conflict));
        }
        messenger.send(sender, ChatColor.YELLOW + "Run " + ChatColor.WHITE + "/dpm clean --confirm" + ChatColor.YELLOW + " to delete them.");
    }

    private void sendResult(CommandSender sender, CleanResult result) {
        if (result.isEmpty()) {
            messenger.send(sender, ChatColor.GREEN + "No duplicate JARs found.");
            return;
        }
        if (!result.getRemoved().isEmpty()) {
            messenger.send(sender, ChatColor.GREEN + "Removed " + result.getRemoved().size() + " duplicate JAR(s):");
            for (Conflict conflict : result.getRemoved()) {
                messenger.send(sender, ChatColor.AQUA + "  - " + label(conflict));
            }
            messenger.send(sender, ChatColor.YELLOW + "Restart the server to apply changes.");
        }
        if (!result.getFailed().isEmpty()) {
            messenger.send(sender, ChatColor.RED + "Failed to delete " + result.getFailed().size() + " JAR(s) — check server file permissions:");
            for (Conflict conflict : result.getFailed()) {
                messenger.send(sender, ChatColor.RED + "  - " + label(conflict));
            }
        }
    }

    private String label(Conflict conflict) {
        return conflict.getJar().getName() + " (" + conflict.getPluginName() + ")";
    }
}
