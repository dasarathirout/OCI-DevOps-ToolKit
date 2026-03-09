package dasarathi.devops.toolkit.gui.builds;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.Gray;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.panels.NonOpaquePanel;
import com.intellij.ui.components.panels.VerticalLayout;
import com.intellij.ui.components.panels.Wrapper;
import com.intellij.util.ui.JBUI;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import org.jetbrains.annotations.NotNull;

public final class BuildsPanelHandler {
  private static final Logger LOG = Logger.getInstance(BuildsPanelHandler.class);

  private BuildsPanelHandler() {}

  public static Component createPlaceholderPanel(@NotNull String title, @NotNull String message) {
    LOG.info("Builds Placeholder Panel active: " + title + " | " + message);
    JPanel buildsContainerPanel = new JPanel(new GridBagLayout());
    JPanel buildsContentPanel = new JPanel();
    buildsContentPanel.setLayout(new VerticalLayout(JBUI.scale(8)));
    buildsContentPanel.setBorder(
        BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Gray._220, 1, true), JBUI.Borders.empty(20, 28)));
    buildsContentPanel.setOpaque(true);
    buildsContentPanel.setBackground(JBUI.CurrentTheme.ToolWindow.background());
    buildsContentPanel.setPreferredSize(new Dimension(JBUI.scale(320), JBUI.scale(150)));

    JBLabel buildsIconLabel = new JBLabel(AllIcons.General.Information);
    buildsIconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
    buildsIconLabel.setHorizontalAlignment(SwingConstants.CENTER);

    JBLabel buildsTitleLabel = new JBLabel(title);
    buildsTitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
    buildsTitleLabel.setHorizontalAlignment(SwingConstants.CENTER);

    SimpleColoredComponent buildsMessageLabel = new SimpleColoredComponent();
    buildsMessageLabel.setOpaque(false);
    buildsMessageLabel.setIcon(AllIcons.General.BalloonInformation);
    buildsMessageLabel.setIconTextGap(JBUI.scale(6));
    buildsMessageLabel.append(message, SimpleTextAttributes.GRAYED_ATTRIBUTES);

    JPanel buildsMessagePanel = new NonOpaquePanel(new GridBagLayout());
    buildsMessagePanel.add(buildsMessageLabel);

    buildsContentPanel.add(buildsIconLabel);
    buildsContentPanel.add(buildsTitleLabel);
    buildsContentPanel.add(new Wrapper(buildsMessagePanel));
    buildsContainerPanel.add(buildsContentPanel);
    return buildsContainerPanel;
  }

  public static String buildRunTitle() {
    return UIText.BUILD_RUN_TITLE.value();
  }

  public static String listBuildsTitle() {
    return UIText.LIST_BUILDS_TITLE.value();
  }

  public static String placeholderMessage() {
    return UIText.PLACEHOLDER_MESSAGE.value();
  }

  private enum UIText {
    BUILD_RUN_TITLE("Build Run"),
    LIST_BUILDS_TITLE("List Builds"),
    PLACEHOLDER_MESSAGE("Under Development");

    private final String value;

    UIText(String value) {
      this.value = value;
    }

    private String value() {
      return value;
    }
  }
}
