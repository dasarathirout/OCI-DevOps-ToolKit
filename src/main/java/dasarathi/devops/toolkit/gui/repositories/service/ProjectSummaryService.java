package dasarathi.devops.toolkit.gui.repositories.service;

import static dasarathi.devops.toolkit.DevOpsToolKitToolWindowFactory.DEVOPS_TOOLKIT_PROJECT_SUMMARY_KEY;
import static dasarathi.devops.toolkit.core.DevOpsToolKitConstants.DEVOPS_COMPARTMENT_ID;
import static dasarathi.devops.toolkit.core.DevOpsToolKitUtil.getDevOpsClient;
import static dasarathi.devops.toolkit.core.DevOpsToolKitUtil.getRequestId;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.oracle.bmc.devops.DevopsClient;
import com.oracle.bmc.devops.model.ProjectCollection;
import com.oracle.bmc.devops.model.ProjectSummary;
import com.oracle.bmc.devops.requests.ListProjectsRequest;
import com.oracle.bmc.devops.responses.ListProjectsResponse;
import dasarathi.devops.toolkit.core.DevOpsToolKitSettings;
import java.util.List;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;

public class ProjectSummaryService {
  private static final Logger LOG = Logger.getInstance(ProjectSummaryService.class);

  public void fetchProjectSummaryList(
      @NotNull Project project,
      @NotNull Consumer<List<ProjectSummary>> onCompletedProjectSummaryList) {
    ProgressManager.getInstance()
        .run(
            new Task.Backgroundable(project, "Fetching SCM ProjectSummary", true) {
              @Override
              public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                List<ProjectSummary> projectSummaryList = getProjectSummaryList();
                project.putUserData(DEVOPS_TOOLKIT_PROJECT_SUMMARY_KEY, projectSummaryList);
                DevOpsToolKitSettings.setProjectSummaries(project, projectSummaryList);
                ApplicationManager.getApplication()
                    .invokeLater(() -> onCompletedProjectSummaryList.accept(projectSummaryList));
              }
            });
  }

  private static List<ProjectSummary> getProjectSummaryList() {
    LOG.info("Fetching project summary list of DevOps projects summary");
    ListProjectsRequest request =
        ListProjectsRequest.builder()
            .compartmentId(DEVOPS_COMPARTMENT_ID)
            .opcRequestId(getRequestId("ListProjects"))
            .build();
    try (DevopsClient devopsClient = getDevOpsClient()) {
      ListProjectsResponse listProjectsResponse = devopsClient.listProjects(request);
      return getProjectSummaries(listProjectsResponse);
    }
  }

  private static List<ProjectSummary> getProjectSummaries(
      ListProjectsResponse listProjectsResponse) {
    if (listProjectsResponse == null) {
      return List.of();
    }
    ProjectCollection projectCollection = listProjectsResponse.getProjectCollection();
    if (projectCollection == null || projectCollection.getItems() == null) {
      return List.of();
    }
    return List.copyOf(projectCollection.getItems());
  }
}
