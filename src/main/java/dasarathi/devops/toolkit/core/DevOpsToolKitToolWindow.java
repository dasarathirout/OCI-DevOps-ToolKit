package dasarathi.devops.toolkit.core;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTabbedPane;
import dasarathi.devops.toolkit.gui.builds.BuildsPanel;
import dasarathi.devops.toolkit.gui.deployments.DeploymentPanel;
import dasarathi.devops.toolkit.gui.footer.FooterPanel;
import dasarathi.devops.toolkit.gui.repositories.RepositoryPanel;
import java.awt.*;
import org.jetbrains.annotations.NotNull;

public class DevOpsToolKitToolWindow extends JBPanel<DevOpsToolKitToolWindow>
    implements DevOpsToolKitPanel {
  private static final Logger LOG = Logger.getInstance(DevOpsToolKitToolWindow.class);
  private final transient Project currentProject;
  private final transient ToolWindow currentToolWindow;
  private final transient DevOpsToolKitPanel footerPanel;
  private final transient DevOpsToolKitPanel buildsPanel;
  private final transient DevOpsToolKitPanel deploymentPanel;
  private final transient DevOpsToolKitPanel repositoryPanel;

  public DevOpsToolKitToolWindow(
      @NotNull Project currentProject, @NotNull ToolWindow currentToolWindow) {
    this.currentProject = currentProject;
    this.currentToolWindow = currentToolWindow;
    this.footerPanel = new FooterPanel(currentProject);
    this.buildsPanel = new BuildsPanel(currentProject);
    this.deploymentPanel = new DeploymentPanel(currentProject);
    this.repositoryPanel = new RepositoryPanel(currentProject);
  }

  @Override
  public Component getComponent() {
    return getDevOpsToolKitComponent();
  }

  private Component getDevOpsToolKitComponent() {
    LOG.info(
        String.format(
            "Creating ToolWindowPanel %s for project %s",
            currentToolWindow.getStripeTitle(), currentProject.getName()));
    JBTabbedPane devOpsToolKitTabbedPane = new JBTabbedPane();
    devOpsToolKitTabbedPane.addTab("Repository", repositoryPanel.getComponent());
    devOpsToolKitTabbedPane.addTab("Build", buildsPanel.getComponent());
    devOpsToolKitTabbedPane.addTab("Deployment", deploymentPanel.getComponent());
    return devOpsToolKitTabbedPane;
  }

  public JBPanel<DevOpsToolKitToolWindow> getDevOpsToolKitRootComponent() {
    JBPanel<DevOpsToolKitToolWindow> devOpsToolKitRootPanel = new JBPanel<>(new BorderLayout());
    devOpsToolKitRootPanel.add(getDevOpsToolKitComponent(), BorderLayout.CENTER);
    devOpsToolKitRootPanel.add(footerPanel.getComponent(), BorderLayout.SOUTH);
    return devOpsToolKitRootPanel;
  }
}
