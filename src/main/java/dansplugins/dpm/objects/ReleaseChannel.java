package dansplugins.dpm.objects;

/**
 * Which stream of builds a plugin tracks.
 *
 * <p>{@link #STABLE} follows GitHub's "latest release" — the versions the maintainers have
 * tagged and published. {@link #EXPERIMENTAL} follows the rolling prerelease built from the
 * repository's main branch, which is unreviewed and may be broken.
 */
public enum ReleaseChannel {
    STABLE,
    EXPERIMENTAL;

    /** Lowercase name used in the channel store, config, and player-facing messages. */
    public String getDisplayName() {
        return name().toLowerCase();
    }

    /** Parses a stored or user-supplied channel name, falling back to {@link #STABLE}. */
    public static ReleaseChannel fromStored(String value) {
        if (value == null) return STABLE;
        for (ReleaseChannel channel : values()) {
            if (channel.name().equalsIgnoreCase(value.trim())) return channel;
        }
        return STABLE;
    }
}
