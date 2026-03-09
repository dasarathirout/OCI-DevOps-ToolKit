package dasarathi.devops.toolkit.gui.repositories;

import static dasarathi.devops.toolkit.DevOpsToolKitToolWindowFactory.DEVOPS_TOOLKIT_PROJECT_SUMMARY_KEY;
import static dasarathi.devops.toolkit.DevOpsToolKitToolWindowFactory.DEVOPS_TOOLKIT_PULL_REQUEST_SUMMARY_KEY;
import static dasarathi.devops.toolkit.DevOpsToolKitToolWindowFactory.DEVOPS_TOOLKIT_REPOSITORY_SUMMARY_KEY;

import com.intellij.openapi.project.Project;
import com.oracle.bmc.devops.model.PrincipalDetails;
import com.oracle.bmc.devops.model.ProjectSummary;
import com.oracle.bmc.devops.model.PullRequest;
import com.oracle.bmc.devops.model.PullRequestSummary;
import com.oracle.bmc.devops.model.RepositorySummary;
import dasarathi.devops.toolkit.core.DevOpsToolKitSettings;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;

public final class RepositoryPanelHandler {
  private static final Comparator<PullRequestSummary> BY_CREATED_DATE =
      Comparator.comparingLong(RepositoryPanelHandler::createdTimeMillis);

  private RepositoryPanelHandler() {}

  public static List<ProjectSummary> loadProjects(@NotNull Project project) {
    List<ProjectSummary> projectSummaries = project.getUserData(DEVOPS_TOOLKIT_PROJECT_SUMMARY_KEY);
    if (projectSummaries != null && !projectSummaries.isEmpty()) {
      return projectSummaries;
    }
    List<ProjectSummary> cachedProjectSummaries =
        DevOpsToolKitSettings.getProjectSummaries(project);
    if (!cachedProjectSummaries.isEmpty()) {
      project.putUserData(DEVOPS_TOOLKIT_PROJECT_SUMMARY_KEY, cachedProjectSummaries);
      return cachedProjectSummaries;
    }
    return List.of();
  }

  public static List<RepositorySummary> loadRepositories(@NotNull Project project) {
    List<RepositorySummary> repositorySummaries =
        project.getUserData(DEVOPS_TOOLKIT_REPOSITORY_SUMMARY_KEY);
    if (repositorySummaries != null && !repositorySummaries.isEmpty()) {
      return repositorySummaries;
    }
    List<RepositorySummary> cachedRepositorySummaries =
        DevOpsToolKitSettings.getRepositorySummaries(project);
    if (!cachedRepositorySummaries.isEmpty()) {
      project.putUserData(DEVOPS_TOOLKIT_REPOSITORY_SUMMARY_KEY, cachedRepositorySummaries);
      return cachedRepositorySummaries;
    }
    return List.of();
  }

  public static List<PullRequestSummary> loadPullRequests(@NotNull Project project) {
    List<PullRequestSummary> pullRequestSummaries =
        project.getUserData(DEVOPS_TOOLKIT_PULL_REQUEST_SUMMARY_KEY);
    if (pullRequestSummaries != null && !pullRequestSummaries.isEmpty()) {
      return pullRequestSummaries;
    }
    List<PullRequestSummary> cachedPullRequestSummaries =
        DevOpsToolKitSettings.getPullRequestSummaries(project);
    if (!cachedPullRequestSummaries.isEmpty()) {
      project.putUserData(DEVOPS_TOOLKIT_PULL_REQUEST_SUMMARY_KEY, cachedPullRequestSummaries);
      return cachedPullRequestSummaries;
    }
    return List.of();
  }

  public static String resolveProjectId(
      @NotNull List<ProjectSummary> projectSummaries, @NotNull String selectedProjectName) {
    return projectSummaries.stream()
        .filter(summary -> selectedProjectName.equals(summary.getName()))
        .map(ProjectSummary::getId)
        .filter(Objects::nonNull)
        .filter(projectId -> !projectId.isBlank())
        .findFirst()
        .orElseGet(
            () ->
                projectSummaries.stream()
                    .map(ProjectSummary::getId)
                    .filter(Objects::nonNull)
                    .filter(projectId -> !projectId.isBlank())
                    .findFirst()
                    .orElse(""));
  }

