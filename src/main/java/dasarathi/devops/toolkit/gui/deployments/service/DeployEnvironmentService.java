package dasarathi.devops.toolkit.gui.deployments.service;

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
import com.oracle.bmc.devops.model.DeployEnvironment;
import com.oracle.bmc.devops.model.DeployEnvironmentCollection;
import com.oracle.bmc.devops.model.DeployEnvironmentSummary;
import com.oracle.bmc.devops.model.SortOrder;
import com.oracle.bmc.devops.requests.GetDeployEnvironmentRequest;
import com.oracle.bmc.devops.requests.ListDeployEnvironmentsRequest;
import com.oracle.bmc.devops.responses.GetDeployEnvironmentResponse;
import com.oracle.bmc.devops.responses.ListDeployEnvironmentsResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;

public class DeployEnvironmentService {
  private static final Logger LOG = Logger.getInstance(DeployEnvironmentService.class);

  public void fetchDeployEnvironments(
      @NotNull Project project,
      @NotNull Consumer<List<DeployEnvironmentSummary>> onCompletedEnvironments) {
    ProgressManager.getInstance()
        .run(
            new Task.Backgroundable(project, "Fetching deploy environments", true) {
              @Override
              public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                List<DeployEnvironmentSummary> environments = getDeployEnvironments(project);
                ApplicationManager.getApplication()
                    .invokeLater(() -> onCompletedEnvironments.accept(environments));
              }
            });
  }

  public void fetchDeployEnvironment(
      @NotNull Project project,
      @NotNull String deployEnvironmentId,
      @NotNull Consumer<DeployEnvironment> onCompletedEnvironment) {
    ProgressManager.getInstance()
        .run(
            new Task.Backgroundable(project, "Fetching deploy environment detail", true) {
              @Override
              public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                DeployEnvironment environment = getDeployEnvironment(project, deployEnvironmentId);
                ApplicationManager.getApplication()
                    .invokeLater(() -> onCompletedEnvironment.accept(environment));
              }
            });
  }

  private static List<DeployEnvironmentSummary> getDeployEnvironments(Project project) {
    LOG.info("Fetching deploy environments");
    try (DevopsClient devOpsClient = getDevOpsClient()) {
      List<DeployEnvironmentSummary> environments = new ArrayList<>();
      String page = null;
      do {
        ListDeployEnvironmentsRequest request =
            ListDeployEnvironmentsRequest.builder()
                .compartmentId(DEVOPS_COMPARTMENT_ID)
                .sortOrder(SortOrder.Desc)
                .limit(100)
                .page(page)
                .opcRequestId(getRequestId("ListDeployEnvironments"))
                .build();
        ListDeployEnvironmentsResponse response = devOpsClient.listDeployEnvironments(request);
        DeployEnvironmentCollection collection = response.getDeployEnvironmentCollection();
        if (collection != null && collection.getItems() != null) {
          environments.addAll(collection.getItems());
        }
        page = response.getOpcNextPage();
      } while (page != null && !page.isBlank());
      return List.copyOf(environments);
    } catch (Exception exception) {
      LOG.warn("Unable to fetch deploy environments", exception);
      showStatus(project, "Deploy environments unavailable");
    }
    return List.of();
  }

  private static DeployEnvironment getDeployEnvironment(
      Project project, String deployEnvironmentId) {
    LOG.info("Fetching deploy environment detail deployEnvironmentId=" + deployEnvironmentId);
    try (DevopsClient devOpsClient = getDevOpsClient()) {
      GetDeployEnvironmentRequest request =
          GetDeployEnvironmentRequest.builder()
              .deployEnvironmentId(deployEnvironmentId)
              .opcRequestId(getRequestId("GetDeployEnvironment"))
              .build();
      GetDeployEnvironmentResponse response = devOpsClient.getDeployEnvironment(request);
      return response.getDeployEnvironment();
    } catch (Exception exception) {
      LOG.warn("Unable to fetch deploy environment detail", exception);
      showStatus(project, "Deploy environment detail unavailable");
    }
    return null;
  }
}
