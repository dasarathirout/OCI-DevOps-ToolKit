package dasarathi.devops.toolkit.gui.repositories;

import static dasarathi.devops.toolkit.DevOpsToolKitToolWindowFactory.DEVOPS_TOOLKIT_INITIAL_LOAD_DONE_KEY;
import static dasarathi.devops.toolkit.DevOpsToolKitToolWindowFactory.DEVOPS_TOOLKIT_PROJECT_NAME_KEY;
import static dasarathi.devops.toolkit.DevOpsToolKitToolWindowFactory.DEVOPS_TOOLKIT_PROJECT_SUMMARY_KEY;
import static dasarathi.devops.toolkit.DevOpsToolKitToolWindowFactory.DEVOPS_TOOLKIT_PULL_REQUEST_SUMMARY_KEY;
import static dasarathi.devops.toolkit.DevOpsToolKitToolWindowFactory.DEVOPS_TOOLKIT_REPOSITORY_NAME_KEY;
import static dasarathi.devops.toolkit.DevOpsToolKitToolWindowFactory.DEVOPS_TOOLKIT_REPOSITORY_SUMMARY_KEY;
import static dasarathi.devops.toolkit.event.CustomEventNotification.notifyError;
import static dasarathi.devops.toolkit.event.CustomEventNotification.showStatus;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.SearchTextField;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.util.ui.JBUI;
import com.oracle.bmc.devops.model.ProjectSummary;
import com.oracle.bmc.devops.model.PullRequestSummary;
import com.oracle.bmc.devops.model.RepositorySummary;
import dasarathi.devops.toolkit.core.DevOpsToolKitPanel;
import dasarathi.devops.toolkit.core.DevOpsToolKitSettings;
import dasarathi.devops.toolkit.gui.repositories.pullrequest.PullRequestFilterDropDownAction;
import dasarathi.devops.toolkit.gui.repositories.pullrequest.PullRequestSummaryService;
import dasarathi.devops.toolkit.gui.repositories.service.ProjectSummaryService;
import dasarathi.devops.toolkit.gui.repositories.service.RepositorySummaryService;
import java.awt.BorderLayout;
import java.awt.Component;
import java.util.List;
import java.util.Objects;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import org.jetbrains.annotations.NotNull;

public final class RepositoryPanel implements DevOpsToolKitPanel {
  private static final Logger LOG = Logger.getInstance(RepositoryPanel.class);
  private static final String SHOW_ALL = "All";
  private static final String DEFAULT_STATUS = "OPENED";

  private final Project currentProject;
  private final CollectionListModel<String> pullRequestModelList = new CollectionListModel<>();
  private final JBList<String> pullRequestJBList = new JBList<>(pullRequestModelList);
  private final SearchTextField searchField = new SearchTextField();
  private final JBLabel searchHintLabel = new JBLabel(UIText.SEARCH_SHORTCUT_HINT.value());
  private final JPanel contentPanel = new JPanel(new java.awt.CardLayout());
  private final JBScrollPane listScrollPane = new JBScrollPane(pullRequestJBList);
  private final JBLabel emptyStateLabel = new JBLabel(UIText.NO_PULL_REQUESTS_FOUND.value());
  private final LinkLabel<Void> refreshLink = new LinkLabel<>("Refresh", null);
  private final PullRequestDetailsPanel pullRequestDetailsPanel;

  private final String selectedProjectName;
  private String selectedRepositoryName = UIText.NO_REPOSITORY.value();
  private String selectedUser = SHOW_ALL;
  private String selectedStatus = DEFAULT_STATUS;
  private String selectedBranch = SHOW_ALL;
  private SortOption selectedSort = SortOption.NEWEST;
  private PullRequestSummary selectedPullRequestSummary;
  private Component repositoryComponent;

