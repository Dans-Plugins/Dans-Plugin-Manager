/**
 * The install pipeline behind {@code /dpm get} and {@code /dpm update}, as a pure core plus
 * ports and adapters.
 *
 * <pre>
 *   GetCommand / UpdateCommand            (Bukkit: async scheduling, chat output)
 *          |
 *          v
 *   InstallService                        composition root, built in DansPluginManager.onEnable
 *          |
 *          v
 *   core/   DownloadService, GetController, UpdateController, DependencyResolutionService
 *          |            (policy only: no files, network, Bukkit or plugin class)
 *          v
 *   ports/  ReleaseSource  JarDownloader  PluginFolder  VersionStore  ChannelStore  Notifier  DebugLog
 *          ^
 *          |  implements
 *   adapters/ (and utils.Logger)
 * </pre>
 *
 * <table>
 *   <caption>Ports and their adapters</caption>
 *   <tr><th>Port</th><th>Production adapter</th><th>Test fake</th></tr>
 *   <tr><td>ReleaseSource</td><td>adapters.GitHubReleaseRepository</td><td>FakeReleaseSource</td></tr>
 *   <tr><td>JarDownloader</td><td>adapters.HttpJarDownloader</td><td>FakeJarDownloader</td></tr>
 *   <tr><td>PluginFolder</td><td>adapters.PluginFileRepository</td><td>FakePluginFolder</td></tr>
 *   <tr><td>VersionStore</td><td>adapters.VersionRepository</td><td>FakeVersionStore</td></tr>
 *   <tr><td>ChannelStore</td><td>adapters.ChannelRepository</td><td>FakeChannelStore</td></tr>
 *   <tr><td>Notifier</td><td>adapters.DiscordNotificationService</td><td>FakeNotifier</td></tr>
 *   <tr><td>DebugLog</td><td>utils.Logger</td><td>FakeDebugLog</td></tr>
 * </table>
 *
 * <p>Rules, enforced by {@code InstallArchitectureTest}: {@code core} and {@code ports} never
 * mention {@code adapters}, {@code InstallService}, Bukkit, {@code java.net}, {@code java.io},
 * {@code java.nio.file} or the plugin class; each port and adapter file declares one top-level type.
 * Every port has an abstract contract test in {@code src/test/.../install/ports} that both its fake
 * and its production adapter extend.
 *
 * <p><b>Adding a new X</b> (say, a release source other than GitHub): write one adapter file in
 * {@code adapters/} that implements the port, add a {@code *ContractTest} extending the port's
 * contract, and choose it in {@link dansplugins.dpm.install.InstallService} (or in
 * {@code DansPluginManager}, where the shared instances are built).
 */
package dansplugins.dpm.install;
