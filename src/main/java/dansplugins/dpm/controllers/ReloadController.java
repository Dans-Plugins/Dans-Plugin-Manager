package dansplugins.dpm.controllers;

import dansplugins.dpm.repositories.ConfigRepository;
import dansplugins.dpm.repositories.GitHubReleaseRepository;

import java.util.logging.Logger;

// Orchestrates /dpm reload: re-reads the config file and re-applies the settings the GitHub
// release repository holds in memory. ReloadCommand reports the outcome to the sender.
public class ReloadController {
    private final Runnable configReloader;
    private final ConfigRepository configRepository;
    private final GitHubReleaseRepository gitHubReleaseRepository;
    private final Logger logger;

    public ReloadController(Runnable configReloader, ConfigRepository configRepository,
                            GitHubReleaseRepository gitHubReleaseRepository, Logger logger) {
        this.configReloader = configReloader;
        this.configRepository = configRepository;
        this.gitHubReleaseRepository = gitHubReleaseRepository;
        this.logger = logger;
    }

    // Copies the config-backed settings into the release repository. Called once on enable, and
    // again as part of every reload, so the two paths cannot drift apart.
    public void applySettings() {
        String token = configRepository.getStringOrDefault("githubToken", "");
        gitHubReleaseRepository.setApiToken(token);
        gitHubReleaseRepository.setExperimentalTag(configRepository.getStringOrDefault("experimentalReleaseTag",
                GitHubReleaseRepository.DEFAULT_EXPERIMENTAL_TAG));
        // An empty token is a valid configuration that fails silently later: unauthenticated
        // lookups share a 60/hour budget, and a batch update can exhaust it midway through the
        // plugin list. Saying so here, on every start and reload, is the only place the operator
        // hears about it before a rate-limited run reports "could not reach GitHub" (#126).
        if (token.trim().isEmpty()) {
            logger.warning("[DPM] githubToken is not set in config.yml — GitHub API requests are unauthenticated"
                    + " and limited to 60 per hour, which one /dpm update across many plugins can exhaust."
                    + " Set githubToken and run /dpm reload to raise the limit to 5 000 per hour.");
        }
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
