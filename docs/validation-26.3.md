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
| 300-second RC2 protocol connection | 4 play-state logins, 34 chunk packets, 4 teleport confirmations, 0 codec errors; connection remained alive |
| First-configuration trim registry regression | RC1 fails with 11 materials / 0 patterns; RC2 passes with 11 materials / 18 patterns and matching pattern assets/descriptions |
| Block state ID 35722 in received chunk palettes | Decoded as 35722 |
| Geyser RakNet discovery on guest UDP 19132 | Unconnected pong received |
| Controller plugin compiled against the fork API and Velocity 4.2.0 | Passed; 25 tests passed in both Java 21/default and Java 25/26.3 validation profiles |
| Controller failure/recovery component tests | 16 added tests passed: unlisted/wrong identity kinds, API 503/refusal/malformed responses, permit expiry/reuse, delayed response after disconnect/revocation, whitelist synchronization/backoff, deadlines and TCP reachability changes |
| Controller with a guest-local mock API | Whitelist synchronization and observations received |
| Whitelisted offline Java identity | Rejected before play state; no join request reached the mock API |
| Unlisted offline protocol-777 identity | Rejected before play state; 0 play logins/chunks/codec errors and no join request for the fixture identity |
| Paper stopped during the SmokePlugin transfer | Clean connection-failure disconnect after Limbo; 0 codec errors. After Paper restarted, a new 25-second connection and round trip passed |
| Authenticated vanilla Java 26.3 login and round trip | Passed on RC1 and RC2, including initial login and command-triggered round trip |
| Actual Bedrock login through Floodgate | RC2 authenticated login and round trip passed through linked Floodgate; no initial trim-registry exception |
| Authenticated Controller waiting/ready/transfer flow | Java 26.3 and linked Floodgate Bedrock passed against the guest-local mock; pending → connected observations confirmed |
| Controller command-triggered return | Java `/mcserver backend` returned Paper → Limbo → Paper; readiness changed after 15 seconds and transfer followed within the next poll |
| Already-ready backend on authenticated login | Java reached Paper directly without entering the waiting Limbo |
| Authenticated unlisted identities, unlinked Bedrock, API/backend outage paths | Not performed as real-client integration tests |
| Production deployment, Cloudflare path and multi-hour load | Not performed |

The protocol probes use an isolated offline-mode proxy and the complete protocol-777
packet codec from Geyser. They do not replace vanilla registry reference validation,
rendering, Microsoft authentication, Xbox authentication or Bedrock input testing.
RakNet discovery alone is not a successful Bedrock login.

The separate Controller failure tests invoke the plugin's actual admission handlers and
HTTP client against a loopback HTTP server. Player authentication results and the
Velocity/Limbo platform objects are simulated. Actual TCP sockets are opened and closed
for the backend observation check. These tests verify admission/recovery decisions;
they do not authenticate a client or establish a Velocity backend connection. No runtime
code changes were required by these added cases, and published RC2 artifacts are unchanged.
The VM backend-outage probe used the isolated offline SmokePlugin profile. It confirmed
an explicit disconnect on failed transfer and recovery on a new connection; it did not
test automatic retry of that disconnected session or Controller outage handling with
an authenticated real client.

The authenticated tests used an online-mode proxy. The Bedrock account was linked
to the tested Java identity through Floodgate; the matching validation whitelist entry
accepted both authenticated identity kinds. Controller responses came from a guest-local
mock API, not the production scheduler or power controller. Waiting observations contained
the connected client's identity before mock readiness changed, followed by a backend
connection and a connected observation. An initial fixture watcher timed out before the
human connected; changing the mock response then transferred the waiting client normally.

One resource sample with the Java client connected measured Paper RSS at about 991 MiB
and proxy RSS at about 811 MiB. Process-lifetime CPU averages were 3.0% and 3.4%; these
are single-session samples, not peak usage or a load benchmark.

During a 120-second portion of the additional single-client 300-second RC2 probe,
nine samples measured proxy RSS at 760.52–761.27 MiB and Paper RSS at 883.97–891.25 MiB.
CPU time deltas corresponded to 1.67% for the proxy and 2.95% for Paper, with one
fully occupied core defined as 100%. This bounded observation is not a multi-user
load test or evidence of long-term memory stability.

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
`/limbo-test` exists only in the minimal SmokePlugin profile. The separate Controller
profile uses `/mcserver backend`; it does not install SmokePlugin's automatic transfer.

The fork retains upstream's previous-gamemode workaround and its placeholder registry
content. Authenticated transfer checks do not verify every registry-dependent feature. Earlier
Minecraft versions have not been covered by runtime regression tests in this session.

Artifacts and their checksums are attached to the fork's prerelease. Build dependencies
still include upstream's Velocity 4.1.2-SNAPSHOT compile dependency; this is distinct
from the tested Velocity 4.2.0 runtime. Revalidate before changing either.

The test VM and all Minecraft processes are stopped between sessions. Service units
are not enabled at boot. Restore/start them explicitly for additional validation.