  public RepositoryPanel(@NotNull Project currentProject) {
    this.currentProject = currentProject;
    this.pullRequestDetailsPanel = new PullRequestDetailsPanel(currentProject);
    String projectNameFromGit = currentProject.getUserData(DEVOPS_TOOLKIT_PROJECT_NAME_KEY);
    this.selectedProjectName =
        (projectNameFromGit == null || projectNameFromGit.isBlank())
            ? currentProject.getName()
            : projectNameFromGit;
  }

  @Override
  public Component getComponent() {
    if (repositoryComponent == null) {
      repositoryComponent = createRepositoryComponent();
    }
    return repositoryComponent;
  }

  private Component createRepositoryComponent() {
    LOG.info("Init... Repository Component");
    SwingUtilities.invokeLater(() -> searchField.getTextEditor().requestFocusInWindow());
    emptyStateLabel.setText(UIText.CLICK_REFRESH_TO_LOAD_PRS.value());
    showCard(CardType.EMPTY);
    loadInitialDataOnce();
    return createPullRequestTabPanel();
  }

  private Component createPullRequestTabPanel() {
    JPanel pullRequestTabPanel = new JPanel(new BorderLayout());
    addPullRequestFilter(pullRequestTabPanel);
    return pullRequestTabPanel;
  }

  private void loadInitialDataOnce() {
    if (Boolean.TRUE.equals(currentProject.getUserData(DEVOPS_TOOLKIT_INITIAL_LOAD_DONE_KEY))) {
      return;
    }

    currentProject.putUserData(DEVOPS_TOOLKIT_INITIAL_LOAD_DONE_KEY, true);
    List<ProjectSummary> cachedProjects = RepositoryPanelHandler.loadProjects(currentProject);
    List<RepositorySummary> cachedRepositories =
        RepositoryPanelHandler.loadRepositories(currentProject);
    if (!cachedProjects.isEmpty()) {
      currentProject.putUserData(DEVOPS_TOOLKIT_PROJECT_SUMMARY_KEY, List.copyOf(cachedProjects));
      DevOpsToolKitSettings.setProjectSummaries(currentProject, cachedProjects);
      if (!cachedRepositories.isEmpty()) {
        currentProject.putUserData(
            DEVOPS_TOOLKIT_REPOSITORY_SUMMARY_KEY, List.copyOf(cachedRepositories));
        DevOpsToolKitSettings.setRepositorySummaries(currentProject, cachedRepositories);
        selectedRepositoryName = resolveInitialRepositoryName(cachedRepositories);
      }

      List<PullRequestSummary> cachedPullRequests =
          RepositoryPanelHandler.loadPullRequests(currentProject);
      if (!cachedPullRequests.isEmpty()) {
        currentProject.putUserData(
            DEVOPS_TOOLKIT_PULL_REQUEST_SUMMARY_KEY, List.copyOf(cachedPullRequests));
        DevOpsToolKitSettings.setPullRequestSummaries(currentProject, cachedPullRequests);
        updatePullRequestModelList(
            RepositoryPanelHandler.pullRequestDisplayNames(cachedPullRequests));
      } else {
        emptyStateLabel.setText(UIText.CLICK_REFRESH_TO_LOAD_PRS.value());
        showCard(CardType.EMPTY);
      }
      return;
    }
    emptyStateLabel.setText(UIText.CLICK_REFRESH_TO_LOAD_PRS.value());
    showCard(CardType.EMPTY);
  }

  private String resolveInitialRepositoryName(
      @NotNull List<RepositorySummary> repositorySummaries) {
    String repositoryNameFromGit = currentProject.getUserData(DEVOPS_TOOLKIT_REPOSITORY_NAME_KEY);
    if (repositoryNameFromGit != null
        && !repositoryNameFromGit.isBlank()
        && repositorySummaries.stream()
            .map(RepositorySummary::getName)
            .filter(Objects::nonNull)
            .anyMatch(repositoryNameFromGit::equals)) {
      return repositoryNameFromGit;
    }
    List<String> repositoryOptions = getRepositoryOptions();
    return repositoryOptions.isEmpty()
        ? UIText.NO_REPOSITORY.value()
        : repositoryOptions.getFirst();
  }

