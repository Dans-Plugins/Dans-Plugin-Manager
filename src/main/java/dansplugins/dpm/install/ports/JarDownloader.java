package dansplugins.dpm.install.ports;

/**
 * Copies a plugin jar from a URL into a file on disk.
 *
 * <p>Returns the number of bytes written ({@code 0} for an empty source), or
 * {@link #NETWORK_ERROR} if the URL could not be opened, or {@link #FILE_ERROR} if the bytes could
 * not be written; on {@code FILE_ERROR} no partial file is left behind. Never throws. Blocks on
 * the network.
 */
public interface JarDownloader {
    /** The URL could not be opened. Same value as {@code DownloadService.NETWORK_ERROR}. */
    int NETWORK_ERROR = -4;
    /** The destination could not be written. Same value as {@code DownloadService.FILE_ERROR}. */
    int FILE_ERROR = -5;

    int download(String url, String destPath);
}
