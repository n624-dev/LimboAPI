/* SPDX-License-Identifier: AGPL-3.0-or-later */
package net.elytrium.limboapi.buildlogic;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Runs Minecraft's data generator without leaving unread output pipes. */
public final class GeneratorProcess {
  private GeneratorProcess() {
  }

  public static void run(List<String> command, File directory, File log, long timeoutSeconds)
      throws IOException, InterruptedException {
    Process process = new ProcessBuilder(command)
        .directory(directory)
        .redirectErrorStream(true)
        .redirectOutput(log)
        .start();
    try {
      process.getOutputStream().close();
      if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
        throw new IOException("Minecraft data generator timed out. See " + log);
      }
      if (process.exitValue() != 0) {
        throw new IOException("Minecraft data generator exited with " + process.exitValue() + ". See " + log);
      }
    } finally {
      if (process.isAlive()) {
        process.descendants().forEach(ProcessHandle::destroyForcibly);
        process.destroyForcibly();
        process.waitFor();
      }
    }
  }
}
