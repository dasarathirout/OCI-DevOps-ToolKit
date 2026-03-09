package dasarathi.devops.toolkit.cli;

import static dasarathi.devops.toolkit.DevOpsToolKitToolWindowFactory.DEVOPS_TOOLKIT_REPOSITORY_OCI_CLI_KEY;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import dasarathi.devops.toolkit.core.DevOpsToolKitSettings;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class OciCliFinder {
  private static final Logger LOG = Logger.getInstance(OciCliFinder.class);
  private static final String OCI_COMMAND = "oci";
  private static final boolean IS_WINDOWS =
      System.getProperty("os.name", "").toLowerCase().contains("win");
  private static final List<String> EXECUTABLE_NAMES = executableNames();

  private OciCliFinder() {
    /*NOOP*/
  }

  public static List<String> resolveCommand(Project project, List<String> command) {
    if (command == null || command.isEmpty()) {
      return List.of();
    }

    String executable = command.getFirst();
    if (!OCI_COMMAND.equals(executable)) {
      return command;
    }

    String resolvedExecutable = findOciExecutable(project);
    if (executable.equals(resolvedExecutable)) {
      return command;
    }

    return withResolvedExecutable(command, resolvedExecutable);
  }

  public static String findOciExecutable(Project project) {
    String configuredExecutable = DevOpsToolKitSettings.getOciCliPath(project);
    if (isUsableAbsoluteExecutable(configuredExecutable)) {
      LOG.info("Using OCI CLI from configured settings path: " + configuredExecutable);
      project.putUserData(DEVOPS_TOOLKIT_REPOSITORY_OCI_CLI_KEY, configuredExecutable);
      return configuredExecutable;
    }

    String cachedExecutable = project.getUserData(DEVOPS_TOOLKIT_REPOSITORY_OCI_CLI_KEY);
    if (isUsableAbsoluteExecutable(cachedExecutable)) {
      LOG.debug("Using cached OCI CLI path: " + cachedExecutable);
      return cachedExecutable;
    }

    for (String candidate : pathCandidates()) {
      if (isUsableAbsoluteExecutable(candidate)) {
        LOG.info("Using OCI CLI from PATH: " + candidate);
        project.putUserData(DEVOPS_TOOLKIT_REPOSITORY_OCI_CLI_KEY, candidate);
        return candidate;
      }
    }

    String whereisCandidate = findUsingWhereCommand();
    if (isUsableAbsoluteExecutable(whereisCandidate)) {
      LOG.info("Using OCI CLI from shell lookup: " + whereisCandidate);
      project.putUserData(DEVOPS_TOOLKIT_REPOSITORY_OCI_CLI_KEY, whereisCandidate);
      return whereisCandidate;
    }

    LOG.warn("OCI CLI path not found in PATH, falling back to command name: " + OCI_COMMAND);
    project.putUserData(DEVOPS_TOOLKIT_REPOSITORY_OCI_CLI_KEY, OCI_COMMAND);
    return OCI_COMMAND;
  }

  private static boolean isUsableAbsoluteExecutable(String candidate) {
    if (candidate == null || candidate.isBlank()) {
      return false;
    }

    try {
      Path path = Paths.get(candidate.trim());
      return path.isAbsolute() && Files.isRegularFile(path) && Files.isExecutable(path);
    } catch (Exception ex) {
      LOG.warn("Invalid OCI CLI path candidate: " + candidate, ex);
      return false;
    }
  }

  private static List<String> pathCandidates() {
    Set<String> candidates = new LinkedHashSet<>();
    String pathEnv = System.getenv("PATH");
    if (pathEnv != null && !pathEnv.isBlank()) {
      for (String entry : pathEnv.split(File.pathSeparator)) {
        if (!entry.isBlank()) {
          for (String executableName : EXECUTABLE_NAMES) {
            candidates.add(Paths.get(entry, executableName).toString());
          }
        }
      }
    }
    return List.copyOf(candidates);
  }

  private static String findUsingWhereCommand() {
    Process process = null;
    try {
      process = new ProcessBuilder(whereCommand(), OCI_COMMAND).start();
      try (BufferedReader reader =
          new BufferedReader(new InputStreamReader(process.getInputStream()))) {
        String line;
        while ((line = reader.readLine()) != null) {
          for (String candidate : extractCandidates(line)) {
            if (isUsableAbsoluteExecutable(candidate)) {
              return candidate;
            }
          }
        }
      }
      process.waitFor();
    } catch (IOException | InterruptedException ex) {
      if (ex instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      LOG.debug("Unable to resolve OCI CLI via " + whereCommand(), ex);
    } finally {
      if (process != null) {
        process.destroy();
      }
    }
    return null;
  }

  private static List<String> extractCandidates(String line) {
    if (line == null || line.isBlank()) {
      return List.of();
    }

    String trimmed = line.trim();
    if (IS_WINDOWS) {
      return List.of(trimmed);
    }

    int colonIndex = trimmed.indexOf(':');
    if (colonIndex >= 0) {
      trimmed = trimmed.substring(colonIndex + 1).trim();
    }

    if (trimmed.isBlank()) {
      return List.of();
    }

    List<String> candidates = new ArrayList<>();
    for (String part : trimmed.split("\\s+")) {
      if (!part.isBlank()) {
        candidates.add(part);
      }
    }
    return List.copyOf(candidates);
  }

  private static List<String> withResolvedExecutable(
      List<String> command, String resolvedExecutable) {
    if (command.size() == 1) {
      return List.of(resolvedExecutable);
    }

    List<String> resolvedCommand = new java.util.ArrayList<>(command);
    resolvedCommand.set(0, resolvedExecutable);
    return List.copyOf(resolvedCommand);
  }

  private static List<String> executableNames() {
    if (!IS_WINDOWS) {
      return List.of(OCI_COMMAND);
    }

    Set<String> executableNames = new LinkedHashSet<>();
    executableNames.add("oci.exe");
    executableNames.add("oci.cmd");
    executableNames.add("oci.bat");
    executableNames.add(OCI_COMMAND);
    return List.copyOf(executableNames);
  }

  private static String whereCommand() {
    return IS_WINDOWS ? "where" : "whereis";
  }
}
