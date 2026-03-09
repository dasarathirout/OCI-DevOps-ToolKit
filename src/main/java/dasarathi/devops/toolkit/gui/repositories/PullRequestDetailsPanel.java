package dasarathi.devops.toolkit.gui.repositories;

import static dasarathi.devops.toolkit.event.CustomEventNotification.showStatus;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.components.panels.NonOpaquePanel;
import com.intellij.util.ui.JBUI;
import com.oracle.bmc.devops.model.PullRequest;
import com.oracle.bmc.devops.model.PullRequestComment;
import com.oracle.bmc.devops.model.PullRequestCommentSummary;
import com.oracle.bmc.devops.model.PullRequestFileChangeSummary;
import com.oracle.bmc.devops.model.PullRequestSummary;
import dasarathi.devops.toolkit.core.DevOpsToolKitSettings;
import dasarathi.devops.toolkit.gui.repositories.pullrequest.PullRequestCommentDetailService;
import dasarathi.devops.toolkit.gui.repositories.pullrequest.PullRequestCommentsService;
import dasarathi.devops.toolkit.gui.repositories.pullrequest.PullRequestFileChangesService;
import dasarathi.devops.toolkit.gui.repositories.pullrequest.PullRequestMergeService;
import dasarathi.devops.toolkit.gui.repositories.pullrequest.PullRequestReviewService;
import dasarathi.devops.toolkit.gui.repositories.pullrequest.PullRequestReviewService.ReviewAction;
import dasarathi.devops.toolkit.gui.repositories.pullrequest.RepoFileDiffService;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class PullRequestDetailsPanel {
  private static final int DIFF_TAB_INDEX = 1;
  private static final String PATCH_ACTION_KEY = "patchAction";
  private static final String PATCH_PAYLOAD_KEY = "patchPayload";
  private static final String PATCH_ACTION_OPEN_FILE = "openFile";
  private static final String PATCH_ACTION_OPEN_HUNK = "openHunk";
  private final Project currentProject;
  private final JBLabel reviewSummaryLabel = new JBLabel(UiText.SELECT_PR_TO_REVIEW.value());
  private final JTextArea reviewDetailsArea = new JTextArea();
  private final CollectionListModel<String> diffFileListModel = new CollectionListModel<>();
  private final JBList<String> diffFileList = new JBList<>(diffFileListModel);
  private final JTextPane diffSummaryArea = new JTextPane();
  private final JTextArea reviewDraftArea = new JTextArea(5, 20);
  private final CollectionListModel<String> commentListModel = new CollectionListModel<>();
  private final JBList<String> commentList = new JBList<>(commentListModel);
  private final JTextArea commentDetailArea = new JTextArea();
  private final JButton approveButton = new JButton(UiText.APPROVE_BUTTON_LABEL.value());
  private final JButton unapproveButton = new JButton(UiText.UNAPPROVE_BUTTON_LABEL.value());
  private final JButton mergeButton = new JButton(UiText.MERGE_BUTTON_LABEL.value());
  private final JBPanel<?> overviewPanel = new JBPanel<>(new BorderLayout());
  private final JBTabbedPane detailTabs = new JBTabbedPane();

  private PullRequestSummary selectedPullRequestSummary;
  private List<PullRequestCommentSummary> currentComments = List.of();
  private List<PullRequestFileChangeSummary> currentFileChanges = List.of();
  private List<DiffEntry> currentDiffEntries = List.of();
  private final Map<String, String> diffPatchCache = new HashMap<>();
  private @Nullable String currentPatchFilePath;
  private @Nullable String requestedDiffPullRequestId;
  private JPanel detailPanel;

  private record DiffEntry(String anchor, String rowText, String detailText, String filePath) {}

  PullRequestDetailsPanel(@NotNull Project currentProject) {
    this.currentProject = currentProject;
    configureAreas();
    configureReviewButtons();
    configureDiffList();
    configureCommentList();
    configureDiffDetailInteractions();
    configureDetailTabs();
  }

  JComponent getComponent() {
    if (detailPanel == null) {
      detailPanel = createDetailPanel();
    }
    return detailPanel;
  }

  void displayPullRequest(@Nullable PullRequestSummary pullRequestSummary) {
    selectedPullRequestSummary = pullRequestSummary;
    if (pullRequestSummary == null || pullRequestSummary.getId() == null) {
      reset();
      return;
    }

    showOverview(pullRequestSummary);
    setReviewActionButtonsEnabled(false);
    reviewSummaryLabel.setText(UiText.LOADING_REVIEW_DETAILS.value());
    reviewDetailsArea.setText(UiText.LOADING_REVIEW_DETAILS.value());
    diffPatchCache.clear();
    requestedDiffPullRequestId = null;
    showDiffPlaceholder(UiText.LOADING_DIFF_DETAILS.value());

    List<PullRequestCommentSummary> cachedComments =
        DevOpsToolKitSettings.getPullRequestComments(currentProject);
    if (!cachedComments.isEmpty()) {
      renderPullRequestComments(cachedComments);
      renderDiffSummaryFromComments(cachedComments);
    } else {
      commentListModel.removeAll();
      commentList.clearSelection();
      commentDetailArea.setText(UiText.SELECT_COMMENT_TO_VIEW.value());
      showDiffPlaceholder(UiText.DIFF_HELPER_FALLBACK.value());
    }

    String savedDraft = DevOpsToolKitSettings.getPullRequestReviewText(currentProject);
    reviewDraftArea.setText(
        savedDraft == null ? UiText.REVIEW_DRAFT_PLACEHOLDER.value() : savedDraft);

    new PullRequestReviewService()
        .fetchPullRequestReviewList(
            currentProject, pullRequestSummary.getId(), this::renderPullRequestReview);
    new PullRequestCommentsService()
        .fetchPullRequestComments(
            currentProject,
            pullRequestSummary.getId(),
            comments -> {
              renderPullRequestComments(comments);
              renderDiffSummaryFromComments(comments);
            });
    maybeLoadDiffContext();
  }

  void reset() {
    selectedPullRequestSummary = null;
    setReviewActionButtonsEnabled(false);
    reviewSummaryLabel.setText(UiText.SELECT_PR_TO_REVIEW.value());
    overviewPanel.removeAll();
    overviewPanel.add(createEmptyMessage(UiText.SELECT_PR_TO_REVIEW.value()), BorderLayout.CENTER);
    reviewDetailsArea.setText(UiText.SELECT_PR_TO_REVIEW.value());
    reviewDraftArea.setText(UiText.REVIEW_DRAFT_PLACEHOLDER.value());
    currentComments = List.of();
    currentFileChanges = List.of();
    currentDiffEntries = List.of();
    diffPatchCache.clear();
    currentPatchFilePath = null;
    requestedDiffPullRequestId = null;
    diffFileListModel.removeAll();
    diffFileList.clearSelection();
    commentListModel.removeAll();
    commentList.clearSelection();
    setDiffDetailText(UiText.SELECT_PR_FOR_DIFF.value(), null);
    commentDetailArea.setText(UiText.SELECT_COMMENT_TO_VIEW.value());
    DevOpsToolKitSettings.setPullRequestComments(currentProject, List.of());
  }

  private JPanel createDetailPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(JBUI.Borders.empty(10));

    JPanel headerPanel = new JPanel(new BorderLayout(0, 10));
    headerPanel.setOpaque(false);
    headerPanel.add(reviewSummaryLabel, BorderLayout.NORTH);
    overviewPanel.setOpaque(false);
    overviewPanel.add(createEmptyMessage(UiText.SELECT_PR_TO_REVIEW.value()), BorderLayout.CENTER);
    headerPanel.add(overviewPanel, BorderLayout.CENTER);
    headerPanel.add(new JSeparator(), BorderLayout.SOUTH);
    panel.add(headerPanel, BorderLayout.NORTH);

    detailTabs.addTab(
        UiText.REVIEW_SUBTAB_TITLE.value(),
        PullRequestPresentation.createSectionPanel(
            UiText.REVIEW_SUBTAB_TITLE.value(),
            UiText.REVIEW_HELPER_TEXT.value(),
            createReviewTab()));
    detailTabs.addTab(
        UiText.DIFF_COMMENTS_SUBTAB_TITLE.value(),
        PullRequestPresentation.createSectionPanel(
            UiText.DIFF_COMMENTS_SUBTAB_TITLE.value(),
            UiText.DIFF_HELPER_TEXT.value(),
            createDiffCommentsTab()));
    detailTabs.addTab(
        UiText.COMMENTS_SUBTAB_TITLE.value(),
        PullRequestPresentation.createSectionPanel(
            UiText.COMMENTS_SUBTAB_TITLE.value(),
            UiText.COMMENTS_HELPER_TEXT.value(),
            createCommentsTab()));
    panel.add(detailTabs, BorderLayout.CENTER);
    return panel;
  }

  private JComponent createReviewTab() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setOpaque(false);

    JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    actionPanel.setOpaque(false);
    actionPanel.add(approveButton);
    actionPanel.add(unapproveButton);
    actionPanel.add(mergeButton);

    panel.add(actionPanel, BorderLayout.NORTH);
    panel.add(new JBScrollPane(reviewDetailsArea), BorderLayout.CENTER);
    return panel;
  }

  private JComponent createDiffCommentsTab() {
    JPanel panel = new JPanel(new BorderLayout(0, 8));
    panel.setOpaque(false);
    JSplitPane splitPane =
        new JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT,
            new JBScrollPane(diffFileList),
            new JBScrollPane(diffSummaryArea));
    splitPane.setResizeWeight(0.4d);
    splitPane.setDividerLocation(300);
    splitPane.setBorder(JBUI.Borders.empty());
    panel.add(splitPane, BorderLayout.CENTER);

    JPanel composerPanel = new JPanel(new BorderLayout(0, 6));
    composerPanel.setOpaque(false);
    composerPanel.add(new JBLabel(UiText.REVIEW_DRAFT_LABEL.value()), BorderLayout.NORTH);
    JBScrollPane draftScrollPane = new JBScrollPane(reviewDraftArea);
    draftScrollPane.setPreferredSize(new Dimension(240, 110));
    composerPanel.add(draftScrollPane, BorderLayout.CENTER);
    panel.add(composerPanel, BorderLayout.SOUTH);
    return panel;
  }

  private JComponent createCommentsTab() {
    JPanel panel = new JPanel(new BorderLayout(0, 8));
    panel.setOpaque(false);
    JSplitPane splitPane =
        new JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT,
            new JBScrollPane(commentList),
            new JBScrollPane(commentDetailArea));
    splitPane.setResizeWeight(0.4d);
    splitPane.setDividerLocation(300);
    splitPane.setBorder(JBUI.Borders.empty());
    panel.add(splitPane, BorderLayout.CENTER);
    return panel;
  }

  private void configureAreas() {
    reviewDetailsArea.setEditable(false);
    reviewDetailsArea.setLineWrap(true);
    reviewDetailsArea.setWrapStyleWord(true);

    diffSummaryArea.setEditable(false);
    diffSummaryArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
    diffFileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    commentList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    commentDetailArea.setEditable(false);
    commentDetailArea.setLineWrap(true);
    commentDetailArea.setWrapStyleWord(true);

    reviewDraftArea.setLineWrap(true);
    reviewDraftArea.setWrapStyleWord(true);
    reviewDraftArea.setText(UiText.REVIEW_DRAFT_PLACEHOLDER.value());
    reviewDraftArea
        .getDocument()
        .addDocumentListener(
            new com.intellij.ui.DocumentAdapter() {
              @Override
              protected void textChanged(@NotNull javax.swing.event.DocumentEvent e) {
                DevOpsToolKitSettings.setPullRequestReviewText(
                    currentProject, reviewDraftArea.getText());
              }
            });
  }

  private void configureDiffList() {
    diffFileList.addListSelectionListener(
        event -> {
          if (event.getValueIsAdjusting()) {
            return;
          }
          int selectedIndex = diffFileList.getSelectedIndex();
          if (selectedIndex < 0 || selectedIndex >= currentDiffEntries.size()) {
            return;
          }
          showDiffEntryDetail(currentDiffEntries.get(selectedIndex));
        });
  }

  private void configureCommentList() {
    commentList.addListSelectionListener(
        event -> {
          if (event.getValueIsAdjusting()) {
            return;
          }
          int selectedIndex = commentList.getSelectedIndex();
          if (selectedIndex < 0 || selectedIndex >= currentComments.size()) {
            return;
          }
          PullRequestCommentSummary comment = currentComments.get(selectedIndex);
          if (comment.getId() == null || comment.getPullRequestId() == null) {
            commentDetailArea.setText(UiText.COMMENT_DETAIL_UNAVAILABLE.value());
            return;
          }
          commentDetailArea.setText(UiText.LOADING_COMMENT_DETAIL.value());
          new PullRequestCommentDetailService()
              .fetchPullRequestComment(
                  currentProject,
                  comment.getPullRequestId(),
                  comment.getId(),
                  this::renderPullRequestCommentDetail);
          syncDiffContextForComment(comment);
        });
    commentList.addMouseListener(
        new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent event) {
            if (event.getButton() != MouseEvent.BUTTON1) {
              return;
            }
            int selectedIndex = commentList.locationToIndex(event.getPoint());
            if (selectedIndex < 0 || selectedIndex >= currentComments.size()) {
              return;
            }
            openCommentFile(currentComments.get(selectedIndex));
          }
        });
  }

  private void configureReviewButtons() {
    approveButton.addActionListener(event -> submitReviewAction(ReviewAction.APPROVE));
    unapproveButton.addActionListener(event -> submitReviewAction(ReviewAction.UNAPPROVE));
    mergeButton.addActionListener(event -> submitMergeAction());
    setReviewActionButtonsEnabled(false);
  }

  private void configureDetailTabs() {
    detailTabs.addChangeListener(event -> maybeLoadDiffContext());
  }

  private void submitMergeAction() {
    if (selectedPullRequestSummary == null || selectedPullRequestSummary.getId() == null) {
      showStatus(currentProject, UiText.SELECT_PR_BEFORE_MERGE_ACTION.value());
      return;
    }
    setReviewActionButtonsEnabled(false);
    new PullRequestMergeService()
        .mergePullRequest(
            currentProject,
            selectedPullRequestSummary.getId(),
            pullRequest -> {
              renderPullRequestReview(pullRequest);
              if (pullRequest == null
                  || pullRequest.getId() == null
                  || pullRequest.getId().isBlank()) {
                setReviewActionButtonsEnabled(true);
              }
            });
  }

  private void submitReviewAction(@NotNull ReviewAction reviewAction) {
    if (selectedPullRequestSummary == null || selectedPullRequestSummary.getId() == null) {
      showStatus(currentProject, UiText.SELECT_PR_BEFORE_REVIEW_ACTION.value());
      return;
    }

    setReviewActionButtonsEnabled(false);
    new PullRequestReviewService()
        .submitReviewAction(
            currentProject,
            selectedPullRequestSummary.getId(),
            reviewAction,
            pullRequest -> {
              renderPullRequestReview(pullRequest);
              if (pullRequest == null
                  || pullRequest.getId() == null
                  || pullRequest.getId().isBlank()) {
                setReviewActionButtonsEnabled(true);
              }
            });
  }

  private void renderPullRequestReview(@Nullable PullRequest pullRequest) {
    if (pullRequest == null || pullRequest.getId() == null || pullRequest.getId().isBlank()) {
      renderPullRequestReviewFallback();
      reviewDetailsArea.setCaretPosition(0);
      return;
    }

    setReviewActionButtonsEnabled(true);
    String displayName =
        RepositoryPanelHandler.displayName(
            pullRequest.getDisplayName(), UiText.UNNAMED_PULL_REQUEST.value());
    reviewSummaryLabel.setText(UiText.REVIEW_HEADER_PREFIX.value() + displayName);
    reviewSummaryLabel.setForeground(JBColor.foreground());

    reviewDetailsArea.setText(PullRequestPresentation.formatPullRequestDetails(pullRequest));
    reviewDetailsArea.setCaretPosition(0);
  }

  private void renderPullRequestReviewFallback() {
    if (selectedPullRequestSummary == null) {
      setReviewActionButtonsEnabled(false);
      reviewSummaryLabel.setText(UiText.REVIEW_UNAVAILABLE_TITLE.value());
      reviewDetailsArea.setText(UiText.REVIEW_UNAVAILABLE_MESSAGE.value());
      return;
    }

    setReviewActionButtonsEnabled(false);
    reviewSummaryLabel.setText(
        UiText.REVIEW_HEADER_PREFIX.value()
            + RepositoryPanelHandler.displayName(
                selectedPullRequestSummary.getDisplayName(), UiText.UNNAMED_PULL_REQUEST.value()));
    reviewSummaryLabel.setForeground(JBColor.foreground());
    reviewDetailsArea.setText(
        PullRequestPresentation.formatPullRequestSummaryDetails(selectedPullRequestSummary));
  }

  private void showOverview(@NotNull PullRequestSummary summary) {
    overviewPanel.removeAll();
    overviewPanel.add(PullRequestPresentation.createOverviewCard(summary), BorderLayout.CENTER);
    overviewPanel.revalidate();
    overviewPanel.repaint();
  }

  private @NotNull Component createEmptyMessage(@NotNull String message) {
    JBLabel label = new JBLabel(message, AllIcons.General.Information, JBLabel.LEFT);
    label.setBorder(
        BorderFactory.createCompoundBorder(
            JBUI.Borders.customLine(JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground()),
            JBUI.Borders.empty(10)));
    label.setForeground(JBUI.CurrentTheme.ContextHelp.FOREGROUND);

    NonOpaquePanel wrapper = new NonOpaquePanel(new BorderLayout());
    wrapper.add(label, BorderLayout.CENTER);
    return wrapper;
  }

  private void renderPullRequestComments(@NotNull List<PullRequestCommentSummary> comments) {
    currentComments = comments;
    if (comments.isEmpty()) {
      commentListModel.removeAll();
      commentList.clearSelection();
      commentDetailArea.setText(UiText.SELECT_COMMENT_TO_VIEW.value());
      DevOpsToolKitSettings.setPullRequestComments(currentProject, List.of());
      renderDiffSummaryFromComments(List.of());
      return;
    }

    DevOpsToolKitSettings.setPullRequestComments(currentProject, comments);
    commentListModel.removeAll();
    commentListModel.add(
        comments.stream().map(PullRequestPresentation::formatPullRequestCommentSummary).toList());
    if (!commentListModel.isEmpty()) {
      commentList.setSelectedIndex(0);
    }
  }

  private void renderPullRequestCommentDetail(@Nullable PullRequestComment comment) {
    if (comment == null || comment.getId() == null || comment.getId().isBlank()) {
      commentDetailArea.setText(UiText.COMMENT_DETAIL_UNAVAILABLE.value());
      return;
    }

    StringBuilder builder = new StringBuilder();
    builder.append(UiText.COMMENT_DETAIL_TITLE.value()).append('\n');
    builder
        .append(UiText.COMMENT_ID_LABEL.value())
        .append(RepositoryPanelHandler.displayName(comment.getId(), UiText.NOT_AVAILABLE.value()))
        .append('\n');
    builder
        .append(UiText.COMMENT_STATUS_LABEL.value())
        .append(
            comment.getStatus() == null ? UiText.NOT_AVAILABLE.value() : comment.getStatus().name())
        .append('\n');
    builder
        .append(UiText.COMMENT_TEXT_LABEL.value())
        .append(
            RepositoryPanelHandler.displayName(comment.getData(), UiText.NOT_AVAILABLE.value()));
    commentDetailArea.setText(builder.toString());
    commentDetailArea.setCaretPosition(0);
  }

  private void renderDiffSummaryFromComments(@NotNull List<PullRequestCommentSummary> comments) {
    currentFileChanges = List.of();
    if (comments.isEmpty()) {
      currentDiffEntries = List.of();
      showDiffPlaceholder(UiText.DIFF_HELPER_FALLBACK.value());
      return;
    }

    Map<String, List<PullRequestCommentSummary>> commentsByPath = new LinkedHashMap<>();
    for (PullRequestCommentSummary comment : comments) {
      String path =
          RepositoryPanelHandler.displayName(comment.getFilePath(), UiText.NOT_AVAILABLE.value());
      commentsByPath.computeIfAbsent(path, ignored -> new ArrayList<>()).add(comment);
    }

    List<DiffEntry> diffEntries = new ArrayList<>();
    for (Map.Entry<String, List<PullRequestCommentSummary>> entry : commentsByPath.entrySet()) {
      String filePath = entry.getKey();
      List<PullRequestCommentSummary> fileComments = entry.getValue();
      diffEntries.add(
          new DiffEntry(
              filePath,
              PullRequestPresentation.formatCommentDerivedDiffRow(filePath, fileComments),
              PullRequestPresentation.formatCommentDerivedDiffSummary(filePath, fileComments),
              filePath));
    }
    updateDiffEntries(diffEntries);
  }

  private void renderPullRequestFileChanges(
      @NotNull List<PullRequestFileChangeSummary> fileChanges) {
    currentFileChanges = fileChanges;
    if (fileChanges.isEmpty()) {
      renderDiffSummaryFromComments(DevOpsToolKitSettings.getPullRequestComments(currentProject));
      return;
    }

    List<DiffEntry> diffEntries = new ArrayList<>();
    for (PullRequestFileChangeSummary fileChange : fileChanges) {
      String anchor = resolveDiffAnchor(fileChange);
      diffEntries.add(
          new DiffEntry(
              anchor,
              PullRequestPresentation.formatPullRequestFileChangeSummary(fileChange),
              PullRequestPresentation.formatPullRequestFileChangeDetail(fileChange),
              resolvePreferredDiffFilePath(fileChange)));
    }
    updateDiffEntries(diffEntries);
  }

  private void maybeLoadDiffContext() {
    if (detailTabs.getSelectedIndex() != DIFF_TAB_INDEX || selectedPullRequestSummary == null) {
      return;
    }
    String pullRequestId = selectedPullRequestSummary.getId();
    if (pullRequestId == null || pullRequestId.isBlank()) {
      return;
    }
    if (pullRequestId.equals(requestedDiffPullRequestId)) {
      return;
    }
    requestedDiffPullRequestId = pullRequestId;
    if (currentDiffEntries.isEmpty()) {
      showDiffPlaceholder(UiText.LOADING_DIFF_DETAILS.value());
    }
    String repositoryId =
        firstNonBlank(
            selectedPullRequestSummary.getRepositoryId(),
            selectedPullRequestSummary.getSourceRepositoryId());
    String targetRepositoryId = selectedPullRequestSummary.getSourceRepositoryId();
    String baseVersion =
        firstNonBlank(
            selectedPullRequestSummary.getMergeBaseCommitIdAtTermination(),
            selectedPullRequestSummary.getDestinationBranch());
    String targetVersion =
        firstNonBlank(
            selectedPullRequestSummary.getSourceCommitIdAtTermination(),
            selectedPullRequestSummary.getSourceBranch());
    new PullRequestFileChangesService()
        .fetchPullRequestFileChanges(
            currentProject,
            pullRequestId,
            repositoryId,
            targetRepositoryId,
            baseVersion,
            targetVersion,
            fileChanges -> {
              PullRequestSummary currentSelection = selectedPullRequestSummary;
              if (currentSelection == null || !pullRequestId.equals(currentSelection.getId())) {
                return;
              }
              renderPullRequestFileChanges(fileChanges);
            });
  }

  private void syncDiffContextForComment(@NotNull PullRequestCommentSummary comment) {
    String anchor = resolveDiffAnchor(comment);
    if (anchor == null || anchor.isBlank()) {
      return;
    }
    for (int index = 0; index < currentDiffEntries.size(); index++) {
      if (anchor.equals(currentDiffEntries.get(index).anchor())) {
        detailTabs.setSelectedIndex(DIFF_TAB_INDEX);
        diffFileList.setSelectedIndex(index);
        diffFileList.ensureIndexIsVisible(index);
        diffFileList.requestFocusInWindow();
        return;
      }
    }
  }

  private @Nullable String resolveDiffAnchor(@NotNull PullRequestCommentSummary comment) {
    String filePath = comment.getFilePath();
    if (filePath == null || filePath.isBlank()) {
      return null;
    }

    for (PullRequestFileChangeSummary fileChange : currentFileChanges) {
      String newPath = fileChange.getNewPath();
      String oldPath = fileChange.getOldPath();
      if (filePath.equals(newPath) || filePath.equals(oldPath)) {
        if (newPath != null && !newPath.isBlank()) {
          if (oldPath != null && !oldPath.isBlank() && !oldPath.equals(newPath)) {
            return oldPath + " -> " + newPath;
          }
          return newPath;
        }
        return oldPath;
      }
    }
    return filePath;
  }

  private void openCommentFile(@NotNull PullRequestCommentSummary comment) {
    String filePath = resolveCommentFilePath(comment);
    Integer lineNumber = comment.getLineNumber();
    if (filePath == null || filePath.isBlank()) {
      showStatus(currentProject, "Comment file path unavailable");
      return;
    }
    openDiffFile(filePath, lineNumber);
  }

  private @Nullable String resolveCommentFilePath(@NotNull PullRequestCommentSummary comment) {
    String filePath = comment.getFilePath();
    if (filePath == null || filePath.isBlank()) {
      return null;
    }

    for (PullRequestFileChangeSummary fileChange : currentFileChanges) {
      String newPath = fileChange.getNewPath();
      String oldPath = fileChange.getOldPath();
      if (filePath.equals(newPath) && newPath != null && !newPath.isBlank()) {
        return newPath;
      }
      if (filePath.equals(oldPath)) {
        if (newPath != null && !newPath.isBlank()) {
          return newPath;
        }
        return oldPath;
      }
    }
    return filePath;
  }

  private @NotNull String resolveDiffAnchor(@NotNull PullRequestFileChangeSummary fileChange) {
    String newPath = fileChange.getNewPath();
    String oldPath = fileChange.getOldPath();
    if (newPath != null && !newPath.isBlank()) {
      if (oldPath != null && !oldPath.isBlank() && !oldPath.equals(newPath)) {
        return oldPath + " -> " + newPath;
      }
      return newPath;
    }
    return RepositoryPanelHandler.displayName(oldPath, UiText.NOT_AVAILABLE.value());
  }

  private void updateDiffEntries(@NotNull List<DiffEntry> diffEntries) {
    currentDiffEntries = List.copyOf(diffEntries);
    diffFileListModel.removeAll();
    if (currentDiffEntries.isEmpty()) {
      showDiffPlaceholder(UiText.DIFF_HELPER_FALLBACK.value());
      return;
    }
    diffFileListModel.add(currentDiffEntries.stream().map(DiffEntry::rowText).toList());
    diffFileList.clearSelection();
    setDiffDetailText(
        "Changed files: "
            + currentDiffEntries.size()
            + "\n\n"
            + UiText.SELECT_DIFF_FILE_TO_VIEW.value(),
        null);
  }

  private void showDiffPlaceholder(@NotNull String message) {
    currentDiffEntries = List.of();
    diffFileListModel.removeAll();
    diffFileList.clearSelection();
    setDiffDetailText(message, null);
  }

  private void showDiffEntryDetail(@NotNull DiffEntry diffEntry) {
    String cachedPatch = diffPatchCache.get(diffEntry.anchor());
    if (cachedPatch != null && !cachedPatch.isBlank()) {
      setDiffDetailText(cachedPatch, diffEntry.filePath());
      return;
    }

    setDiffDetailText(diffEntry.detailText(), null);
    requestRepoFileDiff(diffEntry);
  }

  private void requestRepoFileDiff(@NotNull DiffEntry diffEntry) {
    DiffRequest diffRequest = resolveDiffRequest(diffEntry.filePath());
    if (diffRequest == null) {
      return;
    }

    diffSummaryArea.setText(UiText.LOADING_FILE_DIFF.value());
    new RepoFileDiffService()
        .fetchRepoFileDiff(
            currentProject,
            diffRequest.repositoryId(),
            diffRequest.baseVersion(),
            diffRequest.targetVersion(),
            diffRequest.filePath(),
            diffResponse -> applyRepoFileDiff(diffEntry, diffResponse));
  }

  private void applyRepoFileDiff(
      @NotNull DiffEntry diffEntry,
      @NotNull com.oracle.bmc.devops.model.FileDiffResponse diffResponse) {
    String renderedDiff = PullRequestPresentation.formatRepoFileDiff(diffResponse);
    diffPatchCache.put(diffEntry.anchor(), renderedDiff);
    int selectedIndex = diffFileList.getSelectedIndex();
    if (selectedIndex < 0 || selectedIndex >= currentDiffEntries.size()) {
      return;
    }
    DiffEntry selectedEntry = currentDiffEntries.get(selectedIndex);
    if (selectedEntry.anchor().equals(diffEntry.anchor())) {
      setDiffDetailText(renderedDiff, diffEntry.filePath());
    }
  }

  private @Nullable DiffRequest resolveDiffRequest(@Nullable String filePath) {
    if (selectedPullRequestSummary == null || filePath == null || filePath.isBlank()) {
      return null;
    }

    String repositoryId =
        firstNonBlank(
            selectedPullRequestSummary.getSourceRepositoryId(),
            selectedPullRequestSummary.getRepositoryId());
    String baseVersion =
        firstNonBlank(
            selectedPullRequestSummary.getMergeBaseCommitIdAtTermination(),
            selectedPullRequestSummary.getDestinationBranch());
    String targetVersion =
        firstNonBlank(
            selectedPullRequestSummary.getSourceCommitIdAtTermination(),
            selectedPullRequestSummary.getSourceBranch());
    if (repositoryId == null || baseVersion == null || targetVersion == null) {
      return null;
    }
    return new DiffRequest(repositoryId, baseVersion, targetVersion, filePath);
  }

  private @Nullable String firstNonBlank(String first, String second) {
    if (first != null && !first.isBlank()) {
      return first;
    }
    if (second != null && !second.isBlank()) {
      return second;
    }
    return null;
  }

  private @NotNull String resolvePreferredDiffFilePath(
      @NotNull PullRequestFileChangeSummary fileChange) {
    String newPath = fileChange.getNewPath();
    if (newPath != null && !newPath.isBlank()) {
      return newPath;
    }
    return RepositoryPanelHandler.displayName(
        fileChange.getOldPath(), UiText.NOT_AVAILABLE.value());
  }

  private record DiffRequest(
      String repositoryId, String baseVersion, String targetVersion, String filePath) {}

  private void setDiffDetailText(@NotNull String text, @Nullable String filePath) {
    currentPatchFilePath = filePath;
    if (looksLikePatch(text)) {
      renderStyledPatch(text);
      return;
    }
    diffSummaryArea.setText(text);
    diffSummaryArea.setCaretPosition(0);
  }

  private boolean looksLikePatch(@NotNull String text) {
    return text.startsWith("--- ") || text.contains("\n@@ -");
  }

  private void renderStyledPatch(@NotNull String patchText) {
    StyledDocument document = diffSummaryArea.getStyledDocument();
    diffSummaryArea.setText("");

    SimpleAttributeSet headerStyle =
        createPatchStyle(
            new JBColor(new java.awt.Color(120, 120, 120), new java.awt.Color(145, 145, 145)),
            true);
    SimpleAttributeSet hunkStyle =
        createPatchStyle(
            new JBColor(new java.awt.Color(90, 110, 170), new java.awt.Color(123, 170, 247)), true);
    SimpleAttributeSet addStyle =
        createPatchStyle(
            new JBColor(new java.awt.Color(20, 110, 55), new java.awt.Color(104, 196, 125)), false);
    SimpleAttributeSet removeStyle =
        createPatchStyle(
            new JBColor(new java.awt.Color(160, 50, 50), new java.awt.Color(240, 128, 128)), false);
    SimpleAttributeSet contextStyle = createPatchStyle(JBColor.foreground(), false);
    SimpleAttributeSet metaStyle =
        createPatchStyle(JBUI.CurrentTheme.ContextHelp.FOREGROUND, false);

    int baseLine = 0;
    int targetLine = 0;
    String[] lines = patchText.split("\\R", -1);
    for (int index = 0; index < lines.length; index++) {
      String line = lines[index];
      String renderedLine = line;
      SimpleAttributeSet style = contextStyle;
      String action = null;
      Object payload = null;
      if (line.startsWith("@@ ")) {
        int[] parsedLines = parseHunkHeader(line);
        baseLine = parsedLines[0];
        targetLine = parsedLines[1];
        style = hunkStyle;
        if (currentPatchFilePath != null && !currentPatchFilePath.isBlank() && targetLine > 0) {
          action = PATCH_ACTION_OPEN_HUNK;
          payload = Integer.valueOf(targetLine);
        }
      } else if (line.startsWith("--- ") || line.startsWith("+++ ")) {
        style = headerStyle;
        if (currentPatchFilePath != null && !currentPatchFilePath.isBlank()) {
          action = PATCH_ACTION_OPEN_FILE;
          payload = currentPatchFilePath;
        }
      } else if (line.startsWith("Conflicts:")) {
        style = metaStyle;
      } else if (line.startsWith("+")) {
        renderedLine = formatPatchLine("", targetLine, line);
        targetLine++;
        style = addStyle;
      } else if (line.startsWith("-")) {
        renderedLine = formatPatchLine(baseLine, "", line);
        baseLine++;
        style = removeStyle;
      } else if (!line.isBlank()) {
        renderedLine = formatPatchLine(baseLine, targetLine, line);
        baseLine++;
        targetLine++;
      }
      appendStyledLine(document, renderedLine, style, index < lines.length - 1, action, payload);
    }
    diffSummaryArea.setCaretPosition(0);
  }

  private @NotNull SimpleAttributeSet createPatchStyle(java.awt.Color color, boolean bold) {
    SimpleAttributeSet style = new SimpleAttributeSet();
    StyleConstants.setFontFamily(style, Font.MONOSPACED);
    StyleConstants.setFontSize(style, 12);
    StyleConstants.setForeground(style, color);
    StyleConstants.setBold(style, bold);
    return style;
  }

  private int[] parseHunkHeader(@NotNull String line) {
    int minusIndex = line.indexOf('-');
    int plusIndex = line.indexOf('+');
    int commaAfterMinus = line.indexOf(',', minusIndex);
    int commaAfterPlus = line.indexOf(',', plusIndex);
    int spaceAfterPlus = line.indexOf(' ', plusIndex);
    int parsedBaseLine =
        minusIndex >= 0 && commaAfterMinus > minusIndex
            ? parsePositiveInt(line.substring(minusIndex + 1, commaAfterMinus))
            : 0;
    int parsedTargetLine =
        plusIndex >= 0 && ((commaAfterPlus > plusIndex) || (spaceAfterPlus > plusIndex))
            ? parsePositiveInt(
                line.substring(
                    plusIndex + 1, commaAfterPlus > plusIndex ? commaAfterPlus : spaceAfterPlus))
            : 0;
    return new int[] {parsedBaseLine, parsedTargetLine};
  }

  private int parsePositiveInt(@NotNull String value) {
    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException exception) {
      return 0;
    }
  }

  private @NotNull String formatPatchLine(
      Object baseLine, Object targetLine, @NotNull String line) {
    return String.format("%5s %5s %s", baseLine, targetLine, line);
  }

  private void appendStyledLine(
      @NotNull StyledDocument document,
      @NotNull String line,
      @NotNull SimpleAttributeSet style,
      boolean appendNewLine,
      @Nullable String action,
      @Nullable Object payload) {
    try {
      SimpleAttributeSet styledLine = new SimpleAttributeSet(style);
      if (action != null) {
        styledLine.addAttribute(PATCH_ACTION_KEY, action);
        if (payload != null) {
          styledLine.addAttribute(PATCH_PAYLOAD_KEY, payload);
        }
      }
      document.insertString(document.getLength(), appendNewLine ? line + "\n" : line, styledLine);
    } catch (BadLocationException exception) {
      diffSummaryArea.setText(line);
    }
  }

  private void configureDiffDetailInteractions() {
    diffSummaryArea.addMouseMotionListener(
        new MouseAdapter() {
          @Override
          public void mouseMoved(MouseEvent event) {
            diffSummaryArea.setCursor(resolvePatchCursor(event.getPoint()));
          }
        });
    diffSummaryArea.addMouseListener(
        new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent event) {
            if (event.getButton() != MouseEvent.BUTTON1) {
              return;
            }
            handlePatchClick(event.getPoint());
          }
        });
  }

  private @NotNull Cursor resolvePatchCursor(@NotNull Point point) {
    var element = diffSummaryArea.getStyledDocument().getCharacterElement(viewToModel(point));
    Object action = element.getAttributes().getAttribute(PATCH_ACTION_KEY);
    return action == null
        ? Cursor.getDefaultCursor()
        : Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
  }

  private void handlePatchClick(@NotNull Point point) {
    var element = diffSummaryArea.getStyledDocument().getCharacterElement(viewToModel(point));
    Object action = element.getAttributes().getAttribute(PATCH_ACTION_KEY);
    Object payload = element.getAttributes().getAttribute(PATCH_PAYLOAD_KEY);
    if (!(action instanceof String actionText)) {
      return;
    }
    if (PATCH_ACTION_OPEN_FILE.equals(actionText) && payload instanceof String filePath) {
      openDiffFile(filePath, null);
      return;
    }
    if (PATCH_ACTION_OPEN_HUNK.equals(actionText)
        && payload instanceof Integer lineNumber
        && currentPatchFilePath != null) {
      openDiffFile(currentPatchFilePath, lineNumber);
    }
  }

  private int viewToModel(@NotNull Point point) {
    return diffSummaryArea.viewToModel2D(point);
  }

  private void openDiffFile(@NotNull String filePath, @Nullable Integer lineNumber) {
    String basePath = currentProject.getBasePath();
    if (basePath == null || basePath.isBlank()) {
      showStatus(currentProject, "Project path unavailable");
      return;
    }
    VirtualFile file =
        LocalFileSystem.getInstance()
            .findFileByPath(java.nio.file.Path.of(basePath, filePath).normalize().toString());
    if (file == null) {
      showStatus(currentProject, "File not found in project: " + filePath);
      return;
    }
    OpenFileDescriptor descriptor =
        lineNumber != null && lineNumber > 0
            ? new OpenFileDescriptor(currentProject, file, lineNumber - 1, 0)
            : new OpenFileDescriptor(currentProject, file);
    FileEditorManager.getInstance(currentProject).openTextEditor(descriptor, true);
  }

  private void setReviewActionButtonsEnabled(boolean enabled) {
    approveButton.setEnabled(enabled);
    unapproveButton.setEnabled(enabled);
    mergeButton.setEnabled(enabled);
  }

  private enum UiText {
    SELECT_PR_TO_REVIEW("Select a pull request to inspect review details"),
    SELECT_PR_FOR_DIFF(
        "Select a pull request to inspect changed discussion context and review draft"),
    LOADING_REVIEW_DETAILS("Loading pull request details..."),
    LOADING_DIFF_DETAILS("Loading diff context..."),
    LOADING_FILE_DIFF("Loading file patch..."),
    LOADING_COMMENTS("Loading pull request comments..."),
    REVIEW_HELPER_TEXT("Approve, unapprove, merge, and inspect pull request metadata"),
    DIFF_HELPER_TEXT("Use comments and your persisted draft to prepare polished review feedback"),
    COMMENTS_HELPER_TEXT("Browse discussion threads and pull request comment details"),
    REVIEW_SUBTAB_TITLE("Review"),
    DIFF_COMMENTS_SUBTAB_TITLE("Diff"),
    COMMENTS_SUBTAB_TITLE("Comments"),
    APPROVE_BUTTON_LABEL("Approve"),
    UNAPPROVE_BUTTON_LABEL("Unapprove"),
    MERGE_BUTTON_LABEL("Merge"),
    SELECT_PR_BEFORE_REVIEW_ACTION("Select a pull request before submitting a review action"),
    SELECT_PR_BEFORE_MERGE_ACTION("Select a pull request before submitting merge"),
    REVIEW_UNAVAILABLE_TITLE("Review unavailable"),
    REVIEW_UNAVAILABLE_MESSAGE(
        "Full pull request details could not be loaded with the current OCI access.\n"
            + "Pull request summary, comments, and diff context may still be available."),
    REVIEW_HEADER_PREFIX("Review: "),
    UNNAMED_PULL_REQUEST("Unnamed Pull Request"),
    NOT_AVAILABLE("N/A"),
    COMMENT_ID_LABEL("Comment ID: "),
    COMMENT_FILE_LABEL("File: "),
    COMMENT_LINE_LABEL("Line: "),
    COMMENT_STATUS_LABEL("Status: "),
    REVIEW_DRAFT_LABEL("Review Draft"),
    COMMENT_TEXT_LABEL("Comment: "),
    COMMENT_DETAIL_TITLE("Comment Detail"),
    REVIEW_DRAFT_PLACEHOLDER(
        "Capture your review notes here. This draft is persisted for the project and restored after IDE restart."),
    SELECT_PR_FOR_COMMENTS("Select a pull request to inspect comments"),
    SELECT_COMMENT_TO_VIEW("Comment details will appear here"),
    SELECT_DIFF_FILE_TO_VIEW("Select a changed file to inspect its diff."),
    LOADING_COMMENT_DETAIL("Loading pull request comment detail..."),
    NO_COMMENTS_AVAILABLE("No pull request comments available."),
    COMMENT_DETAIL_UNAVAILABLE("Pull request comment detail unavailable."),
    DIFF_HELPER_FALLBACK(
        "Diff context will be inferred from pull request comments when available.");

    private final String value;

    UiText(String value) {
      this.value = value;
    }

    private String value() {
      return value;
    }
  }
}