  private void clearPullRequestsAndShow(@NotNull String message) {
    currentProject.putUserData(DEVOPS_TOOLKIT_PULL_REQUEST_SUMMARY_KEY, List.of());
    DevOpsToolKitSettings.setPullRequestSummaries(currentProject, List.of());
    pullRequestModelList.removeAll();
    emptyStateLabel.setText(message);
    showCard(CardType.EMPTY);
  }

  private boolean hasActiveFilters() {
    return RepositoryPanelHandler.hasSelection(SHOW_ALL, selectedUser)
        || !DEFAULT_STATUS.equals(selectedStatus)
        || RepositoryPanelHandler.hasSelection(SHOW_ALL, selectedBranch)
        || selectedSort != SortOption.NEWEST
        || (searchField.getText() != null && !searchField.getText().isBlank());
  }

  private void updateFilterSelection(@NotNull FilterType filterType, @NotNull String selection) {
    switch (filterType) {
      case USER -> selectedUser = selection;
      case STATUS -> selectedStatus = selection;
      case BRANCH -> selectedBranch = selection;
      case SORT -> selectedSort = SortOption.fromDisplayName(selection);
    }
    refreshPullRequestSummaryList();
  }

  private void resetAllFilters() {
    selectedUser = SHOW_ALL;
    selectedStatus = DEFAULT_STATUS;
    selectedBranch = SHOW_ALL;
    selectedSort = SortOption.NEWEST;
    searchField.setText("");
    refreshPullRequestSummaryList();
  }

  private PullRequestFilterDropDownAction createFilterAction(
      @NotNull String title,
      @NotNull String defaultValue,
      @NotNull java.util.function.Supplier<List<String>> optionsSupplier,
      @NotNull FilterType filterType) {
    return new PullRequestFilterDropDownAction(
        title,
        defaultValue,
        optionsSupplier,
        selection -> updateFilterSelection(filterType, selection));
  }

  private void fetchProjectsAndRepositoriesTask() {
    showLoadingState();
    new ProjectSummaryService().fetchProjectSummaryList(currentProject, this::applyLoadedProjects);
  }

  private void refreshPullRequestsOnlyTask() {
    fetchAndRenderRepositoryPullRequests();
  }

  private void applyLoadedProjects(@NotNull List<ProjectSummary> projectSummaries) {
    currentProject.putUserData(DEVOPS_TOOLKIT_PROJECT_SUMMARY_KEY, List.copyOf(projectSummaries));
    String projectId =
        RepositoryPanelHandler.resolveProjectId(projectSummaries, selectedProjectName);
    if (projectId.isBlank()) {
      clearPullRequestsAndShow(UIText.NO_ACTIVE_REPOSITORY_FOUND.value());
      return;
    }

    new RepositorySummaryService()
        .fetchRepositorySummaryList(currentProject, projectId, this::applyLoadedRepositories);
  }

  private void applyLoadedRepositories(@NotNull List<RepositorySummary> loadedRepositories) {
    currentProject.putUserData(
        DEVOPS_TOOLKIT_REPOSITORY_SUMMARY_KEY, List.copyOf(loadedRepositories));
    selectedRepositoryName = resolveInitialRepositoryName(loadedRepositories);
    fetchAndRenderRepositoryPullRequests();
  }

  private void fetchAndRenderRepositoryPullRequests() {
    if (RepositoryPanelHandler.loadRepositories(currentProject).isEmpty()) {
      fetchProjectsAndRepositoriesTask();
      return;
    }
    String repositoryId =
        RepositoryPanelHandler.resolveRepositoryId(
            RepositoryPanelHandler.loadRepositories(currentProject), selectedRepositoryName);
    if (repositoryId.isBlank()) {
      clearPullRequestsAndShow(UIText.NO_ACTIVE_REPOSITORY_FOUND.value());
      return;
    }
    showLoadingState();
    new PullRequestSummaryService()
        .fetchPullRequestSummaryList(
            currentProject,
            repositoryId,
            response -> updatePullRequestModelList(mapPullRequestRows(response)));
  }

