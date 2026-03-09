package dasarathi.devops.toolkit.gui.deployments;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.oracle.bmc.devops.model.DeployEnvironment;
import com.oracle.bmc.devops.model.DeployEnvironmentSummary;
import com.oracle.bmc.devops.model.DeployPipeline;
import com.oracle.bmc.devops.model.Deployment;
import com.oracle.bmc.devops.model.DeploymentSummary;
import dasarathi.devops.toolkit.core.DevOpsToolKitPanel;
import dasarathi.devops.toolkit.gui.deployments.service.DeployEnvironmentService;
import dasarathi.devops.toolkit.gui.deployments.service.DeployPipelineService;
import dasarathi.devops.toolkit.gui.deployments.service.DeploymentDetailService;
import dasarathi.devops.toolkit.gui.deployments.service.DeploymentService;
import java.awt.*;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import org.jetbrains.annotations.NotNull;

public final class DeploymentPanel implements DevOpsToolKitPanel {
  private static final Logger LOG = Logger.getInstance(DeploymentPanel.class);
  final Project currentProject;
  private final CollectionListModel<String> deploymentModel = new CollectionListModel<>();
  private final JBList<String> deploymentList = new JBList<>(deploymentModel);
  private final JTextArea deploymentDetailArea = new JTextArea();
  private final CollectionListModel<String> environmentModel = new CollectionListModel<>();
  private final JBList<String> environmentList = new JBList<>(environmentModel);
  private final JTextArea environmentDetailArea = new JTextArea();
  private final JTextArea pipelineDetailArea = new JTextArea();
  private List<DeploymentSummary> currentDeployments = List.of();
  private List<DeployEnvironmentSummary> currentEnvironments = List.of();

  public DeploymentPanel(@NotNull Project currentProject) {
    this.currentProject = currentProject;
  }

  @Override
  public Component getComponent() {
    return getDeploymentComponent();
  }

  private Component getDeploymentComponent() {
    LOG.info("Init... Deployment Component");
    JBTabbedPane deploymentTabbedPane = new JBTabbedPane();
    deploymentTabbedPane.addTab(
        DeploymentPanelHandler.deploymentsTitle(), createDeploymentListPanel());
    deploymentTabbedPane.addTab(
        DeploymentPanelHandler.deployPipelinesTitle(), createDeployEnvironmentPanel());
    return deploymentTabbedPane;
  }

