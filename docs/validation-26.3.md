# Validation record: 1.1.27-n624.1-rc.2

Date: 2026-10-15 (Japan time). This is a release candidate, not a stable compatibility guarantee.

Build on a separate development host; execute on a Debian 13 VM with two vCPUs and 4 GiB RAM.
Java: Temurin 25.0.4.1+1. Runtime versions are listed in [26.3.md](26.3.md).

| Check | Result |
| --- | --- |
| Local Gradle build, Checkstyle, license checks and SpotBugs tasks | Passed; upstream optional-class warnings remain |
| Offline rebuild with previously downloaded mappings/dependencies | Passed |
| Generator runner regression tests | 3 passed: large merged output/closed stdin, nonzero exit, timeout cleanup |
| Original unread-pipe behavior with a 512 KiB output fixture | Reproduced blocking; the new runner completes |
| Upstream JAR with Velocity 4.2.0 | Startup and Limbo creation passed |
| Fork with Velocity, Geyser and Floodgate | Startup and Limbo creation passed |
| Protocol 777 login, chunks and teleport acknowledgements | Passed with MCProtocolLib from the tested Geyser build |
| Limbo → Paper → Limbo → Paper | Passed; two backend connections observed |
| 120-second RC1 protocol connection | 4 play-state logins, 194 chunk packets, 6 teleport confirmations, 0 codec errors; connection remained alive |
| 25-second RC2 protocol connection | 4 play-state logins, 194 chunk packets, 4 teleport confirmations, 0 codec errors; connection remained alive |
| First-configuration trim registry regression | RC1 fails with 11 materials / 0 patterns; RC2 passes with 11 materials / 18 patterns and matching pattern assets/descriptions |
| Block state ID 35722 in received chunk palettes | Decoded as 35722 |
| Geyser RakNet discovery on guest UDP 19132 | Unconnected pong received |
| Controller plugin compiled against the fork API and Velocity 4.2.0 | Passed; separate Maven validation profile and 9 existing tests passed |
| Controller with a guest-local mock API | Whitelist synchronization and observations received |
| Whitelisted offline Java identity | Rejected before play state; no join request reached the mock API |
| Authenticated vanilla Java 26.3 login and round trip | Passed on RC1; RC2 real-client retest pending |
| Actual Bedrock login through Floodgate | RC1 transfers and round trip observed, but initial Geyser recipe initialization throws; RC2 real-client retest pending |
| Authenticated Controller waiting/ready/transfer flow | Pending real clients and validation identities |
| Production deployment, Cloudflare path and multi-hour load | Not performed |

The protocol probes use an isolated offline-mode proxy and the complete protocol-777
packet codec from Geyser. They do not replace vanilla registry reference validation,
rendering, Microsoft authentication, Xbox authentication or Bedrock input testing.
RakNet discovery alone is not a successful Bedrock login.

## Initial Geyser configuration fix

RC1 omitted `minecraft:trim_pattern` when sending the initial Limbo configuration.
Geyser 2.12.0 build 1256 iterates both trim registries when configuration finishes;
the missing pattern registry throws `Registry must be frozen to access its entries`.
The client could still reach Paper, so successful transfer alone did not detect this.
RC2 sends all 18 vanilla 26.3 trim patterns with their asset IDs, descriptions and
decal flags. Earlier protocol versions are unchanged by this registry addition.

The protocol probe now checks the first configuration before Paper has supplied its
registries. The same probe rejects RC1 and accepts RC2 while the existing palette
and transfer checks continue to pass. Source:
[Geyser configuration translator](https://github.com/GeyserMC/Geyser/blob/fc2170c/core/src/main/java/org/geysermc/geyser/translator/protocol/java/JavaFinishConfigurationTranslator.java),
[trim recipe initialization](https://github.com/GeyserMC/Geyser/blob/fc2170c/core/src/main/java/org/geysermc/geyser/inventory/recipe/TrimRecipes.java).

## Waiting room behavior

The validation and Controller handlers call `disableFalling()` without correcting
player positions. A real Java client could move after repeatedly pressing space.
Strict position locking is not implemented by these handlers. Proxy command
suggestions require `announce-proxy-commands = true`; that setting was corrected
in the disposable validation VM, and the Java client confirmed `/limbo-test` suggestions.

The fork retains upstream's previous-gamemode workaround and its placeholder registry
content. Their client-visible effects remain part of the real-client checks. Earlier
Minecraft versions have not been covered by runtime regression tests in this session.

Artifacts and their checksums are attached to the fork's prerelease. Build dependencies
still include upstream's Velocity 4.1.2-SNAPSHOT compile dependency; this is distinct
from the tested Velocity 4.2.0 runtime. Revalidate before changing either.

The test VM and all Minecraft processes are stopped between sessions. Service units
are not enabled at boot. Restore/start them explicitly for the remaining client tests.
