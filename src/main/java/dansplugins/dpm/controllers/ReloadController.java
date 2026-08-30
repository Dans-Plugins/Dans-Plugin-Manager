package dansplugins.dpm.controllers;

import dansplugins.dpm.repositories.ConfigRepository;
import dansplugins.dpm.repositories.GitHubReleaseRepository;

// Orchestrates /dpm reload: re-reads the config file and re-applies the settings the GitHub
// release repository holds in memory. ReloadCommand reports the outcome to the sender.
public class ReloadController {
    private final Runnable configReloader;
    private final ConfigRepository configRepository;
    private final GitHubReleaseRepository gitHubReleaseRepository;

    public ReloadController(Runnable configReloader, ConfigRepository configRepository,
                            GitHubReleaseRepository gitHubReleaseRepository) {
        this.configReloader = configReloader;
        this.configRepository = configRepository;
        this.gitHubReleaseRepository = gitHubReleaseRepository;
    }

    // Copies the config-backed settings into the release repository. Called once on enable, and
    // again as part of every reload, so the two paths cannot drift apart.
    public void applySettings() {
        gitHubReleaseRepository.setApiToken(configRepository.getStringOrDefault("githubToken", ""));
        gitHubReleaseRepository.setExperimentalTag(configRepository.getStringOrDefault("experimentalReleaseTag",
                GitHubReleaseRepository.DEFAULT_EXPERIMENTAL_TAG));
    }

    // Cached releases were fetched under the previous token and experimental tag, so the cache is
    // dropped after the new settings are applied rather than before — a fetch racing the reload
    // must not repopulate it from the old configuration.
    public void reload() {
        configReloader.run();
        applySettings();
        gitHubReleaseRepository.clearCache();
    }
}
