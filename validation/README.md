# Runtime validation tools

These tools are for a disposable test server. `SmokePlugin` automatically puts players
in Limbo and transfers them to a Velocity server named `backend` after five seconds.
Names starting with `Smoke` also make one automatic return trip from Paper to Limbo.
The `/limbo-test` command starts another trip. Do not install this plugin in production.

Build the plugin using Java 25 and the tested Velocity and LimboAPI JARs:

```sh
sh validation/build.sh /absolute/path/velocity.jar /absolute/path/limboapi.jar
```

The output is `build/limboapi-validation.jar`. Deploy it alongside LimboAPI in the test proxy.
The test world includes block state ID 35722 to exercise unsigned state ID handling.

`ProtocolProbe` uses MCProtocolLib bundled in the tested Geyser JAR. Compile it with the
Geyser and Velocity JARs on the classpath, then run it against a loopback test listener
on port 25563:

```sh
mkdir -p build/probe
javac --release 25 -proc:none -cp "$GEYSER_JAR:$VELOCITY_JAR" -d build/probe \
  validation/src/main/java/jp/n624/limbo/validation/ProtocolProbe.java
java -Xmx256m -cp "build/probe:$GEYSER_JAR:$VELOCITY_JAR" \
  jp.n624.limbo.validation.ProtocolProbe Smoke263 25
```

The probe requires protocol 777, decodes chunks and light data, reads chunk palettes,
checks that state ID 35722 survives transmission, echoes teleport coordinates and
rotation, and checks the connection stays alive. Before the first configuration
finishes it also requires both trim registries, including 18 pattern entries with
their matching asset IDs and descriptions. This detects the missing trim-pattern
registry that caused Geyser's first-connection recipe initialization to fail in RC1.
The duration is in seconds.
Offline probes require an isolated offline-mode proxy; they do not validate Microsoft
or Xbox authentication, rendering, or vanilla registry reference validation.

An additional `deny` argument checks the Japanese Controller rejection message and
requires that no play-state login/chunk was received. This mode is for a separate
Controller profile without `SmokePlugin`, using a guest-local mock Controller.

Power off the VM when validation ends. Keep it stopped between sessions.
