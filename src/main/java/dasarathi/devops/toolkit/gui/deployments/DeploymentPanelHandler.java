package dasarathi.devops.toolkit.gui.deployments;

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

public final class DeploymentPanelHandler {
  private static final Logger LOG = Logger.getInstance(DeploymentPanelHandler.class);

  private DeploymentPanelHandler() {}

  public static Component createPlaceholderPanel(@NotNull String title, @NotNull String message) {

    LOG.info("Deployments Placeholder Panel active: " + title + " | " + message);

    JPanel deploymentContainerPanel = new JPanel(new GridBagLayout());
    JPanel deploymentContentPanel = new JPanel();
    deploymentContentPanel.setLayout(new VerticalLayout(JBUI.scale(8)));
    deploymentContentPanel.setBorder(
        BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Gray._220, 1, true), JBUI.Borders.empty(20, 28)));
    deploymentContentPanel.setOpaque(true);
    deploymentContentPanel.setBackground(JBUI.CurrentTheme.ToolWindow.background());
    deploymentContentPanel.setPreferredSize(new Dimension(JBUI.scale(320), JBUI.scale(150)));

    JBLabel deploymentIconLabel = new JBLabel(AllIcons.General.Information);
    deploymentIconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
    deploymentIconLabel.setHorizontalAlignment(SwingConstants.CENTER);

    JBLabel deploymentTitleLabel = new JBLabel(title);
    deploymentTitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
    deploymentTitleLabel.setHorizontalAlignment(SwingConstants.CENTER);

    SimpleColoredComponent deploymentMessageLabel = new SimpleColoredComponent();
    deploymentMessageLabel.setOpaque(false);
    deploymentMessageLabel.setIcon(AllIcons.General.BalloonInformation);
    deploymentMessageLabel.setIconTextGap(JBUI.scale(6));
    deploymentMessageLabel.append(message, SimpleTextAttributes.GRAYED_ATTRIBUTES);

    JPanel deploymentMessagePanel = new NonOpaquePanel(new GridBagLayout());
    deploymentMessagePanel.add(deploymentMessageLabel);

    deploymentContentPanel.add(deploymentIconLabel);
    deploymentContentPanel.add(deploymentTitleLabel);
    deploymentContentPanel.add(new Wrapper(deploymentMessagePanel));
    deploymentContainerPanel.add(deploymentContentPanel);
    return deploymentContainerPanel;
  }

  public static String deploymentsTitle() {
    return UIText.DEPLOYMENTS_TITLE.value();
  }

  public static String deployPipelinesTitle() {
    return UIText.DEPLOY_PIPELINES_TITLE.value();
  }

  public static String placeholderMessage() {
    return UIText.PLACEHOLDER_MESSAGE.value();
  }

  private enum UIText {
    DEPLOYMENTS_TITLE("Deployments"),
    DEPLOY_PIPELINES_TITLE("Deploy Pipelines"),
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
