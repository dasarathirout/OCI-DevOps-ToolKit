package dasarathi.devops.toolkit.gui.footer;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.JBUI;
import dasarathi.devops.toolkit.core.DevOpsToolKitPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import org.jetbrains.annotations.NotNull;

public final class FooterPanel implements DevOpsToolKitPanel {
  private static final Logger LOG = Logger.getInstance(FooterPanel.class);
  private static final String SESSION_TIME_UNKNOWN = "Session ? ⚠";
  private final FooterHandler footerHandler;
  private final JButton settingsButton;
  private JButton sessionButton;
  private JPanel footerComponent;
  private final Timer footerRefreshTimer;

  public FooterPanel(@NotNull Project currentProject) {
    this.footerHandler = new FooterHandler(currentProject);
    settingsButton = new JButton();
    footerRefreshTimer = footerHandler.createFooterRefreshTimer(this);
  }

  @Override
  public Component getComponent() {
    if (footerComponent == null) {
      footerComponent = createFooterComponent();
      footerHandler.refreshFooter(this);
      footerHandler.startFooterRefreshTimer(footerRefreshTimer);
    }
    return footerComponent;
  }

  private JPanel createFooterComponent() {
    LOG.info("Init... Footer Component");
    JPanel footerJPanel = new JPanel(new BorderLayout(8, 0));
    footerJPanel.setBorder(JBUI.Borders.empty(6, 10, 6, 6));

    JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
    leftPanel.setOpaque(false);

    JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 0));
    rightPanel.setOpaque(false);

    settingsButton.setText(FooterHandler.cliVersionUnknownText());
    settingsButton.setIcon(AllIcons.General.GearPlain);
    settingsButton.setBorder(JBUI.Borders.empty(2, 4, 2, 0));
    settingsButton.setFocusable(false);
    settingsButton.setToolTipText("OCI CLI Settings");
    settingsButton.setHorizontalAlignment(SwingConstants.RIGHT);
    settingsButton.setHorizontalTextPosition(SwingConstants.LEFT);
    settingsButton.addActionListener(e -> footerHandler.openSettings(this));

    sessionButton = new JButton(SESSION_TIME_UNKNOWN);
    sessionButton.setIcon(AllIcons.Actions.Refresh);
    sessionButton.setBorder(JBUI.Borders.empty(2, 8));
    sessionButton.setFocusable(false);
    sessionButton.setHorizontalAlignment(SwingConstants.LEFT);
    sessionButton.setHorizontalTextPosition(SwingConstants.RIGHT);
    sessionButton.addActionListener(e -> footerHandler.refreshFooter(this));

    leftPanel.add(sessionButton);
    leftPanel.add(Box.createHorizontalStrut(2));

    rightPanel.add(settingsButton);
    rightPanel.add(Box.createHorizontalStrut(1));

    footerJPanel.add(leftPanel, BorderLayout.CENTER);
    footerJPanel.add(rightPanel, BorderLayout.EAST);
    return footerJPanel;
  }

  void updateVersionText(@NotNull String versionText) {
    settingsButton.setText(versionText);
    settingsButton.setToolTipText(versionText + " · OCI CLI Settings");
  }

  void applySessionDisplay(@NotNull String sessionText, boolean activeSession) {
    if (sessionButton != null) {
      sessionButton.setText(sessionText);
      sessionButton.setIcon(footerHandler.resolveSessionButtonIcon(activeSession));
      sessionButton.setToolTipText(sessionText);
    }
  }
}
