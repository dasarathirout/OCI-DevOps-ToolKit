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
import com.oracle.bmc.devops.model.PullRequestComment;
import com.oracle.bmc.devops.requests.GetPullRequestCommentRequest;
import com.oracle.bmc.devops.responses.GetPullRequestCommentResponse;
import com.oracle.bmc.model.BmcException;
import com.oracle.bmc.retrier.RetryConfiguration;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;

public class PullRequestCommentDetailService {
  private static final Logger LOG = Logger.getInstance(PullRequestCommentDetailService.class);

  public void fetchPullRequestComment(
      @NotNull Project project,
      @NotNull String pullRequestId,
      @NotNull String commentId,
      @NotNull Consumer<PullRequestComment> onCompletedComment) {
    ProgressManager.getInstance()
        .run(
            new Task.Backgroundable(project, "Fetching pull request comment", true) {
              @Override
              public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                PullRequestComment comment =
                    getPullRequestComment(project, pullRequestId, commentId);
                ApplicationManager.getApplication()
                    .invokeLater(() -> onCompletedComment.accept(comment));
              }
            });
  }

  private static PullRequestComment getPullRequestComment(
      Project project, String pullRequestId, String commentId) {
    LOG.info("Fetching pull request comment detail commentId=" + commentId);
    try (DevopsClient devOpsClient = getDevOpsClient()) {
      GetPullRequestCommentRequest request =
          GetPullRequestCommentRequest.builder()
              .pullRequestId(pullRequestId)
              .commentId(commentId)
              .opcRequestId(getRequestId("GetPullRequestComment"))
              .retryConfiguration(RetryConfiguration.NO_RETRY_CONFIGURATION)
              .build();
      GetPullRequestCommentResponse response = devOpsClient.getPullRequestComment(request);
      return response.getPullRequestComment();
    } catch (BmcException bmcException) {
      LOG.warn(
          "Pull request comment detail unavailable for pullRequestId="
              + pullRequestId
              + ", commentId="
              + commentId
              + ", status="
              + bmcException.getStatusCode()
              + ", serviceCode="
              + bmcException.getServiceCode()
              + ", opcRequestId="
              + bmcException.getOpcRequestId());
      showStatus(project, "Pull request comment detail unavailable");
    } catch (Exception exception) {
      LOG.warn("Unable to fetch pull request comment detail", exception);
      showStatus(project, "Pull request comment detail unavailable");
    }
    return PullRequestComment.builder().build();
  }
}
