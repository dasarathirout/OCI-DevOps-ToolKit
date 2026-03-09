package dasarathi.devops.toolkit.core;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import com.oracle.bmc.devops.model.PrincipalDetails;
import com.oracle.bmc.devops.model.ProjectSummary;
import com.oracle.bmc.devops.model.PullRequest;
import com.oracle.bmc.devops.model.PullRequestCommentSummary;
import com.oracle.bmc.devops.model.PullRequestSummary;
import com.oracle.bmc.devops.model.RepositorySummary;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class DevOpsToolKitSettings {
  private static final String OCI_CLI_PATH_PROPERTY = "dasarathi.devops.toolkit.ociCliPath";
  private static final String PROJECT_SUMMARIES_PROPERTY =
      "dasarathi.devops.toolkit.projectSummaries";
  private static final String REPOSITORY_SUMMARIES_PROPERTY =
      "dasarathi.devops.toolkit.repositorySummaries";
  private static final String PULL_REQUEST_SUMMARIES_PROPERTY =
      "dasarathi.devops.toolkit.pullRequestSummaries";
  private static final String PULL_REQUEST_COMMENTS_PROPERTY =
      "dasarathi.devops.toolkit.pullRequestComments";
  private static final String PULL_REQUEST_REVIEW_TEXT_PROPERTY =
      "dasarathi.devops.toolkit.pullRequestReviewText";

  private DevOpsToolKitSettings() {
    /*NOOP*/
  }

  public static @Nullable String getOciCliPath(@NotNull Project project) {
    String value = PropertiesComponent.getInstance(project).getValue(OCI_CLI_PATH_PROPERTY);
    if (value == null) {
      return null;
    }

    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  public static void setOciCliPath(@NotNull Project project, @Nullable String ociCliPath) {
    PropertiesComponent properties = PropertiesComponent.getInstance(project);
    if (ociCliPath == null || ociCliPath.trim().isEmpty()) {
      properties.unsetValue(OCI_CLI_PATH_PROPERTY);
      return;
    }

    properties.setValue(OCI_CLI_PATH_PROPERTY, ociCliPath.trim());
  }

  public static void setProjectSummaries(
      @NotNull Project project, @NotNull List<ProjectSummary> projectSummaries) {
    setEncodedValue(project, PROJECT_SUMMARIES_PROPERTY, encodeProjectSummaries(projectSummaries));
  }

  public static @NotNull List<ProjectSummary> getProjectSummaries(@NotNull Project project) {
    return decodeProjectSummaries(getEncodedValue(project, PROJECT_SUMMARIES_PROPERTY));
  }

  public static void setRepositorySummaries(
      @NotNull Project project, @NotNull List<RepositorySummary> repositorySummaries) {
    setEncodedValue(
        project, REPOSITORY_SUMMARIES_PROPERTY, encodeRepositorySummaries(repositorySummaries));
  }

  public static @NotNull List<RepositorySummary> getRepositorySummaries(@NotNull Project project) {
    return decodeRepositorySummaries(getEncodedValue(project, REPOSITORY_SUMMARIES_PROPERTY));
  }

  public static void setPullRequestSummaries(
      @NotNull Project project, @NotNull List<PullRequestSummary> pullRequestSummaries) {
    setEncodedValue(
        project, PULL_REQUEST_SUMMARIES_PROPERTY, encodePullRequestSummaries(pullRequestSummaries));
  }

  public static @NotNull List<PullRequestSummary> getPullRequestSummaries(
      @NotNull Project project) {
    List<PullRequestSummary> pullRequestSummaries =
        decodePullRequestSummaries(getEncodedValue(project, PULL_REQUEST_SUMMARIES_PROPERTY));
    if (containsCorruptedPullRequestSummaryCache(pullRequestSummaries)) {
      PropertiesComponent.getInstance(project).unsetValue(PULL_REQUEST_SUMMARIES_PROPERTY);
      return List.of();
    }
    return pullRequestSummaries;
  }

  public static void setPullRequestComments(
      @NotNull Project project, @NotNull List<PullRequestCommentSummary> pullRequestComments) {
    setEncodedValue(
        project, PULL_REQUEST_COMMENTS_PROPERTY, encodePullRequestComments(pullRequestComments));
  }

  public static @NotNull List<PullRequestCommentSummary> getPullRequestComments(
      @NotNull Project project) {
    return decodePullRequestComments(getEncodedValue(project, PULL_REQUEST_COMMENTS_PROPERTY));
  }

  public static void setPullRequestReviewText(
      @NotNull Project project, @Nullable String pullRequestReviewText) {
    setEncodedValue(
        project,
        PULL_REQUEST_REVIEW_TEXT_PROPERTY,
        pullRequestReviewText == null ? "" : escape(pullRequestReviewText));
  }

  public static @Nullable String getPullRequestReviewText(@NotNull Project project) {
    String encoded = getEncodedValue(project, PULL_REQUEST_REVIEW_TEXT_PROPERTY);
    if (encoded.isBlank()) {
      return null;
    }
    return fieldValue(decodeFields(encoded, 1), 0);
  }

  private static void setEncodedValue(
      @NotNull Project project, @NotNull String propertyName, @NotNull String value) {
    PropertiesComponent properties = PropertiesComponent.getInstance(project);
    if (value.isBlank()) {
      properties.unsetValue(propertyName);
      return;
    }
    properties.setValue(propertyName, value);
  }

  private static @NotNull String getEncodedValue(
      @NotNull Project project, @NotNull String propertyName) {
    String value = PropertiesComponent.getInstance(project).getValue(propertyName);
    return value == null ? "" : value;
  }

  private static @NotNull String encodeProjectSummaries(
      @NotNull List<ProjectSummary> projectSummaries) {
    List<String> rows = new ArrayList<>();
    for (ProjectSummary summary : projectSummaries) {
      rows.add(encodeFields(summary.getId(), summary.getName()));
    }
    return String.join("\n", rows);
  }

  private static @NotNull List<ProjectSummary> decodeProjectSummaries(@NotNull String raw) {
    if (raw.isBlank()) {
      return List.of();
    }

    List<ProjectSummary> summaries = new ArrayList<>();
    for (String line : raw.split("\\n", -1)) {
      if (line.isBlank()) {
        continue;
      }
      List<String> fields = decodeFields(line, 2);
      if (fields.isEmpty()) {
        continue;
      }
      summaries.add(
          ProjectSummary.builder().id(fieldValue(fields, 0)).name(fieldValue(fields, 1)).build());
    }
    return List.copyOf(summaries);
  }

  private static @NotNull String encodeRepositorySummaries(
      @NotNull List<RepositorySummary> repositorySummaries) {
    List<String> rows = new ArrayList<>();
    for (RepositorySummary summary : repositorySummaries) {
      rows.add(encodeFields(summary.getId(), summary.getName(), summary.getProjectId()));
    }
    return String.join("\n", rows);
  }

  private static @NotNull List<RepositorySummary> decodeRepositorySummaries(@NotNull String raw) {
    if (raw.isBlank()) {
      return List.of();
    }

    List<RepositorySummary> summaries = new ArrayList<>();
    for (String line : raw.split("\\n", -1)) {
      if (line.isBlank()) {
        continue;
      }
      List<String> fields = decodeFields(line, 3);
      if (fields.isEmpty()) {
        continue;
      }
      summaries.add(
          RepositorySummary.builder()
              .id(fieldValue(fields, 0))
              .name(fieldValue(fields, 1))
              .projectId(fieldValue(fields, 2))
              .build());
    }
    return List.copyOf(summaries);
  }

  private static @NotNull String encodePullRequestSummaries(
      @NotNull List<PullRequestSummary> pullRequestSummaries) {
    List<String> rows = new ArrayList<>();
    for (PullRequestSummary summary : pullRequestSummaries) {
      String principalName = null;
      PrincipalDetails createdBy = summary.getCreatedBy();
      if (createdBy != null) {
        principalName = createdBy.getPrincipalName();
      }
      rows.add(
          encodeFields(
              summary.getId(),
              summary.getDisplayName(),
              summary.getDestinationBranch(),
              summary.getLifecycleState() == null ? null : summary.getLifecycleState().name(),
              summary.getLifecycleDetails() == null ? null : summary.getLifecycleDetails().name(),
              principalName,
              Long.toString(
                  summary.getTimeCreated() == null ? 0L : summary.getTimeCreated().getTime())));
    }
    return String.join("\n", rows);
  }

  private static @NotNull List<PullRequestSummary> decodePullRequestSummaries(@NotNull String raw) {
    if (raw.isBlank()) {
      return List.of();
    }

    List<PullRequestSummary> summaries = new ArrayList<>();
    for (String line : raw.split("\\n", -1)) {
      if (line.isBlank()) {
        continue;
      }
      List<String> rawFields = decodeFields(line, 0);
      List<String> fields = decodeFields(line, rawFields.size() >= 7 ? 7 : 6);
      if (fields.isEmpty()) {
        continue;
      }

      boolean hasLifecycleDetails = rawFields.size() >= 7;
      PullRequestSummary.Builder builder =
          PullRequestSummary.builder()
              .id(fieldValue(fields, 0))
              .displayName(fieldValue(fields, 1))
              .destinationBranch(fieldValue(fields, 2))
              .timeCreated(parseDate(fieldValue(fields, hasLifecycleDetails ? 6 : 5)));

      PullRequest.LifecycleState lifecycleState = parseLifecycleState(fieldValue(fields, 3));
      if (lifecycleState != null) {
        builder.lifecycleState(lifecycleState);
      }

      if (hasLifecycleDetails) {
        PullRequest.LifecycleDetails lifecycleDetails =
            parseLifecycleDetails(fieldValue(fields, 4));
        if (lifecycleDetails != null) {
          builder.lifecycleDetails(lifecycleDetails);
        }
      }

      String principalName = fieldValue(fields, hasLifecycleDetails ? 5 : 4);
      if (principalName != null) {
        builder.createdBy(PrincipalDetails.builder().principalName(principalName).build());
      }

      summaries.add(builder.build());
    }
    return List.copyOf(summaries);
  }

  private static @NotNull String encodePullRequestComments(
      @NotNull List<PullRequestCommentSummary> pullRequestComments) {
    List<String> rows = new ArrayList<>();
    for (PullRequestCommentSummary comment : pullRequestComments) {
      rows.add(
          encodeFields(
              comment.getId(),
              comment.getPullRequestId(),
              comment.getData(),
              comment.getFilePath(),
              comment.getCommitId(),
              comment.getLineNumber() == null ? null : Integer.toString(comment.getLineNumber())));
    }
    return String.join("\n", rows);
  }

  private static @NotNull List<PullRequestCommentSummary> decodePullRequestComments(
      @NotNull String raw) {
    if (raw.isBlank()) {
      return List.of();
    }
    List<PullRequestCommentSummary> comments = new ArrayList<>();
    for (String line : raw.split("\\n", -1)) {
      if (line.isBlank()) {
        continue;
      }
      List<String> fields = decodeFields(line, 6);
      comments.add(
          PullRequestCommentSummary.builder()
              .id(fieldValue(fields, 0))
              .pullRequestId(fieldValue(fields, 1))
              .data(fieldValue(fields, 2))
              .filePath(fieldValue(fields, 3))
              .commitId(fieldValue(fields, 4))
              .lineNumber(parseInteger(fieldValue(fields, 5)))
              .build());
    }
    return List.copyOf(comments);
  }

  private static @Nullable Date parseDate(@Nullable String timeMillisValue) {
    if (timeMillisValue == null || timeMillisValue.isBlank()) {
      return null;
    }
    try {
      long timeMillis = Long.parseLong(timeMillisValue);
      return timeMillis <= 0L ? null : new Date(timeMillis);
    } catch (NumberFormatException ignored) {
      return null;
    }
  }

  private static @Nullable Integer parseInteger(@Nullable String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException ignored) {
      return null;
    }
  }

  private static @Nullable PullRequest.LifecycleState parseLifecycleState(@Nullable String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return PullRequest.LifecycleState.valueOf(value);
    } catch (IllegalArgumentException ignored) {
      return null;
    }
  }

  private static @Nullable PullRequest.LifecycleDetails parseLifecycleDetails(
      @Nullable String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return PullRequest.LifecycleDetails.valueOf(value);
    } catch (IllegalArgumentException ignored) {
      return null;
    }
  }

  private static boolean containsCorruptedPullRequestSummaryCache(
      @NotNull List<PullRequestSummary> pullRequestSummaries) {
    for (PullRequestSummary summary : pullRequestSummaries) {
      PrincipalDetails createdBy = summary.getCreatedBy();
      String principalName = createdBy == null ? null : createdBy.getPrincipalName();
      if (principalName != null
          && principalName.matches("\\d{10,}")
          && summary.getTimeCreated() == null) {
        return true;
      }
    }
    return false;
  }

  private static @Nullable String fieldValue(@NotNull List<String> fields, int index) {
    if (index < 0 || index >= fields.size()) {
      return null;
    }
    return emptyToNull(fields.get(index));
  }

  private static @Nullable String emptyToNull(@Nullable String value) {
    return value == null || value.isEmpty() ? null : value;
  }

  private static @NotNull String encodeFields(String... fields) {
    List<String> encoded = new ArrayList<>();
    for (String field : fields) {
      encoded.add(escape(field == null ? "" : field));
    }
    return String.join("|", encoded);
  }

  private static @NotNull List<String> decodeFields(@NotNull String line, int expectedFields) {
    List<String> values = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    boolean escaping = false;
    for (int i = 0; i < line.length(); i++) {
      char currentChar = line.charAt(i);
      if (escaping) {
        current.append(currentChar);
        escaping = false;
      } else if (currentChar == '\\') {
        escaping = true;
      } else if (currentChar == '|') {
        values.add(current.toString());
        current.setLength(0);
      } else {
        current.append(currentChar);
      }
    }
    if (escaping) {
      current.append('\\');
    }
    values.add(current.toString());
    while (values.size() < expectedFields) {
      values.add("");
    }
    return List.copyOf(values);
  }

  private static @NotNull String escape(@NotNull String value) {
    StringBuilder encoded = new StringBuilder();
    for (int i = 0; i < value.length(); i++) {
      char current = value.charAt(i);
      if (current == '\\' || current == '|' || current == '\n' || current == '\r') {
        encoded.append('\\');
      }
      if (current != '\r') {
        encoded.append(current);
      }
    }
    return encoded.toString();
  }
}
