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
import com.oracle.bmc.devops.model.DeploymentCollection;
import com.oracle.bmc.devops.model.DeploymentSummary;
import com.oracle.bmc.devops.model.SortOrder;
import com.oracle.bmc.devops.requests.ListDeploymentsRequest;
import com.oracle.bmc.devops.responses.ListDeploymentsResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;

public class DeploymentService {
  private static final Logger LOG = Logger.getInstance(DeploymentService.class);

  public void fetchDeployments(
      @NotNull Project project, @NotNull Consumer<List<DeploymentSummary>> onCompletedDeployments) {
    ProgressManager.getInstance()
        .run(
            new Task.Backgroundable(project, "Fetching deployments", true) {
              @Override
              public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                List<DeploymentSummary> deployments = getDeployments(project);
                ApplicationManager.getApplication()
                    .invokeLater(() -> onCompletedDeployments.accept(deployments));
              }
            });
  }

  private static List<DeploymentSummary> getDeployments(Project project) {
    LOG.info("Fetching deployments");
    try (DevopsClient devOpsClient = getDevOpsClient()) {
      List<DeploymentSummary> deployments = new ArrayList<>();
      String page = null;
      do {
        ListDeploymentsRequest request =
            ListDeploymentsRequest.builder()
                .compartmentId(DEVOPS_COMPARTMENT_ID)
                .sortOrder(SortOrder.Desc)
                .limit(100)
                .page(page)
                .opcRequestId(getRequestId("ListDeployments"))
                .build();
        ListDeploymentsResponse response = devOpsClient.listDeployments(request);
        DeploymentCollection collection = response.getDeploymentCollection();
        if (collection != null && collection.getItems() != null) {
          deployments.addAll(collection.getItems());
        }
        page = response.getOpcNextPage();
      } while (page != null && !page.isBlank());
      return List.copyOf(deployments);
    } catch (Exception exception) {
      LOG.warn("Unable to fetch deployments", exception);
      showStatus(project, "Deployments unavailable");
    }
    return List.of();
  }
}
