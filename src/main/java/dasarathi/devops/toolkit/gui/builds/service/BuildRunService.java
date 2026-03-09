package dasarathi.devops.toolkit.gui.builds.service;

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
import com.oracle.bmc.devops.model.BuildRun;
import com.oracle.bmc.devops.model.BuildRunSummary;
import com.oracle.bmc.devops.model.BuildRunSummaryCollection;
import com.oracle.bmc.devops.model.SortOrder;
import com.oracle.bmc.devops.requests.GetBuildRunRequest;
import com.oracle.bmc.devops.requests.ListBuildRunsRequest;
import com.oracle.bmc.devops.responses.GetBuildRunResponse;
import com.oracle.bmc.devops.responses.ListBuildRunsResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;

public class BuildRunService {
  private static final Logger LOG = Logger.getInstance(BuildRunService.class);

  public void fetchBuildRuns(
      @NotNull Project project, @NotNull Consumer<List<BuildRunSummary>> onCompletedBuildRuns) {
    ProgressManager.getInstance()
        .run(
            new Task.Backgroundable(project, "Fetching build runs", true) {
              @Override
              public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                List<BuildRunSummary> buildRuns = getBuildRuns(project);
                ApplicationManager.getApplication()
                    .invokeLater(() -> onCompletedBuildRuns.accept(buildRuns));
              }
            });
  }

  public void fetchBuildRun(
      @NotNull Project project,
      @NotNull String buildRunId,
      @NotNull Consumer<BuildRun> onCompletedBuildRun) {
    ProgressManager.getInstance()
        .run(
            new Task.Backgroundable(project, "Fetching build run detail", true) {
              @Override
              public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                BuildRun buildRun = getBuildRun(project, buildRunId);
                ApplicationManager.getApplication()
                    .invokeLater(() -> onCompletedBuildRun.accept(buildRun));
              }
            });
  }

  private static List<BuildRunSummary> getBuildRuns(Project project) {
    LOG.info("Fetching build runs");
    try (DevopsClient devOpsClient = getDevOpsClient()) {
      List<BuildRunSummary> buildRuns = new ArrayList<>();
      String page = null;
      do {
        ListBuildRunsRequest request =
            ListBuildRunsRequest.builder()
                .compartmentId(DEVOPS_COMPARTMENT_ID)
                .sortOrder(SortOrder.Desc)
                .limit(100)
                .page(page)
                .opcRequestId(getRequestId("ListBuildRuns"))
                .build();
        ListBuildRunsResponse response = devOpsClient.listBuildRuns(request);
        BuildRunSummaryCollection collection = response.getBuildRunSummaryCollection();
        if (collection != null && collection.getItems() != null) {
          buildRuns.addAll(collection.getItems());
        }
        page = response.getOpcNextPage();
      } while (page != null && !page.isBlank());
      return List.copyOf(buildRuns);
    } catch (Exception exception) {
      LOG.warn("Unable to fetch build runs", exception);
      showStatus(project, "Build runs unavailable");
    }
    return List.of();
  }

  private static BuildRun getBuildRun(Project project, String buildRunId) {
    LOG.info("Fetching build run detail buildRunId=" + buildRunId);
    try (DevopsClient devOpsClient = getDevOpsClient()) {
      GetBuildRunRequest request =
          GetBuildRunRequest.builder()
              .buildRunId(buildRunId)
              .opcRequestId(getRequestId("GetBuildRun"))
              .build();
      GetBuildRunResponse response = devOpsClient.getBuildRun(request);
      return response.getBuildRun();
    } catch (Exception exception) {
      LOG.warn("Unable to fetch build run detail", exception);
      showStatus(project, "Build run detail unavailable");
    }
    return BuildRun.builder().build();
  }
}
