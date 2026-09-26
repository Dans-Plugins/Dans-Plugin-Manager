package dansplugins.dpm.utils;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultMessengerTest {

    // CommandSender has too many methods for an anonymous class to be readable, so the fake is a
    // dynamic proxy that records sendMessage(String) and answers getName().
    private static <T extends CommandSender> T sender(Class<T> type, String name, List<String> received) {
        Object proxy = Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (p, method, args) -> {
            if (method.getName().equals("sendMessage") && args != null && args[0] instanceof String) {
                received.add((String) args[0]);
            } else if (method.getName().equals("getName")) {
                return name;
            }
            return null;
        });
        return type.cast(proxy);
    }

    private static Logger capturingLogger(List<String> logged) {
        Logger logger = Logger.getLogger("ResultMessengerTest-" + System.nanoTime());
        logger.setUseParentHandlers(false);
        logger.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                logged.add(record.getMessage());
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        });
        return logger;
    }

    // -------------------------------------------------------------------------
    // send()
    // -------------------------------------------------------------------------

    @Test
    void send_rconSender_deliversMessageAndMirrorsItToLog() {
        List<String> received = new ArrayList<>();
        List<String> logged = new ArrayList<>();
        ResultMessenger messenger = new ResultMessenger(capturingLogger(logged));

        messenger.send(sender(RemoteConsoleCommandSender.class, "Rcon", received),
                ChatColor.GREEN + "herald is already up to date (dev-abc1234).");

        assertEquals(List.of(ChatColor.GREEN + "herald is already up to date (dev-abc1234)."), received);
        assertEquals(List.of("[to Rcon] herald is already up to date (dev-abc1234)."), logged,
                "an RCON sender has gone by the time an async result arrives, so the result must reach the log");
    }

    @Test
    void send_consoleSender_deliversMessageWithoutMirroringToLog() {
        List<String> received = new ArrayList<>();
        List<String> logged = new ArrayList<>();
        ResultMessenger messenger = new ResultMessenger(capturingLogger(logged));

        messenger.send(sender(ConsoleCommandSender.class, "CONSOLE", received), "Done: 1 downloaded.");

        assertEquals(List.of("Done: 1 downloaded."), received);
        assertTrue(logged.isEmpty(), "console output already lands in the log, so mirroring would duplicate it");
    }

    @Test
    void send_otherSender_stripsColorCodesFromLoggedCopy() {
        List<String> received = new ArrayList<>();
        List<String> logged = new ArrayList<>();
        ResultMessenger messenger = new ResultMessenger(capturingLogger(logged));

        messenger.send(sender(CommandSender.class, "Steve", received),
                ChatColor.WHITE + "Status: " + ChatColor.YELLOW + "Update available");

        assertEquals(List.of("[to Steve] Status: Update available"), logged);
    }
}
