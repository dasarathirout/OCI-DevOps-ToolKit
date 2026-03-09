package dasarathi.devops.toolkit.mock;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.util.ui.JBUI;
import java.awt.*;
import javax.swing.*;
import org.jetbrains.annotations.NotNull;

public interface UserInterfaceMocks {
  static void createToolWindowContentMock(
      @NotNull Project project, @NotNull ToolWindow toolWindow) {
    // =============================================================================================
    JBPanel<?> rootPanel = new JBPanel<>(new BorderLayout());
    // =============================================================================================
    String projectName = project.getName();
    JBTabbedPane mainTabbedPane = new JBTabbedPane();
    for (int x = 1; x <= 3; x++) {
      JBTabbedPane subTabbedPane = new JBTabbedPane();
      for (int y = 1; y <= 3; y++) {
        subTabbedPane.addTab(
            "SubTab-" + x + y,
            new JBLabel(
                String.format("Project %s Content For Main=>%d - Sub=>%d ", projectName, x, y)));
      }
      mainTabbedPane.addTab("MainTab-" + x, subTabbedPane);
    }
    rootPanel.add(mainTabbedPane, BorderLayout.CENTER);
    // =============================================================================================
    JPanel footerPanel = new JPanel(new BorderLayout());
    footerPanel.setBorder(JBUI.Borders.empty(5));
    JBLabel infoLabel = new JBLabel("Status => Ready");
    JButton actionButton = new JButton("Session...");
    footerPanel.add(infoLabel, BorderLayout.WEST);
    footerPanel.add(actionButton, BorderLayout.EAST);
    rootPanel.add(footerPanel, BorderLayout.SOUTH);
    // =============================================================================================
    ContentFactory contentFactory = ContentFactory.getInstance();
    Content content = contentFactory.createContent(rootPanel, "Master Container", false);
    toolWindow.getContentManager().addContent(content);
    // =============================================================================================
  }
}
