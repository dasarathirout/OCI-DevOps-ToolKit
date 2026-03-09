package dasarathi.devops.toolkit.gui.repositories.service;

import static dasarathi.devops.toolkit.DevOpsToolKitToolWindowFactory.DEVOPS_TOOLKIT_REPOSITORY_SUMMARY_KEY;
import static dasarathi.devops.toolkit.core.DevOpsToolKitUtil.getDevOpsClient;
import static dasarathi.devops.toolkit.core.DevOpsToolKitUtil.getRequestId;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.oracle.bmc.devops.DevopsClient;
import com.oracle.bmc.devops.model.Repository;
import com.oracle.bmc.devops.model.RepositoryCollection;
import com.oracle.bmc.devops.model.RepositorySummary;
import com.oracle.bmc.devops.requests.ListRepositoriesRequest;
import com.oracle.bmc.devops.responses.ListRepositoriesResponse;
import dasarathi.devops.toolkit.core.DevOpsToolKitSettings;
import java.util.List;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;

public class RepositorySummaryService {
  private static final Logger LOG = Logger.getInstance(RepositorySummaryService.class);

  public void fetchRepositorySummaryList(
      @NotNull Project project,
      @NotNull String projectId,
      @NotNull Consumer<List<RepositorySummary>> onCompletedRepositorySummaryList) {
    ProgressManager.getInstance()
        .run(
            new Task.Backgroundable(project, "Fetching RepositorySummary list", true) {
              @Override
              public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                List<RepositorySummary> repositorySummaryList =
                    getRepositorySummaryList(projectId, project.getName().trim());
                project.putUserData(DEVOPS_TOOLKIT_REPOSITORY_SUMMARY_KEY, repositorySummaryList);
                DevOpsToolKitSettings.setRepositorySummaries(project, repositorySummaryList);
                ApplicationManager.getApplication()
                    .invokeLater(
                        () -> onCompletedRepositorySummaryList.accept(repositorySummaryList));
              }
            });
  }

  private static List<RepositorySummary> getRepositorySummaryList(
      String projectId, String repositoryName) {
    LOG.info("Fetching repository summary for projectId=" + projectId);
    ListRepositoriesRequest listRepositoriesRequest =
        ListRepositoriesRequest.builder()
            .projectId(projectId)
            .name(repositoryName)
            .lifecycleState(Repository.LifecycleState.Active)
            .opcRequestId(getRequestId("RepositorySummaryService"))
            .build();
    try (DevopsClient devopsClient = getDevOpsClient()) {
      ListRepositoriesResponse listRepositoriesResponse =
          devopsClient.listRepositories(listRepositoriesRequest);
      return getRepositorySummaries(listRepositoriesResponse);
    }
  }

  private static List<RepositorySummary> getRepositorySummaries(
      ListRepositoriesResponse listRepositoriesResponse) {
    if (listRepositoriesResponse == null) {
      return List.of();
    }
    RepositoryCollection repositoryCollection = listRepositoriesResponse.getRepositoryCollection();
    if (repositoryCollection == null || repositoryCollection.getItems() == null) {
      return List.of();
    }
    return List.copyOf(repositoryCollection.getItems());
  }
}
