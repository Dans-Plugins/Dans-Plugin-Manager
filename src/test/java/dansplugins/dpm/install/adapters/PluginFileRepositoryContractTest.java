package dansplugins.dpm.install.adapters;

import dansplugins.dpm.install.ports.PluginFolder;
import dansplugins.dpm.install.ports.PluginFolderContract;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

class PluginFileRepositoryContractTest extends PluginFolderContract {
    @Override
    protected PluginFolder folderWith(Path dir, String... jarNames) throws Exception {
        for (String name : jarNames) Files.createFile(dir.resolve(name));
        return new PluginFileRepository(dir.toString() + File.separator);
    }
}
