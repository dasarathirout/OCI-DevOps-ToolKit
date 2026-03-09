package dasarathi.devops.toolkit.gui.repositories;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.ui.components.panels.NonOpaquePanel;
import com.intellij.util.ui.JBUI;
import java.awt.BorderLayout;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import org.jetbrains.annotations.NotNull;

final class RepositoryPanelToolbarFactory {
  private RepositoryPanelToolbarFactory() {}

  static JPanel createHeader(
      @NotNull com.intellij.ui.SearchTextField searchField,
      @NotNull JBLabel searchHintLabel,
      @NotNull JPanel repositoryPanel,
      @NotNull DumbAwareAction refreshAction,
      @NotNull DumbAwareAction resetAction,
      @NotNull
          dasarathi.devops.toolkit.gui.repositories.pullrequest.PullRequestFilterDropDownAction
              userAction,
      @NotNull
          dasarathi.devops.toolkit.gui.repositories.pullrequest.PullRequestFilterDropDownAction
              statusAction,
      @NotNull
          dasarathi.devops.toolkit.gui.repositories.pullrequest.PullRequestFilterDropDownAction
              branchAction,
      @NotNull
          dasarathi.devops.toolkit.gui.repositories.pullrequest.PullRequestFilterDropDownAction
              sortAction,
      @NotNull String toolbarPlace) {
    DefaultActionGroup filterActionGroup = new DefaultActionGroup();
    filterActionGroup.add(refreshAction);
    filterActionGroup.add(resetAction);
    filterActionGroup.addSeparator();
    filterActionGroup.add(userAction);
    filterActionGroup.add(statusAction);
    filterActionGroup.add(branchAction);
    filterActionGroup.add(sortAction);

    ActionToolbar toolbar =
        ActionManager.getInstance().createActionToolbar(toolbarPlace, filterActionGroup, true);
    toolbar.setTargetComponent(repositoryPanel);

    JPanel header = new JPanel();
    header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
    header.setBorder(JBUI.Borders.empty(6, 2, 6, 8));
    header.add(searchField);
    searchHintLabel.setBorder(JBUI.Borders.empty(2, 8));
    header.add(searchHintLabel);
    header.add(Box.createVerticalStrut(6));
    JPanel toolbarContainer = new NonOpaquePanel(new BorderLayout());
    toolbarContainer.setBorder(JBUI.Borders.emptyLeft(0));
    toolbarContainer.add(toolbar.getComponent(), BorderLayout.WEST);
    header.add(toolbarContainer);
    return header;
  }

  static DumbAwareAction createRefreshAction(@NotNull Runnable refreshAction) {
    return new DumbAwareAction("Refresh", "Fetch PRs again", AllIcons.Actions.Refresh) {
      @Override
      public void actionPerformed(
          @NotNull com.intellij.openapi.actionSystem.AnActionEvent actionEvent) {
        refreshAction.run();
      }
    };
  }

  static DumbAwareAction createResetAction(@NotNull Runnable resetAction) {
    return new DumbAwareAction(
        "Reset All Filters", "Clear all applied PR filters", AllIcons.General.Reset) {
      @Override
      public void actionPerformed(
          @NotNull com.intellij.openapi.actionSystem.AnActionEvent actionEvent) {
        resetAction.run();
      }
    };
  }

  static javax.swing.JComponent createEmptyStatePanel(
      @NotNull JBLabel emptyStateLabel,
      @NotNull LinkLabel<Void> refreshLink,
      @NotNull Runnable refreshAction) {
    JPanel container = new JPanel(new java.awt.GridBagLayout());
    JPanel center = new JPanel();
    center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
    emptyStateLabel.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
    refreshLink.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
    refreshLink.setListener((aSource, aLinkData) -> refreshAction.run(), null);
    center.add(emptyStateLabel);
    center.add(Box.createVerticalStrut(6));
    center.add(refreshLink);
    container.add(center);
    return container;
  }
}
