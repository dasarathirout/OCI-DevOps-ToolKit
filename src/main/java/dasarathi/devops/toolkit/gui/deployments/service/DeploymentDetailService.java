package dasarathi.devops.toolkit.gui.deployments.service;

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
import com.oracle.bmc.devops.model.Deployment;
import com.oracle.bmc.devops.requests.GetDeploymentRequest;
import com.oracle.bmc.devops.responses.GetDeploymentResponse;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;

public class DeploymentDetailService {
  private static final Logger LOG = Logger.getInstance(DeploymentDetailService.class);

  public void fetchDeployment(
      @NotNull Project project,
      @NotNull String deploymentId,
      @NotNull Consumer<Deployment> onCompletedDeployment) {
    ProgressManager.getInstance()
        .run(
            new Task.Backgroundable(project, "Fetching deployment detail", true) {
              @Override
              public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                Deployment deployment = getDeployment(project, deploymentId);
                ApplicationManager.getApplication()
                    .invokeLater(() -> onCompletedDeployment.accept(deployment));
              }
            });
  }

  private static Deployment getDeployment(Project project, String deploymentId) {
    LOG.info("Fetching deployment detail deploymentId=" + deploymentId);
    try (DevopsClient devOpsClient = getDevOpsClient()) {
      GetDeploymentRequest request =
          GetDeploymentRequest.builder()
              .deploymentId(deploymentId)
              .opcRequestId(getRequestId("GetDeployment"))
              .build();
      GetDeploymentResponse response = devOpsClient.getDeployment(request);
      return response.getDeployment();
    } catch (Exception exception) {
      LOG.warn("Unable to fetch deployment detail", exception);
      showStatus(project, "Deployment detail unavailable");
    }
    return null;
  }
}