  public static String resolveRepositoryId(
      @NotNull List<RepositorySummary> repositorySummaries,
      @NotNull String selectedRepositoryName) {
    return repositorySummaries.stream()
        .filter(summary -> selectedRepositoryName.equals(summary.getName()))
        .map(RepositorySummary::getId)
        .filter(Objects::nonNull)
        .filter(repositoryId -> !repositoryId.isBlank())
        .findFirst()
        .orElse("");
  }

  public static String getUserPrincipalDisplayName(
      @NotNull PullRequestSummary pullRequestSummary, @NotNull String fallback) {
    PrincipalDetails principalDetails = pullRequestSummary.getCreatedBy();
    if (principalDetails != null && principalDetails.getPrincipalName() != null) {
      return principalDetails.getPrincipalName();
    }
    return fallback;
  }

  public static String getStatusValue(@NotNull PullRequestSummary pullRequestSummary) {
    PullRequest.LifecycleDetails lifecycleDetails = pullRequestSummary.getLifecycleDetails();
    if (lifecycleDetails == PullRequest.LifecycleDetails.Open
        || lifecycleDetails == PullRequest.LifecycleDetails.Conflict) {
      return UiValue.OPENED.value();
    }
    if (lifecycleDetails == PullRequest.LifecycleDetails.Closed) {
      return UiValue.CLOSED.value();
    }
    if (lifecycleDetails == PullRequest.LifecycleDetails.Merged
        || lifecycleDetails == PullRequest.LifecycleDetails.Merging) {
      return UiValue.MERGED.value();
    }

    PullRequest.LifecycleState lifecycleState = pullRequestSummary.getLifecycleState();
    if (lifecycleState == PullRequest.LifecycleState.Active
        || lifecycleState == PullRequest.LifecycleState.Creating
        || lifecycleState == PullRequest.LifecycleState.Updating) {
      return UiValue.OPENED.value();
    }
    if (lifecycleState == PullRequest.LifecycleState.Deleting
        || lifecycleState == PullRequest.LifecycleState.Deleted) {
      return UiValue.CLOSED.value();
    }
    return UiValue.UNKNOWN_STATUS.value();
  }

  public static String getBranchValue(
      @NotNull PullRequestSummary pullRequestSummary, @NotNull String fallback) {
    return pullRequestSummary.getDestinationBranch() == null
        ? fallback
        : pullRequestSummary.getDestinationBranch();
  }

  public static boolean matchesSearch(
      @NotNull PullRequestSummary pullRequestSummary, String query) {
    if (query == null || query.isBlank()) {
      return true;
    }
    String normalizedQuery = query.trim().toLowerCase(Locale.ROOT);
    String summary = displayName(pullRequestSummary.getDisplayName(), "");
    return summary.toLowerCase(Locale.ROOT).contains(normalizedQuery);
  }

  public static long createdTimeMillis(@NotNull PullRequestSummary pullRequestSummary) {
    Date timeCreated = pullRequestSummary.getTimeCreated();
    return timeCreated == null ? 0L : timeCreated.getTime();
  }

  public static String displayName(String value, @NotNull String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }

  public static boolean hasSelection(@NotNull String allValue, @NotNull String selectedValue) {
    return !allValue.equals(selectedValue);
  }

  private static List<String> distinctSortedValues(
      @NotNull List<PullRequestSummary> pullRequests,
      @NotNull String allValue,
      @NotNull Function<PullRequestSummary, String> valueMapper) {
    Set<String> values = new TreeSet<>();
    values.add(allValue);
    pullRequests.stream().map(valueMapper).filter(Objects::nonNull).forEach(values::add);
    return List.copyOf(values);
  }

