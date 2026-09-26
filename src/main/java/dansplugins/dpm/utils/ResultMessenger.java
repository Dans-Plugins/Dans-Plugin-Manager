package dansplugins.dpm.utils;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import java.util.logging.Logger;

/**
 * Delivers the result of an asynchronous command to its sender and, unless that sender is the
 * server console, mirrors it to the plugin log.
 *
 * An RCON sender only receives output produced while the command is being dispatched, so a result
 * that arrives later from an async task never reaches it. Mirroring keeps the outcome recoverable
 * from latest.log however the command was invoked. The console is skipped because its messages
 * already land in the log.
 */
public class ResultMessenger {
    private final Logger logger;

    public ResultMessenger(Logger logger) {
        this.logger = logger;
    }

    public void send(CommandSender sender, String message) {
        sender.sendMessage(message);
        if (sender instanceof ConsoleCommandSender) return;
        logger.info("[to " + sender.getName() + "] " + ChatColor.stripColor(message));
    }
}
