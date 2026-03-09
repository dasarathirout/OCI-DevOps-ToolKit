package dasarathi.devops.toolkit.gui.icons;

import static dasarathi.devops.toolkit.core.DevOpsToolKitConstants.SUPPRESS_ALL_WARNING;

import com.intellij.openapi.util.IconLoader;
import javax.swing.Icon;

@SuppressWarnings({SUPPRESS_ALL_WARNING})
public interface DevOpsToolKitIcons {
  // IconLoader automatically handles the "_dark" suffix switch
  Icon DEVOPS_TOOL_WINDOW = IconLoader.getIcon("/icons/toolWindow.svg", DevOpsToolKitIcons.class);
}
