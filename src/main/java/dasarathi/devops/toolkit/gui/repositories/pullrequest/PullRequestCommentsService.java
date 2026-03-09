package dasarathi.devops.toolkit.gui.repositories.pullrequest;

import static dasarathi.devops.toolkit.core.DevOpsToolKitConstants.DEVOPS_COMPARTMENT_ID;
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
import com.oracle.bmc.devops.model.PullRequestCommentCollection;
import com.oracle.bmc.devops.model.PullRequestCommentSummary;
import com.oracle.bmc.devops.model.SortOrder;
import com.oracle.bmc.devops.requests.ListPullRequestCommentsRequest;
import com.oracle.bmc.devops.responses.ListPullRequestCommentsResponse;
import com.oracle.bmc.model.BmcException;
import com.oracle.bmc.retrier.RetryConfiguration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;

public class PullRequestCommentsService {
  private static final Logger LOG = Logger.getInstance(PullRequestCommentsService.class);

  public void fetchPullRequestComments(
      @NotNull Project project,
      @NotNull String pullRequestId,
      @NotNull Consumer<List<PullRequestCommentSummary>> onCompletedComments) {
    ProgressManager.getInstance()
        .run(
            new Task.Backgroundable(project, "Fetching pull request comments", true) {
              @Override
              public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                List<PullRequestCommentSummary> comments =
                    getPullRequestComments(project, pullRequestId);
                ApplicationManager.getApplication()
                    .invokeLater(() -> onCompletedComments.accept(comments));
              }
            });
  }

  private static List<PullRequestCommentSummary> getPullRequestComments(
      Project project, String pullRequestId) {
    LOG.info("Fetching pull request comments for pullRequestId=" + pullRequestId);
    try (DevopsClient devOpsClient = getDevOpsClient()) {
      List<PullRequestCommentSummary> comments = new ArrayList<>();
      String page = null;
      do {
        ListPullRequestCommentsRequest request =
            ListPullRequestCommentsRequest.builder()
                .pullRequestId(pullRequestId)
                .compartmentId(DEVOPS_COMPARTMENT_ID)
                .sortOrder(SortOrder.Desc)
                .sortBy(ListPullRequestCommentsRequest.SortBy.TimeCreated)
                .limit(100)
                .page(page)
                .opcRequestId(getRequestId("ListPullRequestComments"))
                .retryConfiguration(RetryConfiguration.NO_RETRY_CONFIGURATION)
                .build();
        ListPullRequestCommentsResponse response = devOpsClient.listPullRequestComments(request);
        PullRequestCommentCollection collection = response.getPullRequestCommentCollection();
        if (collection != null && collection.getItems() != null) {
          comments.addAll(collection.getItems());
        }
        page = response.getOpcNextPage();
      } while (page != null && !page.isBlank());
      return List.copyOf(comments);
    } catch (Exception exception) {
      if (exception instanceof BmcException bmcException) {
        LOG.warn(
            "Pull request comments unavailable for pullRequestId="
                + pullRequestId
                + ", status="
                + bmcException.getStatusCode()
                + ", serviceCode="
                + bmcException.getServiceCode());
      } else {
        LOG.warn("Unable to fetch pull request comments", exception);
      }
      showStatus(project, "Pull request comments unavailable");
    }
    return List.of();
  }
}
