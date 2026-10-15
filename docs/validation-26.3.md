# Validation record: 1.1.27-n624.1-rc.1

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
| 120-second protocol connection | 4 play-state logins, 194 chunk packets, 6 teleport confirmations, 0 codec errors; connection remained alive |
| Block state ID 35722 in received chunk palettes | Decoded as 35722 |
| Geyser RakNet discovery on guest UDP 19132 | Unconnected pong received |
| Controller plugin compiled against the fork API and Velocity 4.2.0 | Passed; separate Maven validation profile and 9 existing tests passed |
| Controller with a guest-local mock API | Whitelist synchronization and observations received |
| Whitelisted offline Java identity | Rejected before play state; no join request reached the mock API |
| Authenticated vanilla Java 26.3 login and rendering | Pending real client |
| Actual Bedrock login through Floodgate | Pending real client |
| Authenticated Controller waiting/ready/transfer flow | Pending real clients and validation identities |
| Production deployment, Cloudflare path and multi-hour load | Not performed |

The protocol probes use an isolated offline-mode proxy and the complete protocol-777
packet codec from Geyser. They do not replace vanilla registry reference validation,
rendering, Microsoft authentication, Xbox authentication or Bedrock input testing.
RakNet discovery alone is not a successful Bedrock login.

The fork retains upstream's previous-gamemode workaround and its placeholder registry
content. Their client-visible effects remain part of the real-client checks. Earlier
Minecraft versions have not been covered by runtime regression tests in this session.

Artifacts and their checksums are attached to the fork's prerelease. Build dependencies
still include upstream's Velocity 4.1.2-SNAPSHOT compile dependency; this is distinct
from the tested Velocity 4.2.0 runtime. Revalidate before changing either.

The test VM and all Minecraft processes are stopped between sessions. Service units
are not enabled at boot. Restore/start them explicitly for the remaining client tests.