  private void handleRepositoryLoadError(@NotNull Exception exception) {
    LOG.error("Failed loading projects/repositories", exception);
    ApplicationManager.getApplication()
        .invokeLater(
            () -> {
              showStatus(currentProject, UIText.ERROR_LOADING_REPOSITORIES.value());
              notifyError(currentProject, "Unable to load repositories: " + exception.getMessage());
              clearPullRequestsAndShow(UIText.NO_PULL_REQUESTS_FOUND.value());
            });
  }

  private List<String> mapPullRequestRows(@NotNull List<PullRequestSummary> pullRequestSummaries) {
    if (pullRequestSummaries.isEmpty()) {
      return List.of();
    }
    return RepositoryPanelHandler.pullRequestDisplayNames(filteredPullRequestSummaryList());
  }

  private PullRequestFilterDropDownAction createUserFilterAction() {
    return createFilterAction("User", SHOW_ALL, this::getUserFilterOptions, FilterType.USER);
  }

  private PullRequestFilterDropDownAction createStatusFilterAction() {
    return createFilterAction(
        "Status", DEFAULT_STATUS, this::getStatusFilterOptions, FilterType.STATUS);
  }

  private PullRequestFilterDropDownAction createBranchFilterAction() {
    return createFilterAction("Branch", SHOW_ALL, this::getBranchFilterOptions, FilterType.BRANCH);
  }

  private PullRequestFilterDropDownAction createSortFilterAction() {
    return createFilterAction(
        "Sort", SortOption.NEWEST.displayName(), this::getSortOptions, FilterType.SORT);
  }

  private List<String> getRepositoryOptions() {
    return RepositoryPanelHandler.loadRepositories(currentProject).stream()
        .map(RepositorySummary::getName)
        .filter(Objects::nonNull)
        .toList();
  }

  private DumbAwareAction refreshPullRequestAgainAction() {
    return new DumbAwareAction("Refresh", "Fetch PRs again", AllIcons.Actions.Refresh) {
      @Override
      public void actionPerformed(@NotNull com.intellij.openapi.actionSystem.AnActionEvent e) {
        if (RepositoryPanelHandler.loadProjects(currentProject).isEmpty()
            || RepositoryPanelHandler.loadRepositories(currentProject).isEmpty()) {
          fetchProjectsAndRepositoriesTask();
          return;
        }
        refreshPullRequestsOnlyTask();
      }
    };
  }

  private DumbAwareAction resetAllFiltersAction() {
    return new DumbAwareAction(
        "Reset All Filters", "Clear all applied PR filters", AllIcons.General.Reset) {
      @Override
      public void actionPerformed(@NotNull com.intellij.openapi.actionSystem.AnActionEvent e) {
        resetAllFilters();
      }
    };
  }

