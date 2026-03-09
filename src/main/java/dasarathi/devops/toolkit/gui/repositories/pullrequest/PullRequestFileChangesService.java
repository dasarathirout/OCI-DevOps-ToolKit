package dasarathi.devops.toolkit.gui.repositories.pullrequest;

import static dasarathi.devops.toolkit.DevOpsToolKitToolWindowFactory.DEVOPS_TOOLKIT_PULL_REQUEST_FILE_CHANGES_KEY;
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
import com.oracle.bmc.devops.model.DiffCollection;
import com.oracle.bmc.devops.model.DiffLineDetails;
import com.oracle.bmc.devops.model.DiffSection;
import com.oracle.bmc.devops.model.DiffSummary;
import com.oracle.bmc.devops.model.PullRequestFileChangeCollection;
import com.oracle.bmc.devops.model.PullRequestFileChangeSummary;
import com.oracle.bmc.devops.requests.ListCommitDiffsRequest;
import com.oracle.bmc.devops.requests.ListPullRequestFileChangesRequest;
import com.oracle.bmc.devops.responses.ListCommitDiffsResponse;
import com.oracle.bmc.devops.responses.ListPullRequestFileChangesResponse;
import com.oracle.bmc.model.BmcException;
import com.oracle.bmc.retrier.RetryConfiguration;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PullRequestFileChangesService {
  private static final Logger LOG = Logger.getInstance(PullRequestFileChangesService.class);
  private static final int GIT_DIFF_TIMEOUT_SECONDS = 15;

  public void fetchPullRequestFileChanges(
      @NotNull Project project,
      @NotNull String pullRequestId,
      @Nullable String repositoryId,
      @Nullable String targetRepositoryId,
      @Nullable String baseVersion,
      @Nullable String targetVersion,
      @NotNull
          Consumer<List<PullRequestFileChangeSummary>> onCompletedPullRequestFileChangeSummary) {
    ProgressManager.getInstance()
        .run(
            new Task.Backgroundable(project, "Fetching pull request file changes", true) {
              @Override
              public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                List<PullRequestFileChangeSummary> pullRequestFileChanges =
                    getPullRequestFileChanges(
                        project,
                        pullRequestId,
                        repositoryId,
                        targetRepositoryId,
                        baseVersion,
                        targetVersion);
                project.putUserData(
                    DEVOPS_TOOLKIT_PULL_REQUEST_FILE_CHANGES_KEY, pullRequestFileChanges);
                ApplicationManager.getApplication()
                    .invokeLater(
                        () ->
                            onCompletedPullRequestFileChangeSummary.accept(pullRequestFileChanges));
              }
            });
  }

  private static List<PullRequestFileChangeSummary> getPullRequestFileChanges(
      Project project,
      String pullRequestId,
      @Nullable String repositoryId,
      @Nullable String targetRepositoryId,
      @Nullable String baseVersion,
      @Nullable String targetVersion) {
    LOG.info("Fetching pull request file changes review " + pullRequestId);
    try (DevopsClient devOpsClient = getDevOpsClient()) {
      try {
        List<PullRequestFileChangeSummary> pullRequestFileChanges =
            listPullRequestFileChanges(devOpsClient, pullRequestId);
        if (!pullRequestFileChanges.isEmpty()) {
          return pullRequestFileChanges;
        }
      } catch (BmcException bmcException) {
        if (bmcException.getStatusCode() >= 500) {
          List<PullRequestFileChangeSummary> commitDiffFallback =
              listCommitDiffFileChanges(
                  devOpsClient, repositoryId, targetRepositoryId, baseVersion, targetVersion);
          if (!commitDiffFallback.isEmpty()) {
            LOG.info(
                "Loaded pull request file changes via ListCommitDiffs fallback for pullRequestId="
                    + pullRequestId);
            return commitDiffFallback;
          }
        }
        List<PullRequestFileChangeSummary> gitDiffFallback =
            listLocalGitDiffFileChanges(project, baseVersion, targetVersion);
        if (!gitDiffFallback.isEmpty()) {
          LOG.info(
              "Loaded pull request file changes via local Git fallback for pullRequestId="
                  + pullRequestId);
          return gitDiffFallback;
        }
        String message =
            formatFileChangesErrorMessage(bmcException, repositoryId, baseVersion, targetVersion);
        LOG.warn(
            "Pull request file changes unavailable for pullRequestId="
                + pullRequestId
                + ", status="
                + bmcException.getStatusCode()
                + ", serviceCode="
                + bmcException.getServiceCode()
                + ", opcRequestId="
                + bmcException.getOpcRequestId());
        showStatus(project, message);
        if (bmcException.getStatusCode() >= 500) {
          notifyInfo(project, message);
        }
      } catch (Exception exception) {
        LOG.warn("Failed to fetch pull request file changes info", exception);
        showStatus(project, "Pull request file changes unavailable");
      }
    } catch (Exception exception) {
      LOG.warn("Failed to initialize Devops client for pull request file changes", exception);
      showStatus(project, "Pull request file changes unavailable");
    }
    return List.of();
  }

  private static List<PullRequestFileChangeSummary> listLocalGitDiffFileChanges(
      @NotNull Project project, @Nullable String baseVersion, @Nullable String targetVersion) {
    String basePath = project.getBasePath();
    if (isBlank(basePath) || isBlank(baseVersion) || isBlank(targetVersion)) {
      return List.of();
    }

    Path workTree = Path.of(basePath).toAbsolutePath().normalize();
    if (!Files.isDirectory(workTree)) {
      return List.of();
    }

    for (String range : diffRangeCandidates(baseVersion, targetVersion)) {
      GitCommandResult nameStatusResult =
          runGitCommand(workTree, List.of("git", "diff", "--name-status", "-M", range));
      if (!nameStatusResult.success()) {
        continue;
      }

      GitCommandResult numstatResult =
          runGitCommand(workTree, List.of("git", "diff", "--numstat", "-M", range));
      if (!numstatResult.success()) {
        continue;
      }

      List<PullRequestFileChangeSummary> fileChanges =
          parseGitDiffOutput(nameStatusResult.output(), numstatResult.output());
      if (!fileChanges.isEmpty()) {
        return fileChanges;
      }
    }
    return List.of();
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

  static @NotNull List<PullRequestFileChangeSummary> parseGitDiffOutput(
      @NotNull List<String> nameStatusLines, @NotNull List<String> numstatLines) {
    Map<String, GitNumstat> numstatsByPath = parseNumstatLines(numstatLines);
    List<PullRequestFileChangeSummary> fileChanges = new ArrayList<>();
    for (String line : nameStatusLines) {
      GitNameStatus nameStatus = parseNameStatusLine(line);
      if (nameStatus == null) {
        continue;
      }
      GitNumstat numstat = numstatsByPath.getOrDefault(nameStatus.keyPath(), GitNumstat.EMPTY);
      fileChanges.add(
          PullRequestFileChangeSummary.builder()
              .changeType(nameStatus.changeType())
              .oldPath(nameStatus.oldPath())
              .newPath(nameStatus.newPath())
              .addedLinesCount(numstat.addedLines())
              .deletedLinesCount(numstat.deletedLines())
              .hasConflicts(Boolean.FALSE)
              .build());
    }
    return fileChanges.isEmpty() ? List.of() : List.copyOf(fileChanges);
  }

  private static @Nullable GitNameStatus parseNameStatusLine(@Nullable String line) {
    if (line == null || line.isBlank()) {
      return null;
    }
    String[] parts = line.split("\\t");
    if (parts.length < 2) {
      return null;
    }

    String status = parts[0];
    if (status.startsWith("R") && parts.length >= 3) {
      return new GitNameStatus("RENAMED", parts[1], parts[2], parts[2]);
    }
    if (status.startsWith("C") && parts.length >= 3) {
      return new GitNameStatus("COPIED", parts[1], parts[2], parts[2]);
    }

    String path = parts[1];
    if (status.startsWith("A")) {
      return new GitNameStatus("ADDED", null, path, path);
    }
    if (status.startsWith("D")) {
      return new GitNameStatus("DELETED", path, null, path);
    }
    return new GitNameStatus("MODIFIED", path, path, path);
  }

  private static @NotNull Map<String, GitNumstat> parseNumstatLines(
      @NotNull List<String> numstatLines) {
    Map<String, GitNumstat> numstatsByPath = new HashMap<>();
    for (String line : numstatLines) {
      if (line == null || line.isBlank()) {
        continue;
      }
      String[] parts = line.split("\\t");
      if (parts.length < 3) {
        continue;
      }
      int addedLines = parseLineCount(parts[0]);
      int deletedLines = parseLineCount(parts[1]);
      String keyPath = parts[parts.length - 1];
      numstatsByPath.put(keyPath, new GitNumstat(addedLines, deletedLines));
    }
    return numstatsByPath;
  }

  private static int parseLineCount(@Nullable String value) {
    if (value == null || value.isBlank() || "-".equals(value)) {
      return 0;
    }
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException ignored) {
      return 0;
    }
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
        LOG.warn("Local Git diff fallback timed out: " + String.join(" ", command));
        return new GitCommandResult(false, List.of());
      }
      List<String> output = readProcessOutput(process);
      boolean success = process.exitValue() == 0;
      if (!success) {
        LOG.debug(
            "Local Git diff fallback command failed: "
                + String.join(" ", command)
                + " output="
                + String.join(System.lineSeparator(), output));
      }
      return new GitCommandResult(success, output);
    } catch (InterruptedException interruptedException) {
      Thread.currentThread().interrupt();
      LOG.warn("Local Git diff fallback interrupted", interruptedException);
      return new GitCommandResult(false, List.of());
    } catch (Exception exception) {
      LOG.debug("Local Git diff fallback failed", exception);
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

  private static List<PullRequestFileChangeSummary> listPullRequestFileChanges(
      @NotNull DevopsClient devOpsClient, @NotNull String pullRequestId) {
    List<PullRequestFileChangeSummary> allFileChanges = new ArrayList<>();
    String nextPageToken = null;
    do {
      ListPullRequestFileChangesRequest listPullRequestFileChangesRequest =
          ListPullRequestFileChangesRequest.builder()
              .pullRequestId(pullRequestId)
              .limit(100)
              .page(nextPageToken)
              .opcRequestId(getRequestId("ListPullRequestFileChanges"))
              .retryConfiguration(RetryConfiguration.NO_RETRY_CONFIGURATION)
              .build();
      ListPullRequestFileChangesResponse listPullRequestFileChangesResponse =
          devOpsClient.listPullRequestFileChanges(listPullRequestFileChangesRequest);
      PullRequestFileChangeCollection fileChangeCollection =
          listPullRequestFileChangesResponse.getPullRequestFileChangeCollection();
      if (fileChangeCollection != null && fileChangeCollection.getItems() != null) {
        allFileChanges.addAll(fileChangeCollection.getItems());
      }
      nextPageToken = listPullRequestFileChangesResponse.getOpcNextPage();
    } while (nextPageToken != null && !nextPageToken.isBlank());
    return allFileChanges.isEmpty() ? List.of() : List.copyOf(allFileChanges);
  }

  private static List<PullRequestFileChangeSummary> listCommitDiffFileChanges(
      @NotNull DevopsClient devOpsClient,
      @Nullable String repositoryId,
      @Nullable String targetRepositoryId,
      @Nullable String baseVersion,
      @Nullable String targetVersion) {
    if (isBlank(repositoryId) || isBlank(baseVersion) || isBlank(targetVersion)) {
      return List.of();
    }
    try {
      List<PullRequestFileChangeSummary> mappedFileChanges = new ArrayList<>();
      String nextPageToken = null;
      do {
        ListCommitDiffsRequest.Builder requestBuilder =
            ListCommitDiffsRequest.builder()
                .repositoryId(repositoryId)
                .baseVersion(baseVersion)
                .targetVersion(targetVersion)
                .isComparisonFromMergeBase(Boolean.TRUE)
                .limit(100)
                .page(nextPageToken)
                .opcRequestId(getRequestId("ListCommitDiffs"))
                .retryConfiguration(RetryConfiguration.NO_RETRY_CONFIGURATION);
        if (!isBlank(targetRepositoryId) && !targetRepositoryId.equals(repositoryId)) {
          requestBuilder.targetRepositoryId(targetRepositoryId);
        }

        ListCommitDiffsResponse listCommitDiffsResponse =
            devOpsClient.listCommitDiffs(requestBuilder.build());
        DiffCollection diffCollection = listCommitDiffsResponse.getDiffCollection();
        if (diffCollection != null && diffCollection.getItems() != null) {
          for (DiffSummary diffSummary : diffCollection.getItems()) {
            mappedFileChanges.add(mapDiffSummary(diffSummary));
          }
        }
        nextPageToken = listCommitDiffsResponse.getOpcNextPage();
      } while (nextPageToken != null && !nextPageToken.isBlank());

      return mappedFileChanges.isEmpty() ? List.of() : List.copyOf(mappedFileChanges);
    } catch (BmcException bmcException) {
      LOG.warn(
          "Commit diff fallback unavailable, status="
              + bmcException.getStatusCode()
              + ", serviceCode="
              + bmcException.getServiceCode()
              + ", opcRequestId="
              + bmcException.getOpcRequestId());
    } catch (Exception exception) {
      LOG.warn("Commit diff fallback failed", exception);
    }
    return List.of();
  }

  private static PullRequestFileChangeSummary mapDiffSummary(@NotNull DiffSummary diffSummary) {
    int[] lineCounts = computeLineCounts(diffSummary);
    return PullRequestFileChangeSummary.builder()
        .changeType(resolveChangeType(diffSummary))
        .oldPath(diffSummary.getOldPath())
        .newPath(diffSummary.getNewPath())
        .oldId(diffSummary.getOldId())
        .newId(diffSummary.getNewId())
        .addedLinesCount(lineCounts[0])
        .deletedLinesCount(lineCounts[1])
        .hasConflicts(diffSummary.getAreConflictsInFile())
        .build();
  }

  private static int[] computeLineCounts(@NotNull DiffSummary diffSummary) {
    int addedLines = 0;
    int deletedLines = 0;
    List<DiffChunk> diffChunks = diffSummary.getChanges();
    if (diffChunks == null) {
      return new int[] {0, 0};
    }
    for (DiffChunk diffChunk : diffChunks) {
      if (diffChunk == null || diffChunk.getDiffSections() == null) {
        continue;
      }
      for (DiffSection diffSection : diffChunk.getDiffSections()) {
        List<DiffLineDetails> diffLines = diffSection == null ? null : diffSection.getLines();
        if (diffLines == null) {
          continue;
        }
        for (DiffLineDetails diffLine : diffLines) {
          if (diffLine == null) {
            continue;
          }
          Integer baseLine = diffLine.getBaseLine();
          Integer targetLine = diffLine.getTargetLine();
          if (baseLine == null && targetLine != null) {
            addedLines++;
          } else if (baseLine != null && targetLine == null) {
            deletedLines++;
          }
        }
      }
    }
    return new int[] {addedLines, deletedLines};
  }

  private static @NotNull String resolveChangeType(@NotNull DiffSummary diffSummary) {
    String oldPath = diffSummary.getOldPath();
    String newPath = diffSummary.getNewPath();
    if (isBlank(oldPath) && !isBlank(newPath)) {
      return "ADDED";
    }
    if (!isBlank(oldPath) && isBlank(newPath)) {
      return "DELETED";
    }
    if (!isBlank(oldPath) && !isBlank(newPath) && !oldPath.equals(newPath)) {
      return "RENAMED";
    }
    return "MODIFIED";
  }

  private static @NotNull String formatFileChangesErrorMessage(
      @NotNull BmcException bmcException,
      @Nullable String repositoryId,
      @Nullable String baseVersion,
      @Nullable String targetVersion) {
    if (bmcException.getStatusCode() >= 500) {
      if (!isBlank(repositoryId) && !isBlank(baseVersion) && !isBlank(targetVersion)) {
        return "OCI DevOps failed to return pull request file changes. Commit comparison fallback also returned no changed files."
            + formatOpcRequestId(bmcException);
      }
      return "OCI DevOps failed to return pull request file changes. Falling back to comment-based diff context."
          + formatOpcRequestId(bmcException);
    }
    return "Pull request file changes unavailable" + formatOpcRequestId(bmcException);
  }

  private static boolean isBlank(@Nullable String value) {
    return value == null || value.isBlank();
  }

  private static @NotNull String formatOpcRequestId(@NotNull BmcException bmcException) {
    String opcRequestId = bmcException.getOpcRequestId();
    return opcRequestId == null || opcRequestId.isBlank() ? "" : " Request ID: " + opcRequestId;
  }

  private record GitCommandResult(boolean success, List<String> output) {}

  private record GitNameStatus(String changeType, String oldPath, String newPath, String keyPath) {}

  private record GitNumstat(int addedLines, int deletedLines) {
    private static final GitNumstat EMPTY = new GitNumstat(0, 0);
  }
}
