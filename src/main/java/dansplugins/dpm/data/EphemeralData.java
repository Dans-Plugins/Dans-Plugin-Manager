package dansplugins.dpm.data;

import dansplugins.dpm.objects.ProjectRecord;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;

public class EphemeralData {
    private final ArrayList<ProjectRecord> projectRecords = new ArrayList<>();

    public ProjectRecord getProjectRecord(String name) {
        for (ProjectRecord projectRecord : projectRecords) {
            if (projectRecord.getName().equalsIgnoreCase(name)) {
                return projectRecord;
            }
        }
        return null;
    }

    public void addProjectRecord(ProjectRecord projectRecord) {
        projectRecords.add(projectRecord);
    }

    public boolean removeProjectRecord(ProjectRecord projectRecord) {
        return projectRecords.remove(projectRecord);
    }

    public void sendListOfProjectRecordsToCommandSender(CommandSender commandSender) {
        commandSender.sendMessage(ChatColor.AQUA + "=== Project Records ===");
        for (ProjectRecord projectRecord : projectRecords) {
            commandSender.sendMessage(ChatColor.AQUA + projectRecord.getName());
        }
    }

    public int getNumProjectRecords() {
        return projectRecords.size();
    }
}
