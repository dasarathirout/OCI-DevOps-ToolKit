package dasarathi.devops.toolkit.cli;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CliExecutorHandler {
  private static final Logger LOG = Logger.getInstance(CliExecutorHandler.class);

  private CliExecutorHandler() {
    /*NOOP*/
  }

  public record OciCliResult(
      boolean success, int exitCode, List<String> output, List<String> errors) {}

  /** Execute an OCI CLI command and return structured output. */
  public static OciCliResult execute(Project project, List<String> command, String logContext) {
    Process process = null;
    try {
      List<String> resolvedCommand = OciCliFinder.resolveCommand(project, command);
      GeneralCommandLine commandLine = new GeneralCommandLine(resolvedCommand);
      commandLine.withRedirectErrorStream(true);
      commandLine.withEnvironment(System.getenv());
      process = commandLine.createProcess();
      List<String> outputLines = readLines(process);
      int exitCode = process.waitFor();
      boolean success = exitCode == 0;
      if (!success) {
        LOG.warn(String.format("%s failed: %s", logContext, String.join(" ", resolvedCommand)));
      }
      return new OciCliResult(success, exitCode, outputLines, Collections.emptyList());
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      LOG.error(logContext, ex);
      String message = ex.getMessage() == null ? ex.toString() : ex.getMessage();
      return new OciCliResult(false, -1, Collections.emptyList(), List.of(message));
    } catch (Exception ex) {
      LOG.error(logContext, ex);
      String message = ex.getMessage() == null ? ex.toString() : ex.getMessage();
      return new OciCliResult(false, -1, Collections.emptyList(), List.of(message));
    } finally {
      if (process != null) {
        process.destroy();
      }
    }
  }

  private static List<String> readLines(Process process) throws IOException {
    List<String> lines = new ArrayList<>();
    try (BufferedReader reader =
        new BufferedReader(
            new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        lines.add(line);
      }
    }
    return lines;
  }

  public static Runnable noopRunnable() {
    return () -> {};
  }

  public static void noopSink(String value) {
    LOG.info(String.format("Noop Sinking %s", value));
  }
}