  private static boolean matchesUser(
      @NotNull PullRequestSummary pullRequestSummary,
      @NotNull String allUsers,
      @NotNull String selectedUser,
      @NotNull String unknownUser) {
    return allUsers.equals(selectedUser)
        || selectedUser.equals(getUserPrincipalDisplayName(pullRequestSummary, unknownUser));
  }

  private static boolean matchesStatus(
      @NotNull PullRequestSummary pullRequestSummary,
      @NotNull String allStatuses,
      @NotNull String selectedStatus) {
    return allStatuses.equals(selectedStatus)
        || selectedStatus.equals(getStatusValue(pullRequestSummary));
  }

  private static boolean matchesBranch(
      @NotNull PullRequestSummary pullRequestSummary,
      @NotNull String allBranches,
      @NotNull String selectedBranch,
      @NotNull String unknownBranch) {
    return allBranches.equals(selectedBranch)
        || selectedBranch.equals(getBranchValue(pullRequestSummary, unknownBranch));
  }

  public static List<PullRequestSummary> applyFilters(
      @NotNull List<PullRequestSummary> source,
      @NotNull String allUsers,
      @NotNull String selectedUser,
      @NotNull String allStatuses,
      @NotNull String selectedStatus,
      @NotNull String allBranches,
      @NotNull String selectedBranch,
      @NotNull String sortOldest,
      @NotNull String selectedSort,
      @NotNull String unknownUser,
      @NotNull String unknownBranch,
      String query) {
    Comparator<PullRequestSummary> sortComparator =
        sortOldest.equals(selectedSort) ? BY_CREATED_DATE : BY_CREATED_DATE.reversed();

    return source.stream()
        .filter(
            pullRequestSummary ->
                matchesUser(pullRequestSummary, allUsers, selectedUser, unknownUser))
        .filter(
            pullRequestSummary -> matchesStatus(pullRequestSummary, allStatuses, selectedStatus))
        .filter(
            pullRequestSummary ->
                matchesBranch(pullRequestSummary, allBranches, selectedBranch, unknownBranch))
        .filter(pullRequestSummary -> matchesSearch(pullRequestSummary, query))
        .sorted(sortComparator)
        .toList();
  }

  public static List<String> projectDisplayNames(@NotNull List<ProjectSummary> projectSummaries) {
    return projectSummaries.stream()
        .map(
            projectSummary ->
                displayName(projectSummary.getName(), UiValue.UNNAMED_PROJECT.value()))
        .toList();
  }

  public static List<String> pullRequestDisplayNames(
      @NotNull List<PullRequestSummary> pullRequestSummaryList) {
    return pullRequestSummaryList.stream()
        .map(
            pullRequestSummary ->
                displayName(
                    pullRequestSummary.getDisplayName(), UiValue.UNNAMED_PULL_REQUEST.value()))
        .toList();
  }

  public static List<String> userFilterOptions(
      @NotNull List<PullRequestSummary> pullRequestSummaryArrayList,
      @NotNull String allUsers,
      @NotNull String unknownUser) {
    return distinctSortedValues(
        pullRequestSummaryArrayList,
        allUsers,
        pullRequestSummary -> getUserPrincipalDisplayName(pullRequestSummary, unknownUser));
  }

  public static List<String> branchFilterOptions(
      @NotNull List<PullRequestSummary> pullRequestSummaryArrayList,
      @NotNull String allBranches,
      @NotNull String unknownBranch) {
    return distinctSortedValues(
        pullRequestSummaryArrayList,
        allBranches,
        pullRequestSummary -> getBranchValue(pullRequestSummary, unknownBranch));
  }

  private enum UiValue {
    OPENED("OPENED"),
    CLOSED("CLOSED"),
    MERGED("MERGED"),
    UNKNOWN_STATUS("UNKNOWN"),
    UNNAMED_PROJECT("Unnamed Project Info"),
    UNNAMED_PULL_REQUEST("Unnamed Pull Request");

    private final String value;

    UiValue(String value) {
      this.value = value;
    }

    private String value() {
      return value;
    }
  }
}
