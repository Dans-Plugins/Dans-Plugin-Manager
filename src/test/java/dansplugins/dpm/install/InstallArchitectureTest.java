package dansplugins.dpm.install;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Keeps the install pipeline's layers honest by reading its source files.
 *
 * <p>Dependencies point inward: {@code core} and {@code ports} may not reach adapters, the
 * composition root, Bukkit, the network, the filesystem, or the plugin class. Each port and each
 * adapter file declares exactly one top-level type. The self-tests at the bottom plant a violation
 * in a string to prove each check can fail.
 */
class InstallArchitectureTest {

    private static final Path INSTALL = Paths.get("src/main/java/dansplugins/dpm/install");

    /** Packages and classes the inner layers must never mention (import or fully-qualified use). */
    private static final List<String> FORBIDDEN_INWARD = Arrays.asList(
            "dansplugins.dpm.install.adapters",
            "dansplugins.dpm.install.InstallService",
            "dansplugins.dpm.DansPluginManager",
            "dansplugins.dpm.utils.Logger",
            "dansplugins.dpm.repositories.ConfigRepository",
            "org.bukkit",
            "java.net",
            "java.io",
            "java.nio.file");

    private static final Pattern TOP_LEVEL_TYPE = Pattern.compile(
            "^(?:public\\s+|final\\s+|abstract\\s+)*(?:class|interface|enum|record)\\s+\\w+", Pattern.MULTILINE);

    // -------------------------------------------------------------------------
    // dependency direction
    // -------------------------------------------------------------------------

    @Test
    void coreAndPorts_neverReachOutward() throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path file : javaFiles("core", "ports")) {
            violations.addAll(inwardViolations(file.toString(), read(file)));
        }
        assertEquals(new ArrayList<>(), violations);
    }

    // -------------------------------------------------------------------------
    // one concept per file
    // -------------------------------------------------------------------------

    @Test
    void portsAndAdapters_declareOneTopLevelTypePerFile() throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path file : javaFiles("ports", "adapters")) {
            int types = topLevelTypeCount(read(file));
            if (types != 1) violations.add(file + " declares " + types + " top-level types");
        }
        assertEquals(new ArrayList<>(), violations);
    }

    @Test
    void ports_areAllInterfaces() throws IOException {
        for (Path file : javaFiles("ports")) {
            assertTrue(read(file).contains("public interface "), file + " must declare an interface");
        }
    }

    // -------------------------------------------------------------------------
    // the checks can fail
    // -------------------------------------------------------------------------

    @Test
    void inwardViolations_reportsAnAdapterImportPlantedInCore() {
        String planted = "package dansplugins.dpm.install.core;\n"
                + "import dansplugins.dpm.install.adapters.HttpJarDownloader;\n"
                + "class X { java.io.File f; }\n";
        assertEquals(2, inwardViolations("Planted.java", planted).size());
    }

    @Test
    void topLevelTypeCount_reportsASecondTypePlantedInOneFile() {
        String planted = "package p;\npublic interface A {}\nclass B {}\n";
        assertEquals(2, topLevelTypeCount(planted));
    }

    @Test
    void topLevelTypeCount_ignoresNestedTypes() {
        String nested = "package p;\npublic class A {\n    public enum Outcome { X }\n    static final class B {}\n}\n";
        assertEquals(1, topLevelTypeCount(nested));
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    static List<String> inwardViolations(String name, String source) {
        String code = stripComments(source);
        List<String> violations = new ArrayList<>();
        for (String forbidden : FORBIDDEN_INWARD) {
            if (Pattern.compile("\\b" + Pattern.quote(forbidden) + "\\b").matcher(code).find()) {
                violations.add(name + " mentions " + forbidden);
            }
        }
        return violations;
    }

    static int topLevelTypeCount(String source) {
        Matcher m = TOP_LEVEL_TYPE.matcher(stripComments(source));
        int count = 0;
        while (m.find()) count++;
        return count;
    }

    // Doc comments may name adapters (e.g. "{@code DownloadService.NETWORK_ERROR}"); only code counts.
    private static String stripComments(String source) {
        return source.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\\n]*", "");
    }

    private static List<Path> javaFiles(String... layers) throws IOException {
        List<Path> files = new ArrayList<>();
        for (String layer : layers) {
            Path dir = INSTALL.resolve(layer);
            assertTrue(Files.isDirectory(dir), "missing layer " + dir);
            try (Stream<Path> s = Files.list(dir)) {
                files.addAll(s.filter(p -> p.toString().endsWith(".java")).sorted().collect(Collectors.toList()));
            }
        }
        assertFalse(files.isEmpty(), "no sources found under " + INSTALL);
        return files;
    }

    private static String read(Path file) throws IOException {
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }
}
