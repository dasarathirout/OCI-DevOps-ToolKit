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
import com.oracle.bmc.devops.model.RepositoryCommitCollection;
import com.oracle.bmc.devops.model.RepositoryCommitSummary;
import com.oracle.bmc.devops.requests.ListPullRequestCommitsRequest;
import com.oracle.bmc.devops.responses.ListPullRequestCommitsResponse;
import com.oracle.bmc.model.BmcException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;

public class PullRequestCommitsService {
  private static final Logger LOG = Logger.getInstance(PullRequestCommitsService.class);

  public void fetchPullRequestCommits(
      @NotNull Project project,
      @NotNull String pullRequestId,
      @NotNull Consumer<List<RepositoryCommitSummary>> onCompletedCommits) {
    ProgressManager.getInstance()
        .run(
            new Task.Backgroundable(project, "Fetching pull request commits", true) {
              @Override
              public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                List<RepositoryCommitSummary> commits =
                    getPullRequestCommits(project, pullRequestId);
                ApplicationManager.getApplication()
                    .invokeLater(() -> onCompletedCommits.accept(commits));
              }
            });
  }

  private static List<RepositoryCommitSummary> getPullRequestCommits(
      Project project, String pullRequestId) {
    LOG.info("Fetching pull request commits for pullRequestId=" + pullRequestId);
    try (DevopsClient devOpsClient = getDevOpsClient()) {
      List<RepositoryCommitSummary> commits = new ArrayList<>();
      String page = null;
      do {
        ListPullRequestCommitsRequest request =
            ListPullRequestCommitsRequest.builder()
                .pullRequestId(pullRequestId)
                .limit(100)
                .page(page)
                .opcRequestId(getRequestId("ListPullRequestCommits"))
                .build();
        ListPullRequestCommitsResponse response = devOpsClient.listPullRequestCommits(request);
        RepositoryCommitCollection collection = response.getRepositoryCommitCollection();
        if (collection != null && collection.getItems() != null) {
          commits.addAll(collection.getItems());
        }
        page = response.getOpcNextPage();
      } while (page != null && !page.isBlank());
      return List.copyOf(commits);
    } catch (Exception exception) {
      if (exception instanceof BmcException bmcException) {
        LOG.warn(
            "Pull request commits unavailable for pullRequestId="
                + pullRequestId
                + ", status="
                + bmcException.getStatusCode()
                + ", serviceCode="
                + bmcException.getServiceCode());
      } else {
        LOG.warn("Unable to fetch pull request commits", exception);
      }
      showStatus(project, "Pull request commits unavailable");
    }
    return List.of();
  }
}
