package dansplugins.dpm.utils;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LoggerTest {

    // -------------------------------------------------------------------------
    // warn()
    // -------------------------------------------------------------------------

    @Test
    void warn_alwaysOutputsRegardlessOfDebugMode() {
        Logger logger = new Logger(null);

        PrintStream original = System.out;
        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capture));
        try {
            logger.warn("something went wrong");
        } finally {
            System.setOut(original);
        }
        assertTrue(capture.toString().contains("[DPM] something went wrong"),
                "warn() must always print regardless of debugMode");
    }
}
