package dasarathi.devops.toolkit.gui.repositories.pullrequest;

import static dasarathi.devops.toolkit.DevOpsToolKitToolWindowFactory.DEVOPS_TOOLKIT_PULL_REQUEST_SUMMARY_KEY;
import static dasarathi.devops.toolkit.core.DevOpsToolKitConstants.DEVOPS_COMPARTMENT_ID;
import static dasarathi.devops.toolkit.core.DevOpsToolKitUtil.getDevOpsClient;
import static dasarathi.devops.toolkit.core.DevOpsToolKitUtil.getRequestId;
import static dasarathi.devops.toolkit.event.CustomEventNotification.notifyError;
import static dasarathi.devops.toolkit.event.CustomEventNotification.showStatus;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.oracle.bmc.devops.DevopsClient;
import com.oracle.bmc.devops.model.PullRequestCollection;
import com.oracle.bmc.devops.model.PullRequestSummary;
import com.oracle.bmc.devops.requests.ListPullRequestsRequest;
import com.oracle.bmc.devops.responses.ListPullRequestsResponse;
import dasarathi.devops.toolkit.core.DevOpsToolKitSettings;
import java.util.List;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;

public final class PullRequestSummaryService {
  private static final Logger LOG = Logger.getInstance(PullRequestSummaryService.class);

  public PullRequestSummaryService() {
    /* NOOP */
  }

  public void fetchPullRequestSummaryList(
      @NotNull Project project,
      @NotNull String repositoryId,
      @NotNull Consumer<List<PullRequestSummary>> onCompletedPullRequestSummaryList) {
    ProgressManager.getInstance()
        .run(
            new Task.Backgroundable(project, "Fetching SCM PullRequest", true) {
              @Override
              public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                List<PullRequestSummary> pullRequestSummaryList =
                    getPullRequestSummaryList(project, repositoryId);
                project.putUserData(
                    DEVOPS_TOOLKIT_PULL_REQUEST_SUMMARY_KEY, pullRequestSummaryList);
                DevOpsToolKitSettings.setPullRequestSummaries(project, pullRequestSummaryList);
                ApplicationManager.getApplication()
                    .invokeLater(
                        () -> onCompletedPullRequestSummaryList.accept(pullRequestSummaryList));
              }
            });
  }

  private static List<PullRequestSummary> getPullRequestSummaryList(
      Project project, String repositoryId) {
    LOG.info("Fetching pull request summary list for repository " + repositoryId);
    try (DevopsClient devOpsClient = getDevOpsClient()) {
      ListPullRequestsRequest listPullRequestsRequest =
          ListPullRequestsRequest.builder()
              .compartmentId(DEVOPS_COMPARTMENT_ID)
              .repositoryId(repositoryId)
              .opcRequestId(getRequestId("ListPullRequests"))
              .build();
      ListPullRequestsResponse listPullRequestsResponse =
          devOpsClient.listPullRequests(listPullRequestsRequest);
      return getPullRequestSummaries(listPullRequestsResponse);
    } catch (Exception exception) {
      LOG.error("Failed to fetch pull request summary", exception);
      showStatus(project, "Error: get Pull Request Summary List");
      notifyError(project, "Error: get Pull Request Summary List " + exception.getMessage());
    }
    return List.of();
  }

  private static List<PullRequestSummary> getPullRequestSummaries(
      ListPullRequestsResponse listPullRequestsResponse) {
    if (listPullRequestsResponse == null) {
      return List.of();
    }
    PullRequestCollection pullRequestCollection =
        listPullRequestsResponse.getPullRequestCollection();
    if (pullRequestCollection == null || pullRequestCollection.getItems() == null) {
      return List.of();
    }
    return List.copyOf(pullRequestCollection.getItems());
  }
}
