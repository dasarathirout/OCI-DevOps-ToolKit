package dasarathi.devops.toolkit.gui.repositories.pullrequest;

import static dasarathi.devops.toolkit.DevOpsToolKitToolWindowFactory.DEVOPS_TOOLKIT_PULL_REQUEST_KEY;
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
import com.oracle.bmc.devops.model.PullRequest;
import com.oracle.bmc.devops.model.ReviewPullRequestDetails;
import com.oracle.bmc.devops.requests.GetPullRequestRequest;
import com.oracle.bmc.devops.requests.ReviewPullRequestRequest;
import com.oracle.bmc.devops.responses.GetPullRequestResponse;
import com.oracle.bmc.devops.responses.ReviewPullRequestResponse;
import com.oracle.bmc.model.BmcException;
import com.oracle.bmc.retrier.RetryConfiguration;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;

public class PullRequestReviewService {
  private static final Logger LOG = Logger.getInstance(PullRequestReviewService.class);

  public enum ReviewAction {
    APPROVE(ReviewPullRequestDetails.Action.Approve, "approved"),
    UNAPPROVE(ReviewPullRequestDetails.Action.Unapprove, "unapproved");

    private final ReviewPullRequestDetails.Action action;
    private final String successText;

    ReviewAction(ReviewPullRequestDetails.Action action, String successText) {
      this.action = action;
      this.successText = successText;
    }
  }

  public void fetchPullRequestReviewList(
      @NotNull Project project,
      @NotNull String pullRequestId,
      @NotNull Consumer<PullRequest> onCompletedPullRequest) {
    ProgressManager.getInstance()
        .run(
            new Task.Backgroundable(project, "Fetching pull request review", true) {
              @Override
              public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                PullRequest pullRequest = getPullRequestReviewList(project, pullRequestId);
                project.putUserData(DEVOPS_TOOLKIT_PULL_REQUEST_KEY, pullRequest);
                ApplicationManager.getApplication()
                    .invokeLater(() -> onCompletedPullRequest.accept(pullRequest));
              }
            });
  }

  public void submitReviewAction(
      @NotNull Project project,
      @NotNull String pullRequestId,
      @NotNull ReviewAction reviewAction,
      Consumer<PullRequest> onCompletedPullRequest) {
    ProgressManager.getInstance()
        .run(
            new Task.Backgroundable(project, "Submitting pull request review", true) {
              @Override
              public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                PullRequest pullRequest =
                    reviewPullRequest(project, pullRequestId, reviewAction, indicator);
                project.putUserData(DEVOPS_TOOLKIT_PULL_REQUEST_KEY, pullRequest);
                ApplicationManager.getApplication()
                    .invokeLater(() -> onCompletedPullRequest.accept(pullRequest));
              }
            });
  }

  private static PullRequest getPullRequestReviewList(Project project, String pullRequestId) {
    LOG.info("Fetching pull request review for repository " + pullRequestId);
    try (DevopsClient devOpsClient = getDevOpsClient()) {
      GetPullRequestRequest getPullRequestRequest =
          GetPullRequestRequest.builder()
              .pullRequestId(pullRequestId)
              .opcRequestId(getRequestId("GetPullRequest"))
              .retryConfiguration(RetryConfiguration.NO_RETRY_CONFIGURATION)
              .build();
      GetPullRequestResponse getPullRequestResponse =
          devOpsClient.getPullRequest(getPullRequestRequest);
      return getPullRequestResponse.getPullRequest();
    } catch (BmcException bmcException) {
      LOG.warn(
          "Pull request details unavailable for pullRequestId="
              + pullRequestId
              + ", status="
              + bmcException.getStatusCode()
              + ", serviceCode="
              + bmcException.getServiceCode()
              + ", opcRequestId="
              + bmcException.getOpcRequestId());
      showStatus(project, "Full pull request details unavailable for the current OCI access");
    } catch (Exception exception) {
      LOG.warn("Failed to fetch pull request info: " + exception.getMessage());
      showStatus(project, "Pull request details unavailable");
    }
    return new PullRequest.Builder().build();
  }

  private static PullRequest reviewPullRequest(
      Project project,
      String pullRequestId,
      ReviewAction reviewAction,
      ProgressIndicator indicator) {
    indicator.setText("Submitting review action...");
    try (DevopsClient devOpsClient = getDevOpsClient()) {
      ReviewPullRequestDetails reviewPullRequestDetails =
          ReviewPullRequestDetails.builder().action(reviewAction.action).build();
      ReviewPullRequestRequest reviewPullRequestRequest =
          ReviewPullRequestRequest.builder()
              .pullRequestId(pullRequestId)
              .reviewPullRequestDetails(reviewPullRequestDetails)
              .opcRequestId(getRequestId("ReviewPullRequest"))
              .retryConfiguration(RetryConfiguration.NO_RETRY_CONFIGURATION)
              .build();
      ReviewPullRequestResponse reviewPullRequestResponse =
          devOpsClient.reviewPullRequest(reviewPullRequestRequest);
      showStatus(project, "Pull request " + reviewAction.successText);
      return reviewPullRequestResponse.getPullRequest();
    } catch (BmcException bmcException) {
      LOG.warn(
          "Unable to submit review action for pullRequestId="
              + pullRequestId
              + ", status="
              + bmcException.getStatusCode()
              + ", serviceCode="
              + bmcException.getServiceCode());
      showStatus(project, "Pull request review action unavailable");
    } catch (Exception exception) {
      LOG.warn("Failed to submit pull request review: " + exception.getMessage());
      showStatus(project, "Pull request review action unavailable");
    }
    return new PullRequest.Builder().build();
  }
}
