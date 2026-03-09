package dasarathi.devops.toolkit.exception;

import static dasarathi.devops.toolkit.core.DevOpsToolKitConstants.DEVOPS_PLUGIN_ID;

import com.intellij.diagnostic.PluginException;
import com.intellij.openapi.extensions.PluginId;

public class DevOpsPluginException extends PluginException {
  private static final PluginId DEV_OPS_PLUGIN_ID = PluginId.getId(DEVOPS_PLUGIN_ID);
  private static final String DEFAULT_PLUGIN_ERROR_MESSAGE =
      "From OCI DevOps Tool Kit Plugin Exception Occurred.";

  public DevOpsPluginException() {
    super(DEFAULT_PLUGIN_ERROR_MESSAGE, DEV_OPS_PLUGIN_ID);
  }

  public DevOpsPluginException(final String errorMessage) {
    super(errorMessage, DEV_OPS_PLUGIN_ID);
  }

  public DevOpsPluginException(final String errorMessage, final Throwable throwable) {
    super(errorMessage, throwable, DEV_OPS_PLUGIN_ID);
  }

  public DevOpsPluginException(final Throwable throwable) {
    super(DEFAULT_PLUGIN_ERROR_MESSAGE, throwable, DEV_OPS_PLUGIN_ID);
  }
}
