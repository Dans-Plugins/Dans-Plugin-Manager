package dansplugins.dpm.commands;

import dansplugins.dpm.controllers.RemoveController;
import dansplugins.dpm.controllers.RemoveController.RemovalPreview;
import dansplugins.dpm.controllers.RemoveController.RemovalResult;
import dansplugins.dpm.objects.ProjectRecord;
import dansplugins.dpm.repositories.ProjectRecordRepository;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import preponderous.ponder.minecraft.bukkit.abs.AbstractPluginCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RemoveCommand extends AbstractPluginCommand {
    private final ProjectRecordRepository projectRecordRepository;
    private final RemoveController removeController;

    public RemoveCommand(ProjectRecordRepository projectRecordRepository, RemoveController removeController) {
        super(new ArrayList<>(List.of("remove")), new ArrayList<>(List.of("dpm.remove")));
        this.projectRecordRepository = projectRecordRepository;
        this.removeController = removeController;
    }

    @Override
    public boolean execute(CommandSender sender) {
        sender.sendMessage(ChatColor.RED + "Usage: /dpm remove <plugin-name> [--confirm]");
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

        boolean confirmed = args.length >= 2 && args[1].equalsIgnoreCase("--confirm");
        if (!confirmed) {
            return previewRemoval(sender, record);
        }
        return performRemoval(sender, record);
    }

    private boolean previewRemoval(CommandSender sender, ProjectRecord record) {
        RemovalPreview preview = removeController.preview(record);
        if (!preview.isInstalled()) {
            sender.sendMessage(ChatColor.YELLOW + record.getName() + " is not installed.");
            return true;
        }
        if (!preview.getDependents().isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "Warning: " + formatList(preview.getDependents())
                    + " declare a hard dependency on " + record.getName() + ".");
            sender.sendMessage(ChatColor.YELLOW + "Removing " + record.getName()
                    + " may cause those plugins to stop working.");
        }
        sender.sendMessage(ChatColor.YELLOW + "This will delete " + ChatColor.WHITE + preview.getJar().getName() + ChatColor.YELLOW + " from the plugins folder.");
        sender.sendMessage(ChatColor.YELLOW + "Run " + ChatColor.WHITE + "/dpm remove " + record.getName() + " --confirm" + ChatColor.YELLOW + " to proceed.");
        return true;
    }

    private boolean performRemoval(CommandSender sender, ProjectRecord record) {
        RemovalResult result = removeController.remove(record);
        if (result.getOutcome() == RemoveController.Outcome.NOT_INSTALLED) {
            sender.sendMessage(ChatColor.YELLOW + record.getName() + " is not installed.");
            return true;
        }
        if (!result.getDependents().isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "Warning: " + formatList(result.getDependents())
                    + " declare a hard dependency on " + record.getName() + " and may stop working.");
        }
        if (result.getOutcome() == RemoveController.Outcome.DELETED) {
            sender.sendMessage(ChatColor.GREEN + "Removed " + record.getName() + ".");
            sender.sendMessage(ChatColor.YELLOW + "Restart the server for the removal to take effect.");
            sender.sendMessage(ChatColor.YELLOW + "To reinstall, run " + ChatColor.WHITE + "/dpm get " + record.getName() + ChatColor.YELLOW + ".");
        } else {
            sender.sendMessage(ChatColor.RED + "Failed to delete " + result.getJar().getName() + ". Check server file permissions.");
        }
        return true;
    }

    public List<String> getInstalledPluginNames() {
        return removeController.getInstalledPlugins()
                .stream().map(ProjectRecord::getName).collect(Collectors.toList());
    }

    private String formatList(List<String> names) {
        if (names.size() == 1) return names.get(0);
        if (names.size() == 2) return names.get(0) + " and " + names.get(1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < names.size() - 1; i++) {
            sb.append(names.get(i)).append(", ");
        }
        sb.append("and ").append(names.get(names.size() - 1));
        return sb.toString();
    }
}
