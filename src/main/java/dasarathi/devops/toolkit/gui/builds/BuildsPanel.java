package dasarathi.devops.toolkit.gui.builds;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.oracle.bmc.devops.model.BuildRun;
import com.oracle.bmc.devops.model.BuildRunSummary;
import dasarathi.devops.toolkit.core.DevOpsToolKitPanel;
import dasarathi.devops.toolkit.gui.builds.service.BuildRunService;
import java.awt.*;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import org.jetbrains.annotations.NotNull;

public final class BuildsPanel implements DevOpsToolKitPanel {
  private static final Logger LOG = Logger.getInstance(BuildsPanel.class);
  final Project currentProject;
  private final CollectionListModel<String> buildRunModel = new CollectionListModel<>();
  private final JBList<String> buildRunList = new JBList<>(buildRunModel);
  private final JTextArea buildRunDetailArea = new JTextArea();
  private List<BuildRunSummary> currentBuildRuns = List.of();

  public BuildsPanel(@NotNull Project currentProject) {
    this.currentProject = currentProject;
  }

  @Override
  public Component getComponent() {
    return getBuildComponent();
  }

  private Component getBuildComponent() {
    LOG.info("Init... Build Component");
    JBTabbedPane buildTabbedPane = new JBTabbedPane();
    buildTabbedPane.addTab(BuildsPanelHandler.buildRunTitle(), createBuildRunsPanel());
    buildTabbedPane.addTab(
        BuildsPanelHandler.listBuildsTitle(),
        BuildsPanelHandler.createPlaceholderPanel(
            BuildsPanelHandler.listBuildsTitle(), BuildsPanelHandler.placeholderMessage()));
    return buildTabbedPane;
  }

  private Component createBuildRunsPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    buildRunDetailArea.setEditable(false);
    buildRunDetailArea.setText("Loading build runs...");
    buildRunList.addListSelectionListener(
        event -> {
          if (event.getValueIsAdjusting()) {
            return;
          }
          int index = buildRunList.getSelectedIndex();
          if (index < 0 || index >= currentBuildRuns.size()) {
            return;
          }
          BuildRunSummary summary = currentBuildRuns.get(index);
          new BuildRunService()
              .fetchBuildRun(currentProject, summary.getId(), this::renderBuildRun);
        });
    JSplitPane splitPane =
        new JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT,
            new JBScrollPane(buildRunList),
            new JBScrollPane(buildRunDetailArea));
    splitPane.setResizeWeight(0.35d);
    panel.add(splitPane, BorderLayout.CENTER);
    new BuildRunService().fetchBuildRuns(currentProject, this::renderBuildRuns);
    return panel;
  }

  private void renderBuildRuns(@NotNull List<BuildRunSummary> buildRuns) {
    currentBuildRuns = buildRuns;
    buildRunModel.removeAll();
    if (buildRuns.isEmpty()) {
      buildRunDetailArea.setText("No build runs available.");
      return;
    }
    buildRunModel.add(
        buildRuns.stream()
            .map(
                summary ->
                    summary.getDisplayName() == null
                        ? "Unnamed Build Run"
                        : summary.getDisplayName())
            .toList());
    buildRunList.setSelectedIndex(0);
  }

  private void renderBuildRun(@NotNull BuildRun buildRun) {
    StringBuilder builder = new StringBuilder();
    builder
        .append("Build Run ID: ")
        .append(buildRun.getId() == null ? "N/A" : buildRun.getId())
        .append('\n');
    builder
        .append("Display Name: ")
        .append(buildRun.getDisplayName() == null ? "N/A" : buildRun.getDisplayName())
        .append('\n');
    builder
        .append("Status: ")
        .append(buildRun.getLifecycleState() == null ? "N/A" : buildRun.getLifecycleState().name())
        .append('\n');
    builder
        .append("Details: ")
        .append(buildRun.getLifecycleDetails() == null ? "N/A" : buildRun.getLifecycleDetails());
    buildRunDetailArea.setText(builder.toString());
    buildRunDetailArea.setCaretPosition(0);
  }
}
