package dasarathi.devops.toolkit.gui.repositories;

import com.intellij.icons.AllIcons;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.panels.NonOpaquePanel;
import com.intellij.util.ui.JBUI;
import com.oracle.bmc.devops.model.DiffChunk;
import com.oracle.bmc.devops.model.DiffLineDetails;
import com.oracle.bmc.devops.model.DiffSection;
import com.oracle.bmc.devops.model.FileDiffResponse;
import com.oracle.bmc.devops.model.PullRequest;
import com.oracle.bmc.devops.model.PullRequestCommentSummary;
import com.oracle.bmc.devops.model.PullRequestFileChangeSummary;
import com.oracle.bmc.devops.model.PullRequestSummary;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.List;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import org.jetbrains.annotations.NotNull;

final class PullRequestPresentation {
  private PullRequestPresentation() {}

  static @NotNull Component createOverviewCard(@NotNull PullRequestSummary summary) {
    JBPanel<?> card = new JBPanel<>(new BorderLayout(0, 8));
    card.setBorder(
        BorderFactory.createCompoundBorder(
            JBUI.Borders.customLine(JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground()),
            JBUI.Borders.empty(12)));

    SimpleColoredComponent title = new SimpleColoredComponent();
    title.setIcon(AllIcons.Vcs.Vendors.Github);
    title.append(
        display(summary.getDisplayName(), "Unnamed Pull Request"),
        SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
    title.append("  #" + display(summary.getId(), "N/A"), SimpleTextAttributes.GRAY_ATTRIBUTES);
    card.add(title, BorderLayout.NORTH);

    JPanel meta = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
    meta.setOpaque(false);
    meta.add(
        createTag(
            "Status",
            summary.getLifecycleState() == null ? "N/A" : summary.getLifecycleState().name()));
    meta.add(createTag("Target", display(summary.getDestinationBranch(), "N/A")));
    meta.add(
        createTag(
            "Author", RepositoryPanelHandler.getUserPrincipalDisplayName(summary, "Unknown User")));
    card.add(meta, BorderLayout.CENTER);
    return card;
  }

  static @NotNull Component createSectionPanel(
      @NotNull String title, @NotNull String helperText, @NotNull Component content) {
    JPanel panel = new NonOpaquePanel(new BorderLayout(0, 8));
    JPanel header = new JPanel();
    header.setOpaque(false);
    header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

    JBLabel titleLabel = new JBLabel(title);
    titleLabel.setFont(JBUI.Fonts.label(13f).asBold());
    JBLabel helperLabel = new JBLabel(helperText);
    helperLabel.setForeground(JBUI.CurrentTheme.ContextHelp.FOREGROUND);
    header.add(titleLabel);
    header.add(helperLabel);

    panel.add(header, BorderLayout.NORTH);
    panel.add(content, BorderLayout.CENTER);
    return panel;
  }

  static @NotNull String formatPullRequestDetails(@NotNull PullRequest pullRequest) {
    StringBuilder builder = new StringBuilder();
    builder.append("ID: ").append(display(pullRequest.getId(), "N/A")).append('\n');
    builder
        .append("Title: ")
        .append(display(pullRequest.getDisplayName(), "Unnamed Pull Request"))
        .append('\n');
    builder
        .append("Status: ")
        .append(
            pullRequest.getLifecycleState() == null
                ? "N/A"
                : pullRequest.getLifecycleState().name())
        .append('\n');
    builder
        .append("Source Branch: ")
        .append(display(pullRequest.getSourceBranch(), "N/A"))
        .append('\n');
    builder
        .append("Destination Branch: ")
        .append(display(pullRequest.getDestinationBranch(), "N/A"))
        .append('\n');
    builder
        .append("Description: ")
        .append(display(pullRequest.getDescription(), "No description available"));
    return builder.toString();
  }

  static @NotNull String formatPullRequestSummaryDetails(@NotNull PullRequestSummary summary) {
    StringBuilder builder = new StringBuilder();
    builder.append("ID: ").append(display(summary.getId(), "N/A")).append('\n');
    builder
        .append("Title: ")
        .append(display(summary.getDisplayName(), "Unnamed Pull Request"))
        .append('\n');
    builder
        .append("Status: ")
        .append(summary.getLifecycleState() == null ? "N/A" : summary.getLifecycleState().name())
        .append('\n');
    builder
        .append("Destination Branch: ")
        .append(display(summary.getDestinationBranch(), "N/A"))
        .append('\n');
    builder
        .append("Author: ")
        .append(RepositoryPanelHandler.getUserPrincipalDisplayName(summary, "Unknown User"))
        .append('\n');
    builder.append('\n');
    builder.append("Full pull request details could not be loaded with the current OCI access.\n");
    builder.append("Pull request summary, comments, and diff context may still be available.");
    return builder.toString();
  }

  static @NotNull String formatPullRequestCommentSummary(
      @NotNull PullRequestCommentSummary comment) {
    StringBuilder builder = new StringBuilder();
    builder
        .append(display(comment.getFilePath(), "N/A"))
        .append(" : ")
        .append(comment.getLineNumber() == null ? "N/A" : comment.getLineNumber())
        .append('\n');
    builder.append(display(comment.getData(), "No comment text available"));
    return builder.toString();
  }

  static @NotNull String formatPullRequestFileChangeSummary(
      @NotNull PullRequestFileChangeSummary change) {
    StringBuilder builder = new StringBuilder();
    builder.append(resolvePath(change)).append('\n');
    builder
        .append(display(change.getChangeType(), "N/A"))
        .append("  +")
        .append(zeroIfNull(change.getAddedLinesCount()))
        .append(" / -")
        .append(zeroIfNull(change.getDeletedLinesCount()));
    if (Boolean.TRUE.equals(change.getHasConflicts())) {
      builder.append("  conflicts");
    }
    return builder.toString();
  }

  static @NotNull String formatPullRequestFileChangeDetail(
      @NotNull PullRequestFileChangeSummary change) {
    StringBuilder builder = new StringBuilder();
    builder.append("File: ").append(resolvePath(change)).append('\n');
    builder.append("Type: ").append(display(change.getChangeType(), "N/A")).append('\n');
    builder
        .append("Lines: +")
        .append(zeroIfNull(change.getAddedLinesCount()))
        .append(" / -")
        .append(zeroIfNull(change.getDeletedLinesCount()))
        .append('\n');
    builder
        .append("Conflicts: ")
        .append(Boolean.TRUE.equals(change.getHasConflicts()) ? "yes" : "no");
    return builder.toString();
  }

  static @NotNull String formatCommentDerivedDiffSummary(
      @NotNull String filePath, @NotNull List<PullRequestCommentSummary> comments) {
    StringBuilder builder = new StringBuilder();
    builder.append("File: ").append(display(filePath, "N/A")).append('\n');
    builder.append("Context source: pull request comments").append('\n');
    builder.append("Comments: ").append(comments.size()).append("\n\n");
    for (PullRequestCommentSummary comment : comments) {
      builder
          .append("Line: ")
          .append(comment.getLineNumber() == null ? "N/A" : comment.getLineNumber())
          .append('\n');
      builder
          .append("Comment: ")
          .append(display(comment.getData(), "No comment text available"))
          .append("\n\n");
    }
    return builder.toString().trim();
  }

  static @NotNull String formatCommentDerivedDiffRow(
      @NotNull String filePath, @NotNull List<PullRequestCommentSummary> comments) {
    StringBuilder builder = new StringBuilder();
    builder.append(display(filePath, "N/A")).append('\n');
    builder.append(comments.size()).append(comments.size() == 1 ? " comment" : " comments");
    return builder.toString();
  }

  static @NotNull String formatRepoFileDiff(@NotNull FileDiffResponse diffResponse) {
    if (Boolean.TRUE.equals(diffResponse.getIsBinary())) {
      return formatRepoFileDiffHeader(diffResponse) + "\n\nBinary file diff unavailable.";
    }
    if (Boolean.TRUE.equals(diffResponse.getIsLarge())) {
      return formatRepoFileDiffHeader(diffResponse) + "\n\nDiff omitted because the file is large.";
    }

    StringBuilder builder = new StringBuilder();
    builder.append(formatRepoFileDiffHeader(diffResponse));
    List<DiffChunk> changes = diffResponse.getChanges();
    if (changes == null || changes.isEmpty()) {
      builder.append("\n\nNo line-level patch returned.");
      return builder.toString();
    }

    for (DiffChunk chunk : changes) {
      builder.append("\n\n");
      builder
          .append("@@ -")
          .append(zeroIfNull(chunk.getBaseLine()))
          .append(",")
          .append(zeroIfNull(chunk.getBaseSpan()))
          .append(" +")
          .append(zeroIfNull(chunk.getTargetLine()))
          .append(",")
          .append(zeroIfNull(chunk.getTargetSpan()))
          .append(" @@");
      List<DiffSection> sections = chunk.getDiffSections();
      if (sections == null) {
        continue;
      }
      for (DiffSection section : sections) {
        List<DiffLineDetails> lines = section.getLines();
        if (lines == null) {
          continue;
        }
        for (DiffLineDetails line : lines) {
          builder.append('\n').append(resolveDiffPrefix(line)).append(line.getLineContent());
        }
      }
    }
    return builder.toString();
  }

  static @NotNull String formatPullRequestFileChanges(
      @NotNull List<PullRequestFileChangeSummary> fileChanges,
      @NotNull List<PullRequestCommentSummary> comments,
      @NotNull String emptyFallback) {
    if (fileChanges.isEmpty()) {
      return formatCommentPaths(comments, emptyFallback);
    }

    int totalAdded = 0;
    int totalDeleted = 0;
    int conflicts = 0;
    StringBuilder builder = new StringBuilder();
    builder.append("Changed files: ").append(fileChanges.size()).append('\n');
    for (PullRequestFileChangeSummary change : fileChanges) {
      totalAdded += zeroIfNull(change.getAddedLinesCount());
      totalDeleted += zeroIfNull(change.getDeletedLinesCount());
      if (Boolean.TRUE.equals(change.getHasConflicts())) {
        conflicts++;
      }
    }
    builder
        .append("Line impact: +")
        .append(totalAdded)
        .append(" / -")
        .append(totalDeleted)
        .append('\n');
    builder.append("Conflicts: ").append(conflicts).append("\n\n");

    for (PullRequestFileChangeSummary change : fileChanges) {
      builder.append("• ").append(resolvePath(change)).append('\n');
      builder.append("  Type: ").append(display(change.getChangeType(), "N/A")).append('\n');
      builder
          .append("  Lines: +")
          .append(zeroIfNull(change.getAddedLinesCount()))
          .append(" / -")
          .append(zeroIfNull(change.getDeletedLinesCount()))
          .append('\n');
      if (Boolean.TRUE.equals(change.getHasConflicts())) {
        builder.append("  Conflicts: yes\n");
      }
      builder.append('\n');
    }
    return builder.toString().trim();
  }

  private static @NotNull String formatCommentPaths(
      @NotNull List<PullRequestCommentSummary> comments, @NotNull String emptyFallback) {
    StringBuilder builder = new StringBuilder();
    comments.stream()
        .map(PullRequestCommentSummary::getFilePath)
        .filter(path -> path != null && !path.isBlank())
        .distinct()
        .forEach(path -> builder.append("• ").append(path).append('\n'));
    if (builder.isEmpty()) {
      return emptyFallback;
    }
    return builder.toString().trim();
  }

  private static int zeroIfNull(Integer value) {
    return value == null ? 0 : value;
  }

  private static @NotNull String formatRepoFileDiffHeader(@NotNull FileDiffResponse diffResponse) {
    StringBuilder builder = new StringBuilder();
    builder
        .append("--- ")
        .append(display(diffResponse.getOldPath(), display(diffResponse.getNewPath(), "N/A")))
        .append('\n');
    builder
        .append("+++ ")
        .append(display(diffResponse.getNewPath(), display(diffResponse.getOldPath(), "N/A")));
    if (Boolean.TRUE.equals(diffResponse.getAreConflictsInFile())) {
      builder.append("\nConflicts: yes");
    }
    return builder.toString();
  }

  private static char resolveDiffPrefix(@NotNull DiffLineDetails line) {
    if (line.getBaseLine() == null && line.getTargetLine() != null) {
      return '+';
    }
    if (line.getBaseLine() != null && line.getTargetLine() == null) {
      return '-';
    }
    return ' ';
  }

  private static @NotNull String resolvePath(@NotNull PullRequestFileChangeSummary change) {
    String newPath = change.getNewPath();
    String oldPath = change.getOldPath();
    if (newPath != null && !newPath.isBlank()) {
      if (oldPath != null && !oldPath.isBlank() && !Objects.equals(oldPath, newPath)) {
        return oldPath + " -> " + newPath;
      }
      return newPath;
    }
    return display(oldPath, "N/A");
  }

  private static @NotNull Component createTag(@NotNull String label, @NotNull String value) {
    JBLabel tag = new JBLabel(label + ": " + value);
    tag.setBorder(
        JBUI.Borders.compound(
            JBUI.Borders.customLine(JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground()),
            JBUI.Borders.empty(4, 8)));
    return tag;
  }

  private static @NotNull String display(String value, @NotNull String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }
}
