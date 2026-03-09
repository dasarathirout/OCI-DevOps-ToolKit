package dasarathi.devops.toolkit;

import static dasarathi.devops.toolkit.core.DevOpsToolKitGuardActivity.isTargetGitRepository;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.oracle.bmc.devops.model.ProjectSummary;
import com.oracle.bmc.devops.model.PullRequest;
import com.oracle.bmc.devops.model.PullRequestFileChangeSummary;
import com.oracle.bmc.devops.model.PullRequestSummary;
import com.oracle.bmc.devops.model.RepositorySummary;
import dasarathi.devops.toolkit.core.DevOpsToolKitToolWindow;
import dasarathi.devops.toolkit.gui.builds.BuildsPanel;
import dasarathi.devops.toolkit.gui.deployments.DeploymentPanel;
import dasarathi.devops.toolkit.gui.footer.FooterPanel;
import dasarathi.devops.toolkit.gui.repositories.RepositoryPanel;
import dasarathi.devops.toolkit.mock.UserInterfaceMocks;
import java.awt.*;
import java.util.List;
import javax.swing.*;
import org.jetbrains.annotations.NotNull;

public final class DevOpsToolKitToolWindowFactory implements ToolWindowFactory, DumbAware {
  public static final Key<Boolean> DEVOPS_TOOLKIT_ENABLED_KEY =
      Key.create("dasarathi.devops.toolkit.enabled");
  public static final Key<String> DEVOPS_TOOLKIT_PROJECT_NAMESPACE_KEY =
      Key.create("dasarathi.devops.toolkit.projectNameSpace");
  public static final Key<String> DEVOPS_TOOLKIT_PROJECT_NAME_KEY =
      Key.create("dasarathi.devops.toolkit.projectName");
  public static final Key<String> DEVOPS_TOOLKIT_REPOSITORY_NAME_KEY =
      Key.create("dasarathi.devops.toolkit.repositoryName");
  public static final Key<String> DEVOPS_TOOLKIT_REPOSITORY_OCI_CLI_KEY =
      Key.create("dasarathi.devops.toolkit.ociCliLocation");
  public static final Key<String> DEVOPS_TOOLKIT_REPOSITORY_OCI_CLIV_KEY =
      Key.create("dasarathi.devops.toolkit.ociCliVersion");
  public static final Key<Boolean> DEVOPS_TOOLKIT_INITIAL_LOAD_DONE_KEY =
      Key.create("dasarathi.devops.toolkit.initialLoadDone");
  public static final Key<List<ProjectSummary>> DEVOPS_TOOLKIT_PROJECT_SUMMARY_KEY =
      Key.create("dasarathi.devops.toolkit.projectSummary");
  public static final Key<List<RepositorySummary>> DEVOPS_TOOLKIT_REPOSITORY_SUMMARY_KEY =
      Key.create("dasarathi.devops.toolkit.repositorySummary");
  public static final Key<List<PullRequestSummary>> DEVOPS_TOOLKIT_PULL_REQUEST_SUMMARY_KEY =
      Key.create("dasarathi.devops.toolkit.pullRequestSummary");
  public static final Key<List<PullRequestFileChangeSummary>>
      DEVOPS_TOOLKIT_PULL_REQUEST_FILE_CHANGES_KEY =
          Key.create("dasarathi.devops.toolkit.pullRequestFileChanges");
  public static final Key<PullRequest> DEVOPS_TOOLKIT_PULL_REQUEST_KEY =
      Key.create("dasarathi.devops.toolkit.pullRequest");

  @Override
  public boolean shouldBeAvailable(@NotNull Project project) {
    return isTargetGitRepository(project);
  }

  /**
   * This is the main entry point for the tool window. It is called when the user clicks the tool
   * window button for the first time.
   */
  @Override
  public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
    toolWindowContents(project, toolWindow);
  }

  /*
   * For |Header|ProjectName|
   *     |TAB1|TAB2|TAB3|
   *     |FOOTER|
   * */
  public void toolWindowContents(
      @NotNull Project currentProject, @NotNull ToolWindow currentToolWindow) {
    ContentFactory contentFactory = ContentFactory.getInstance();
    JBPanel<?> pluginPanel =
        new DevOpsToolKitToolWindow(currentProject, currentToolWindow)
            .getDevOpsToolKitRootComponent();
    Content content = contentFactory.createContent(pluginPanel, currentProject.getName(), false);
    currentToolWindow.getContentManager().addContent(content);
  }

  /*
   * For Main MultiTabbed Contents
   * => |Project Header|TAB1|TAB2|TAB3
   * */
  public void multiTabbedContents(
      @NotNull Project currentProject, @NotNull ToolWindow currentToolWindow) {
    ContentManager contentManager = currentToolWindow.getContentManager();
    contentManager.addContent(
        createContentTab("Repository", new RepositoryPanel(currentProject).getComponent()));
    contentManager.addContent(
        createContentTab("Builds", new BuildsPanel(currentProject).getComponent()));
    contentManager.addContent(
        createContentTab("Deployments", new DeploymentPanel(currentProject).getComponent()));
    contentManager.addContent(
        createContentTab("Footer", new FooterPanel(currentProject).getComponent()));
  }

  private Content createContentTab(String displayName, Component component) {
    ContentFactory contentFactory = ContentFactory.getInstance();
    return contentFactory.createContent((JComponent) component, displayName, false);
  }

  /*
   * For Mock Dev Testing
   * */

  public void toolWindowMockTest(@NotNull Project project, @NotNull ToolWindow toolWindow) {
    UserInterfaceMocks.createToolWindowContentMock(project, toolWindow);
  }
}