  public void addPullRequestFilter(@NotNull JPanel repositoryPanel) {
    configurePullRequestList();
    searchField.setBorder(JBUI.Borders.empty(4, 8));
    searchField.setToolTipText("Type to filter pull requests. Shortcut: /");
    searchField.getTextEditor().getAccessibleContext().setAccessibleName("Pull request search");
    searchField
        .getTextEditor()
        .getAccessibleContext()
        .setAccessibleDescription("Filter pull requests by summary text");
    searchField
        .getTextEditor()
        .getDocument()
        .addDocumentListener(
            new DocumentAdapter() {
              @Override
              protected void textChanged(@NotNull DocumentEvent e) {
                refreshPullRequestSummaryList();
              }
            });
    JPanel header =
        RepositoryPanelToolbarFactory.createHeader(
            searchField,
            searchHintLabel,
            repositoryPanel,
            refreshPullRequestAgainAction(),
            resetAllFiltersAction(),
            createUserFilterAction(),
            createStatusFilterAction(),
            createBranchFilterAction(),
            createSortFilterAction(),
            UIText.PR_TOOLBAR_PLACE.value());

    contentPanel.add(createPullRequestSplitPane(), CardType.LIST.name());
    contentPanel.add(
        RepositoryPanelToolbarFactory.createEmptyStatePanel(
            emptyStateLabel, refreshLink, this::fetchAndRenderRepositoryPullRequests),
        CardType.EMPTY.name());

    repositoryPanel.setLayout(new BorderLayout());
    repositoryPanel.registerKeyboardAction(
        e -> searchField.getTextEditor().requestFocusInWindow(),
        KeyStroke.getKeyStroke('/'),
        JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    repositoryPanel.add(header, BorderLayout.NORTH);
    repositoryPanel.add(contentPanel, BorderLayout.CENTER);
  }

  private void refreshPullRequestSummaryList() {
    List<PullRequestSummary> filtered = filteredPullRequestSummaryList();
    updatePullRequestModelList(RepositoryPanelHandler.pullRequestDisplayNames(filtered));
  }

  private List<PullRequestSummary> filteredPullRequestSummaryList() {
    return applyFiltersPullRequestSummary(
        RepositoryPanelHandler.loadPullRequests(currentProject),
        selectedUser,
        selectedStatus,
        selectedBranch,
        selectedSort,
        searchField.getText());
  }

  private void updatePullRequestModelList(@NotNull List<String> rows) {
    pullRequestModelList.removeAll();
    if (rows.isEmpty()) {
      selectedPullRequestSummary = null;
      pullRequestDetailsPanel.reset();
      showEmptyState();
      return;
    }
    pullRequestModelList.add(rows);
    showCard(CardType.LIST);
    if (!pullRequestModelList.isEmpty()) {
      pullRequestJBList.setSelectedIndex(0);
    }
  }

  private void configurePullRequestList() {
    pullRequestJBList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    pullRequestJBList.addListSelectionListener(
        event -> {
          if (event.getValueIsAdjusting()) {
            return;
          }
          String selectedDisplayName = pullRequestJBList.getSelectedValue();
          selectedPullRequestSummary = findPullRequestSummaryByDisplayName(selectedDisplayName);
          pullRequestDetailsPanel.displayPullRequest(selectedPullRequestSummary);
        });
  }

  private JComponent createPullRequestSplitPane() {
    JSplitPane splitPane =
        new JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT, listScrollPane, pullRequestDetailsPanel.getComponent());
    splitPane.setResizeWeight(0.35d);
    splitPane.setDividerLocation(280);
    splitPane.setBorder(JBUI.Borders.empty());
    return splitPane;
  }

  private PullRequestSummary findPullRequestSummaryByDisplayName(String displayName) {
    if (displayName == null || displayName.isBlank()) {
      return null;
    }
    return filteredPullRequestSummaryList().stream()
        .filter(summary -> displayName.equals(summary.getDisplayName()))
        .findFirst()
        .orElse(null);
  }

  private List<PullRequestSummary> applyFiltersPullRequestSummary(
      @NotNull List<PullRequestSummary> source,
      @NotNull String selectedUser,
      @NotNull String selectedStatus,
      @NotNull String selectedBranch,
      @NotNull SortOption selectedSort,
      String query) {
    return RepositoryPanelHandler.applyFilters(
        source,
        SHOW_ALL,
        selectedUser,
        SHOW_ALL,
        selectedStatus,
        SHOW_ALL,
        selectedBranch,
        SortOption.OLDEST.displayName(),
        selectedSort.displayName(),
        UIText.UNKNOWN_USER.value(),
        UIText.UNKNOWN_BRANCH.value(),
        query);
  }

  private List<String> getUserFilterOptions() {
    return RepositoryPanelHandler.userFilterOptions(
        RepositoryPanelHandler.loadPullRequests(currentProject),
        SHOW_ALL,
        UIText.UNKNOWN_USER.value());
  }

