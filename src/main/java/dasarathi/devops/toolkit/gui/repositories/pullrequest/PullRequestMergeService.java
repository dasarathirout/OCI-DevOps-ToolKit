package dasarathi.devops.toolkit.gui.repositories.pullrequest;

import static dasarathi.devops.toolkit.core.DevOpsToolKitUtil.getDevOpsClient;
import static dasarathi.devops.toolkit.core.DevOpsToolKitUtil.getRequestId;
import static dasarathi.devops.toolkit.event.CustomEventNotification.showStatus;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.oracle.bmc.devops.DevopsClient;
import com.oracle.bmc.devops.model.ExecuteMergePullRequestDetails;
import com.oracle.bmc.devops.model.MergeStrategy;
import com.oracle.bmc.devops.model.PullRequest;
import com.oracle.bmc.devops.requests.MergePullRequestRequest;
import com.oracle.bmc.devops.responses.MergePullRequestResponse;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;

public class PullRequestMergeService {
  private static final Logger LOG = Logger.getInstance(PullRequestMergeService.class);

  public void mergePullRequest(
      @NotNull Project project,
      @NotNull String pullRequestId,
      @NotNull Consumer<PullRequest> onCompletedPullRequest) {
    ProgressManager.getInstance()
        .run(
            new Task.Backgroundable(project, "Merging pull request", true) {
              @Override
              public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                PullRequest pullRequest = executeMerge(project, pullRequestId);
                ApplicationManager.getApplication()
                    .invokeLater(() -> onCompletedPullRequest.accept(pullRequest));
              }
            });
  }

  private static PullRequest executeMerge(Project project, String pullRequestId) {
    LOG.info("Merging pull request pullRequestId=" + pullRequestId);
    try (DevopsClient devOpsClient = getDevOpsClient()) {
      ExecuteMergePullRequestDetails details =
          ExecuteMergePullRequestDetails.builder()
              .commitMessage("Merged from OCI DevOps Toolkit")
              .mergeStrategy(MergeStrategy.MergeCommit)
              .postMergeAction(
                  com.oracle.bmc.devops.model.ExecuteMergePullRequestDetails.PostMergeAction
                      .KeepSourceBranch)
              .build();
      MergePullRequestRequest request =
          MergePullRequestRequest.builder()
              .pullRequestId(pullRequestId)
              .mergePullRequestDetails(details)
              .opcRequestId(getRequestId("MergePullRequest"))
              .opcRetryToken(getRequestId("MergePullRequestRetry"))
              .build();
      MergePullRequestResponse response = devOpsClient.mergePullRequest(request);
      showStatus(project, "Pull request merge submitted");
      return response.getPullRequest();
    } catch (Exception exception) {
      LOG.warn("Unable to merge pull request", exception);
      showStatus(project, "Pull request merge unavailable");
    }
    return PullRequest.builder().build();
  }
}
