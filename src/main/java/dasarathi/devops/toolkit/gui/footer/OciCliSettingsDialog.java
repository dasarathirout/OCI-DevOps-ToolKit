package dasarathi.devops.toolkit.gui.footer;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextBrowseFolderListener;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.FormBuilder;
import dasarathi.devops.toolkit.core.DevOpsToolKitSettings;
import java.awt.BorderLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.jspecify.annotations.NonNull;

final class OciCliSettingsDialog extends DialogWrapper {
  private final Project project;
  private final TextFieldWithBrowseButton cliPathField;

  OciCliSettingsDialog(Project project) {
    super(project);
    this.project = project;
    this.cliPathField = new TextFieldWithBrowseButton();
    setTitle("OCI CLI Settings");
    FileChooserFactory.getInstance()
        .installFileCompletion(
            cliPathField.getTextField(),
            FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor(),
            true,
            null);
    cliPathField.addBrowseFolderListener(
        new TextBrowseFolderListener(
            FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor(), project));
    cliPathField.setText(DevOpsToolKitSettings.getOciCliPath(project));
    init();
  }

  @Override
  protected @NonNull JComponent createCenterPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.add(
        FormBuilder.createFormBuilder()
            .addLabeledComponent("OCI CLI Path", cliPathField)
            .addComponentFillVertically(
                new JBLabel("Leave empty to auto-resolve OCI CLI from PATH and whereis."), 0)
            .getPanel(),
        BorderLayout.CENTER);
    return panel;
  }

  @Override
  protected void doOKAction() {
    DevOpsToolKitSettings.setOciCliPath(project, cliPathField.getText());
    super.doOKAction();
  }
}