  private List<String> getStatusFilterOptions() {
    return List.of(DEFAULT_STATUS, "CLOSED", "MERGED", SHOW_ALL);
  }

  private List<String> getBranchFilterOptions() {
    return RepositoryPanelHandler.branchFilterOptions(
        RepositoryPanelHandler.loadPullRequests(currentProject),
        SHOW_ALL,
        UIText.UNKNOWN_BRANCH.value());
  }

  private List<String> getSortOptions() {
    return SortOption.displayNames();
  }

  private JComponent createEmptyStatePanel() {
    JPanel container = new JPanel(new java.awt.GridBagLayout());
    JPanel center = new JPanel();
    center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
    emptyStateLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
    refreshLink.setAlignmentX(Component.CENTER_ALIGNMENT);
    refreshLink.setListener((aSource, aLinkData) -> fetchAndRenderRepositoryPullRequests(), null);
    center.add(emptyStateLabel);
    center.add(Box.createVerticalStrut(6));
    center.add(refreshLink);
    container.add(center);
    return container;
  }

  private void showEmptyState() {
    emptyStateLabel.setText(
        hasActiveFilters()
            ? UIText.NO_PULL_REQUESTS_MATCHING_FILTERS.value()
            : UIText.NO_PULL_REQUESTS_FOUND.value());
    showCard(CardType.EMPTY);
  }

  private void showLoadingState() {
    emptyStateLabel.setText(UIText.LOADING_PULL_REQUESTS.value());
    showCard(CardType.EMPTY);
  }

  private void showCard(@NotNull CardType card) {
    java.awt.CardLayout layout = (java.awt.CardLayout) contentPanel.getLayout();
    layout.show(contentPanel, card.name());
  }

  private enum FilterType {
    USER,
    STATUS,
    BRANCH,
    SORT
  }

  private enum CardType {
    LIST,
    EMPTY
  }

  private enum SortOption {
    NEWEST("Newest"),
    OLDEST("Oldest");

    private final String displayName;

    SortOption(String displayName) {
      this.displayName = displayName;
    }

    private String displayName() {
      return displayName;
    }

    private static SortOption fromDisplayName(@NotNull String displayName) {
      for (SortOption value : values()) {
        if (value.displayName.equals(displayName)) {
          return value;
        }
      }
      return NEWEST;
    }

    private static List<String> displayNames() {
      return List.of(NEWEST.displayName, OLDEST.displayName);
    }
  }

  private enum UIText {
    NO_PULL_REQUESTS_FOUND("No Pull Requests"),
    LOADING_PULL_REQUESTS("Loading Pull Requests..."),
    SEARCH_SHORTCUT_HINT("Tip: Press '/' to Focus Search"),
    NO_PULL_REQUESTS_MATCHING_FILTERS("No PullRequests Matching Filters"),
    CLICK_REFRESH_TO_LOAD_PRS("Click Refresh to load pull requests from SCM repository"),
    NO_ACTIVE_REPOSITORY_FOUND("No Active Repository found"),
    UNKNOWN_USER("Unknown User"),
    UNKNOWN_BRANCH("Unknown Branch"),
    UNNAMED_PULL_REQUEST("Unnamed Pull Request"),
    NOT_AVAILABLE("N/A"),
    NO_DESCRIPTION("No description available"),
    NO_REPOSITORY("No Repository"),
    PULL_REQUEST_TAB_TITLE("Pull Requests"),
    PR_TOOLBAR_PLACE("PR"),
    LOADING_PROJECTS_AND_REPOSITORIES("Loading DevOps project & its repositories"),
    ERROR_LOADING_REPOSITORIES("Error loading repositories"),
    NO_DIFF_CHANGES("No file changes available for this pull request.");

    private final String value;

    UIText(String value) {
      this.value = value;
    }

    private String value() {
      return value;
    }
  }
}
