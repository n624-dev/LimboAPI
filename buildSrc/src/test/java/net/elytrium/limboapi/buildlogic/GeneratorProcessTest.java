/* SPDX-License-Identifier: AGPL-3.0-or-later */
package net.elytrium.limboapi.buildlogic;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class GeneratorProcessTest {
  @Rule public TemporaryFolder temporary = new TemporaryFolder();

  private List<String> command(String mode) throws Exception {
    return List.of(new File(System.getProperty("java.home"), "bin/java").toString(), "-cp",
        new File(Fixture.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString(),
        Fixture.class.getName(), mode);
  }

  @Test public void drainsOutputLargerThanAPipeAndClosesStdin() throws Exception {
    File working = temporary.newFolder("directory with spaces");
    File log = new File(working, "generator.log");
    GeneratorProcess.run(command("output"), working, log, 15);
    String output = Files.readString(log.toPath());
    assertTrue(output.length() > 256 * 1024);
    assertTrue(output.endsWith("STDIN_CLOSED\n"));
  }

  @Test public void reportsNonzeroExitWithTheLogLocation() throws Exception {
    File log = temporary.newFile("failure.log");
    IOException error = assertThrows(IOException.class,
        () -> GeneratorProcess.run(command("failure"), temporary.getRoot(), log, 15));
    assertTrue(error.getMessage().contains("exited with 7"));
    assertTrue(error.getMessage().contains(log.toString()));
    assertTrue(Files.readString(log.toPath()).contains("fixture failure"));
  }

  @Test public void terminatesAGeneratorThatExceedsTheDeadline() throws Exception {
    File log = temporary.newFile("timeout.log");
    IOException error = assertThrows(IOException.class,
        () -> GeneratorProcess.run(command("timeout"), temporary.getRoot(), log, 2));
    assertTrue(error.getMessage().contains("timed out"));
    long pid = Long.parseLong(Files.readString(log.toPath()).trim());
    assertFalse(ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false));
  }

  public static final class Fixture {
    public static void main(String[] args) throws Exception {
      switch (args[0]) {
        case "output":
          for (int index = 0; index < 1024; ++index) {
            System.out.println("o".repeat(256));
            System.err.println("e".repeat(256));
          }
          if (System.in.read() == -1) {
            System.out.print("STDIN_CLOSED\n");
          }
          break;
        case "failure":
          System.err.println("fixture failure");
          System.exit(7);
          break;
        case "timeout":
          System.out.println(ProcessHandle.current().pid());
          System.out.flush();
          Thread.sleep(30_000);
          break;
        default:
          throw new IllegalArgumentException(args[0]);
      }
    }
  }
}
