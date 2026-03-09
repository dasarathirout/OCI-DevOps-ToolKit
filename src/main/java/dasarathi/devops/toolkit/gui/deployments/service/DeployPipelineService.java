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
import com.oracle.bmc.devops.model.DeployPipeline;
import com.oracle.bmc.devops.requests.GetDeployPipelineRequest;
import com.oracle.bmc.devops.responses.GetDeployPipelineResponse;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;

public class DeployPipelineService {
  private static final Logger LOG = Logger.getInstance(DeployPipelineService.class);

  public void fetchDeployPipeline(
      @NotNull Project project,
      @NotNull String deployPipelineId,
      @NotNull Consumer<DeployPipeline> onCompletedPipeline) {
    ProgressManager.getInstance()
        .run(
            new Task.Backgroundable(project, "Fetching deploy pipeline", true) {
              @Override
              public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                DeployPipeline pipeline = getDeployPipeline(project, deployPipelineId);
                ApplicationManager.getApplication()
                    .invokeLater(() -> onCompletedPipeline.accept(pipeline));
              }
            });
  }

  private static DeployPipeline getDeployPipeline(Project project, String deployPipelineId) {
    LOG.info("Fetching deploy pipeline deployPipelineId=" + deployPipelineId);
    try (DevopsClient devOpsClient = getDevOpsClient()) {
      GetDeployPipelineRequest request =
          GetDeployPipelineRequest.builder()
              .deployPipelineId(deployPipelineId)
              .opcRequestId(getRequestId("GetDeployPipeline"))
              .build();
      GetDeployPipelineResponse response = devOpsClient.getDeployPipeline(request);
      return response.getDeployPipeline();
    } catch (Exception exception) {
      LOG.warn("Unable to fetch deploy pipeline", exception);
      showStatus(project, "Deploy pipeline unavailable");
    }
    return DeployPipeline.builder().build();
  }
}
