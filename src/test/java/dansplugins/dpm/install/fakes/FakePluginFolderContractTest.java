package dansplugins.dpm.install.fakes;

import dansplugins.dpm.install.ports.PluginFolder;
import dansplugins.dpm.install.ports.PluginFolderContract;

import java.io.File;
import java.nio.file.Path;

class FakePluginFolderContractTest extends PluginFolderContract {
    @Override
    protected PluginFolder folderWith(Path dir, String... jarNames) {
        return new FakePluginFolder(dir.toString() + File.separator).with(jarNames);
    }
}