  private Component createDeploymentListPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    deploymentDetailArea.setEditable(false);
    deploymentDetailArea.setText("Loading deployments...");
    deploymentList.addListSelectionListener(
        event -> {
          if (event.getValueIsAdjusting()) {
            return;
          }
          int index = deploymentList.getSelectedIndex();
          if (index < 0 || index >= currentDeployments.size()) {
            return;
          }
          DeploymentSummary summary = currentDeployments.get(index);
          new DeploymentDetailService()
              .fetchDeployment(currentProject, summary.getId(), this::renderDeploymentDetail);
        });
    JSplitPane splitPane =
        new JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT,
            new JBScrollPane(deploymentList),
            new JBScrollPane(deploymentDetailArea));
    splitPane.setResizeWeight(0.35d);
    panel.add(splitPane, BorderLayout.CENTER);
    new DeploymentService().fetchDeployments(currentProject, this::renderDeployments);
    return panel;
  }

  private Component createDeployEnvironmentPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    environmentDetailArea.setEditable(false);
    pipelineDetailArea.setEditable(false);
    environmentDetailArea.setText("Loading deploy environments...");
    pipelineDetailArea.setText("Deploy pipeline details will appear here.");
    environmentList.addListSelectionListener(
        event -> {
          if (event.getValueIsAdjusting()) {
            return;
          }
          int index = environmentList.getSelectedIndex();
          if (index < 0 || index >= currentEnvironments.size()) {
            return;
          }
          DeployEnvironmentSummary summary = currentEnvironments.get(index);
          new DeployEnvironmentService()
              .fetchDeployEnvironment(
                  currentProject, summary.getId(), this::renderEnvironmentDetail);
        });
    JSplitPane rightSplitPane =
        new JSplitPane(
            JSplitPane.VERTICAL_SPLIT,
            new JBScrollPane(environmentDetailArea),
            new JBScrollPane(pipelineDetailArea));
    rightSplitPane.setResizeWeight(0.5d);

    JSplitPane mainSplitPane =
        new JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT, new JBScrollPane(environmentList), rightSplitPane);
    mainSplitPane.setResizeWeight(0.35d);
    panel.add(mainSplitPane, BorderLayout.CENTER);
    new DeployEnvironmentService()
        .fetchDeployEnvironments(currentProject, this::renderEnvironments);
    return panel;
  }

  private void renderDeployments(@NotNull List<DeploymentSummary> deployments) {
    currentDeployments = deployments;
    deploymentModel.removeAll();
    if (deployments.isEmpty()) {
      deploymentDetailArea.setText("No deployments available.");
      return;
    }
    deploymentModel.add(
        deployments.stream()
            .map(
                summary ->
                    summary.getDisplayName() == null
                        ? "Unnamed Deployment"
                        : summary.getDisplayName())
            .toList());
    deploymentList.setSelectedIndex(0);
  }

  private void renderEnvironments(@NotNull List<DeployEnvironmentSummary> environments) {
    currentEnvironments = environments;
    environmentModel.removeAll();
    if (environments.isEmpty()) {
      environmentDetailArea.setText("No deploy environments available.");
      pipelineDetailArea.setText("No deploy pipeline available.");
      return;
    }
    environmentModel.add(
        environments.stream()
            .map(
                summary ->
                    summary.getDisplayName() == null
                        ? "Unnamed Environment"
                        : summary.getDisplayName())
            .toList());
    environmentList.setSelectedIndex(0);
  }

  private void renderDeploymentDetail(@NotNull Deployment deployment) {
    if (deployment == null) {
      deploymentDetailArea.setText("Deployment detail unavailable.");
      pipelineDetailArea.setText("Deploy pipeline unavailable.");
      return;
    }
    StringBuilder builder = new StringBuilder();
    builder
        .append("Deployment ID: ")
        .append(deployment.getId() == null ? "N/A" : deployment.getId())
        .append('\n');
    builder
        .append("Display Name: ")
        .append(deployment.getDisplayName() == null ? "N/A" : deployment.getDisplayName())
        .append('\n');
    builder
        .append("Status: ")
        .append(
            deployment.getLifecycleState() == null ? "N/A" : deployment.getLifecycleState().name())
        .append('\n');
    builder
        .append("Details: ")
        .append(
            deployment.getLifecycleDetails() == null ? "N/A" : deployment.getLifecycleDetails());
    deploymentDetailArea.setText(builder.toString());
    deploymentDetailArea.setCaretPosition(0);

    if (deployment.getDeployPipelineId() != null && !deployment.getDeployPipelineId().isBlank()) {
      new DeployPipelineService()
          .fetchDeployPipeline(
              currentProject, deployment.getDeployPipelineId(), this::renderPipelineDetail);
    } else {
      pipelineDetailArea.setText("Deploy pipeline unavailable.");
    }
  }

  private void renderEnvironmentDetail(@NotNull DeployEnvironment deployEnvironment) {
    if (deployEnvironment == null) {
      environmentDetailArea.setText("Deploy environment detail unavailable.");
      return;
    }
    StringBuilder builder = new StringBuilder();
    builder
        .append("Environment ID: ")
        .append(deployEnvironment.getId() == null ? "N/A" : deployEnvironment.getId())
        .append('\n');
    builder
        .append("Display Name: ")
        .append(
            deployEnvironment.getDisplayName() == null ? "N/A" : deployEnvironment.getDisplayName())
        .append('\n');
    builder
        .append("Status: ")
        .append(
            deployEnvironment.getLifecycleState() == null
                ? "N/A"
                : deployEnvironment.getLifecycleState().name())
        .append('\n');
    builder
        .append("Description: ")
        .append(
            deployEnvironment.getDescription() == null
                ? "N/A"
                : deployEnvironment.getDescription());
    environmentDetailArea.setText(builder.toString());
    environmentDetailArea.setCaretPosition(0);
  }

  private void renderPipelineDetail(@NotNull DeployPipeline deployPipeline) {
    StringBuilder builder = new StringBuilder();
    builder
        .append("Pipeline ID: ")
        .append(deployPipeline.getId() == null ? "N/A" : deployPipeline.getId())
        .append('\n');
    builder
        .append("Display Name: ")
        .append(deployPipeline.getDisplayName() == null ? "N/A" : deployPipeline.getDisplayName())
        .append('\n');
    builder
        .append("Status: ")
        .append(
            deployPipeline.getLifecycleState() == null
                ? "N/A"
                : deployPipeline.getLifecycleState().name())
        .append('\n');
    builder
        .append("Description: ")
        .append(deployPipeline.getDescription() == null ? "N/A" : deployPipeline.getDescription());
    pipelineDetailArea.setText(builder.toString());
    pipelineDetailArea.setCaretPosition(0);
  }
}
