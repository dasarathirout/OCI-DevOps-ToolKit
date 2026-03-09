package dasarathi.devops.toolkit.gui.repositories.pullrequest;

import static dasarathi.devops.toolkit.core.DevOpsToolKitUtil.getDevOpsClient;
import static dasarathi.devops.toolkit.core.DevOpsToolKitUtil.getRequestId;
import static dasarathi.devops.toolkit.event.CustomEventNotification.notifyInfo;
import static dasarathi.devops.toolkit.event.CustomEventNotification.showStatus;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.oracle.bmc.devops.DevopsClient;
import com.oracle.bmc.devops.model.DiffChunk;
import com.oracle.bmc.devops.model.DiffLineDetails;
import com.oracle.bmc.devops.model.DiffSection;
import com.oracle.bmc.devops.model.FileDiffResponse;
import com.oracle.bmc.devops.requests.GetRepoFileDiffRequest;
import com.oracle.bmc.devops.responses.GetRepoFileDiffResponse;
import com.oracle.bmc.model.BmcException;
import com.oracle.bmc.retrier.RetryConfiguration;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RepoFileDiffService {
  private static final Logger LOG = Logger.getInstance(RepoFileDiffService.class);
  private static final int GIT_DIFF_TIMEOUT_SECONDS = 15;

  public void fetchRepoFileDiff(
      @NotNull Project project,
      @NotNull String repositoryId,
      @NotNull String baseVersion,
      @NotNull String targetVersion,
      @NotNull String filePath,
      @NotNull Consumer<FileDiffResponse> onCompletedDiff) {
    ProgressManager.getInstance()
        .run(
            new Task.Backgroundable(project, "Fetching repo file diff", true) {
              @Override
              public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                FileDiffResponse diff =
                    getRepoFileDiff(project, repositoryId, baseVersion, targetVersion, filePath);
                ApplicationManager.getApplication().invokeLater(() -> onCompletedDiff.accept(diff));
              }
            });
  }

  private static FileDiffResponse getRepoFileDiff(
      Project project,
      String repositoryId,
      String baseVersion,
      String targetVersion,
      String filePath) {
    LOG.info("Fetching repo file diff for repositoryId=" + repositoryId + ", filePath=" + filePath);
    try (DevopsClient devOpsClient = getDevOpsClient()) {
      GetRepoFileDiffRequest request =
          GetRepoFileDiffRequest.builder()
              .repositoryId(repositoryId)
              .baseVersion(baseVersion)
              .targetVersion(targetVersion)
              .filePath(filePath)
              .isComparisonFromMergeBase(Boolean.TRUE)
              .opcRequestId(getRequestId("GetRepoFileDiff"))
              .retryConfiguration(RetryConfiguration.NO_RETRY_CONFIGURATION)
              .build();
      GetRepoFileDiffResponse response = devOpsClient.getRepoFileDiff(request);
      return response.getFileDiffResponse();
    } catch (BmcException bmcException) {
      String message = formatRepoFileDiffErrorMessage(bmcException);
      LOG.warn(
          "Unable to fetch repo file diff for repositoryId="
              + repositoryId
              + ", filePath="
              + filePath
              + ", status="
              + bmcException.getStatusCode()
              + ", serviceCode="
              + bmcException.getServiceCode()
              + ", opcRequestId="
              + bmcException.getOpcRequestId());
      FileDiffResponse localGitDiff =
          getLocalGitFileDiff(project, baseVersion, targetVersion, filePath);
      if (hasPatch(localGitDiff)) {
        LOG.info(
            "Loaded repo file diff via local Git fallback for repositoryId="
                + repositoryId
                + ", filePath="
                + filePath);
        return localGitDiff;
      }
      showStatus(project, message);
      if (bmcException.getStatusCode() >= 500) {
        notifyInfo(project, message);
      }
    } catch (Exception exception) {
      LOG.warn("Unable to fetch repo file diff", exception);
      showStatus(project, "Repo file diff unavailable");
    }
    return FileDiffResponse.builder().build();
  }

  private static FileDiffResponse getLocalGitFileDiff(
      @NotNull Project project,
      @Nullable String baseVersion,
      @Nullable String targetVersion,
      @Nullable String filePath) {
    String basePath = project.getBasePath();
    if (isBlank(basePath) || isBlank(baseVersion) || isBlank(targetVersion) || isBlank(filePath)) {
      return FileDiffResponse.builder().build();
    }

    Path workTree = Path.of(basePath).toAbsolutePath().normalize();
    if (!Files.isDirectory(workTree)) {
      return FileDiffResponse.builder().build();
    }

    for (String range : diffRangeCandidates(baseVersion, targetVersion)) {
      GitCommandResult result =
          runGitCommand(
              workTree,
              List.of("git", "diff", "--no-ext-diff", "--unified=80", range, "--", filePath));
      if (!result.success() || result.output().isEmpty()) {
        continue;
      }
      FileDiffResponse diffResponse = parseUnifiedDiff(result.output(), filePath);
      if (hasPatch(diffResponse)) {
        return diffResponse;
      }
    }
    return FileDiffResponse.builder().build();
  }

  private static @NotNull List<String> diffRangeCandidates(
      @NotNull String baseVersion, @NotNull String targetVersion) {
    String base = baseVersion.trim();
    String target = targetVersion.trim();
    List<String> ranges = new ArrayList<>();
    ranges.add(base + "..." + target);
    ranges.add(remoteRef(base) + "..." + remoteRef(target));
    ranges.add(base + ".." + target);
    ranges.add(remoteRef(base) + ".." + remoteRef(target));
    return ranges.stream().distinct().toList();
  }

  private static @NotNull String remoteRef(@NotNull String ref) {
    return ref.startsWith("origin/") ? ref : "origin/" + ref;
  }

  static @NotNull FileDiffResponse parseUnifiedDiff(
      @NotNull List<String> diffLines, @NotNull String fallbackPath) {
    String oldPath = fallbackPath;
    String newPath = fallbackPath;
    List<DiffChunk> chunks = new ArrayList<>();
    DiffChunkBuilder currentChunk = null;

    for (String line : diffLines) {
      if (line.startsWith("--- ")) {
        oldPath = normalizeDiffPath(line.substring(4).trim(), fallbackPath);
        continue;
      }
      if (line.startsWith("+++ ")) {
        newPath = normalizeDiffPath(line.substring(4).trim(), fallbackPath);
        continue;
      }
      if (line.startsWith("@@ ")) {
        if (currentChunk != null) {
          chunks.add(currentChunk.build());
        }
        currentChunk = parseHunkHeader(line);
        continue;
      }
      if (currentChunk == null || line.isEmpty() || line.startsWith("\\ No newline")) {
        continue;
      }
      currentChunk.addLine(line);
    }

    if (currentChunk != null) {
      chunks.add(currentChunk.build());
    }

    return FileDiffResponse.builder()
        .oldPath(oldPath)
        .newPath(newPath)
        .isBinary(Boolean.FALSE)
        .isLarge(Boolean.FALSE)
        .areConflictsInFile(Boolean.FALSE)
        .changes(chunks)
        .build();
  }

  private static @NotNull String normalizeDiffPath(
      @NotNull String rawPath, @NotNull String fallbackPath) {
    if (rawPath.isBlank() || "/dev/null".equals(rawPath)) {
      return fallbackPath;
    }
    String path = rawPath;
    int tabIndex = path.indexOf('\t');
    if (tabIndex >= 0) {
      path = path.substring(0, tabIndex);
    }
    if (path.startsWith("a/") || path.startsWith("b/")) {
      return path.substring(2);
    }
    return path;
  }

  private static @NotNull DiffChunkBuilder parseHunkHeader(@NotNull String line) {
    String[] parts = line.split(" ");
    int[] baseRange = parseRange(parts.length > 1 ? parts[1] : "-0,0");
    int[] targetRange = parseRange(parts.length > 2 ? parts[2] : "+0,0");
    return new DiffChunkBuilder(baseRange[0], baseRange[1], targetRange[0], targetRange[1]);
  }

  private static int[] parseRange(@NotNull String rawRange) {
    String range = rawRange.replace("-", "").replace("+", "");
    String[] parts = range.split(",", 2);
    int start = parsePositiveInt(parts.length > 0 ? parts[0] : "0");
    int span = parts.length > 1 ? parsePositiveInt(parts[1]) : 1;
    return new int[] {start, span};
  }

  private static int parsePositiveInt(@Nullable String value) {
    if (value == null || value.isBlank()) {
      return 0;
    }
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException ignored) {
      return 0;
    }
  }

  private static boolean hasPatch(@NotNull FileDiffResponse diffResponse) {
    return diffResponse.getChanges() != null && !diffResponse.getChanges().isEmpty();
  }

  private static @NotNull GitCommandResult runGitCommand(
      @NotNull Path workTree, @NotNull List<String> command) {
    Process process = null;
    try {
      GeneralCommandLine commandLine = new GeneralCommandLine(command);
      commandLine.withWorkDirectory(workTree.toFile());
      commandLine.withRedirectErrorStream(true);
      process = commandLine.createProcess();
      boolean completed = process.waitFor(GIT_DIFF_TIMEOUT_SECONDS, TimeUnit.SECONDS);
      if (!completed) {
        process.destroyForcibly();
        LOG.warn("Local Git file diff fallback timed out: " + String.join(" ", command));
        return new GitCommandResult(false, List.of());
      }
      List<String> output = readProcessOutput(process);
      boolean success = process.exitValue() == 0;
      if (!success) {
        LOG.debug(
            "Local Git file diff fallback command failed: "
                + String.join(" ", command)
                + " output="
                + String.join(System.lineSeparator(), output));
      }
      return new GitCommandResult(success, output);
    } catch (InterruptedException interruptedException) {
      Thread.currentThread().interrupt();
      LOG.warn("Local Git file diff fallback interrupted", interruptedException);
      return new GitCommandResult(false, List.of());
    } catch (Exception exception) {
      LOG.debug("Local Git file diff fallback failed", exception);
      return new GitCommandResult(false, List.of());
    } finally {
      if (process != null) {
        process.destroy();
      }
    }
  }

  private static @NotNull List<String> readProcessOutput(@NotNull Process process)
      throws java.io.IOException {
    List<String> output = new ArrayList<>();
    try (BufferedReader reader =
        new BufferedReader(
            new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        output.add(line);
      }
    }
    return output;
  }

  private static @NotNull String formatRepoFileDiffErrorMessage(
      @NotNull BmcException bmcException) {
    if (bmcException.getStatusCode() >= 500) {
      return "OCI DevOps failed to return the file patch. Showing summary details instead."
          + formatOpcRequestId(bmcException);
    }
    return "Repo file diff unavailable" + formatOpcRequestId(bmcException);
  }

  private static @NotNull String formatOpcRequestId(@NotNull BmcException bmcException) {
    String opcRequestId = bmcException.getOpcRequestId();
    return opcRequestId == null || opcRequestId.isBlank() ? "" : " Request ID: " + opcRequestId;
  }

  private static boolean isBlank(@Nullable String value) {
    return value == null || value.isBlank();
  }

  private record GitCommandResult(boolean success, List<String> output) {}

  private static final class DiffChunkBuilder {
    private final int baseLine;
    private final int baseSpan;
    private final int targetLine;
    private final int targetSpan;
    private final List<DiffLineDetails> lines = new ArrayList<>();
    private int nextBaseLine;
    private int nextTargetLine;

    private DiffChunkBuilder(int baseLine, int baseSpan, int targetLine, int targetSpan) {
      this.baseLine = baseLine;
      this.baseSpan = baseSpan;
      this.targetLine = targetLine;
      this.targetSpan = targetSpan;
      this.nextBaseLine = baseLine;
      this.nextTargetLine = targetLine;
    }

    private void addLine(@NotNull String rawLine) {
      char prefix = rawLine.charAt(0);
      String content = rawLine.length() > 1 ? rawLine.substring(1) : "";
      if (prefix == '+') {
        lines.add(
            DiffLineDetails.builder()
                .baseLine(null)
                .targetLine(nextTargetLine++)
                .lineContent(content)
                .build());
        return;
      }
      if (prefix == '-') {
        lines.add(
            DiffLineDetails.builder()
                .baseLine(nextBaseLine++)
                .targetLine(null)
                .lineContent(content)
                .build());
        return;
      }
      lines.add(
          DiffLineDetails.builder()
              .baseLine(nextBaseLine++)
              .targetLine(nextTargetLine++)
              .lineContent(content)
              .build());
    }

    private @NotNull DiffChunk build() {
      return DiffChunk.builder()
          .baseLine(baseLine)
          .baseSpan(baseSpan)
          .targetLine(targetLine)
          .targetSpan(targetSpan)
          .diffSections(
              List.of(DiffSection.builder().type("default").lines(List.copyOf(lines)).build()))
          .build();
    }
  }
}
