package dasarathi.devops.toolkit.cli;

import static dasarathi.devops.toolkit.core.DevOpsToolKitConstants.IDENTITY_PROVIDER_OCNA_SAML;
import static dasarathi.devops.toolkit.core.DevOpsToolKitConstants.PROFILE_OCI_DEVOPS_SCM;
import static dasarathi.devops.toolkit.core.DevOpsToolKitConstants.REGION_US_PHOENIX_1;
import static dasarathi.devops.toolkit.core.DevOpsToolKitConstants.TENANCY_BMC_OPERATOR_ACCESS;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import dasarathi.devops.toolkit.cli.CliExecutorHandler.OciCliResult;
import java.util.List;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;

public class CliSessionAuthenticate {

  private static final Logger LOG = Logger.getInstance(CliSessionAuthenticate.class);

  public void fetchAuthenticateSession(
      @NotNull Project project, @NotNull Consumer<OciCliResult> onCompletedOciCliResult) {
    ProgressManager.getInstance()
        .run(
            new Task.Backgroundable(project, "Running OCI authenticate session command", true) {
              @Override
              public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                OciCliResult authenticateResult = runAuthenticateSession(project);
                ApplicationManager.getApplication()
                    .invokeLater(() -> onCompletedOciCliResult.accept(authenticateResult));
              }
            });
  }

  public OciCliResult runAuthenticateSession(@NotNull Project project) {
    LOG.info("Executing oci session authenticate session");
    return CliExecutorHandler.execute(
        project, authenticateSessionCommand(), "Failed to execute oci session authenticate");
  }

  static List<String> authenticateSessionCommand() {
    return List.of(
        "oci",
        "session",
        "authenticate",
        "--region",
        REGION_US_PHOENIX_1,
        "--tenancy-name",
        TENANCY_BMC_OPERATOR_ACCESS,
        "--profile-name",
        PROFILE_OCI_DEVOPS_SCM,
        "--identity-provider-name",
        IDENTITY_PROVIDER_OCNA_SAML);
  }

  public OciCliResult runSessionValidation(@NotNull Project project) {
    var commandRegionList =
        List.of(
            "oci",
            "iam",
            "region",
            "list",
            "--profile",
            PROFILE_OCI_DEVOPS_SCM,
            "--auth",
            "security_token");
    LOG.info("Executing session validation command");
    return CliExecutorHandler.execute(
        project, commandRegionList, "Failed to execute oci session validation");
  }

  public void refreshSessionAuthenticate(
      @NotNull Project project, @NotNull Consumer<OciCliResult> onCompletedOciCliResult) {
    ProgressManager.getInstance()
        .run(
            new Task.Backgroundable(project, "Running OCI refresh session command", true) {
              @Override
              public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                OciCliResult authenticateResult = runRefreshSession(project);
                ApplicationManager.getApplication()
                    .invokeLater(() -> onCompletedOciCliResult.accept(authenticateResult));
              }
            });
  }

  public OciCliResult runRefreshSession(@NotNull Project project) {
    var commandRegionList =
        List.of(
            "oci",
            "session",
            "refresh",
            "--profile",
            PROFILE_OCI_DEVOPS_SCM,
            "--auth",
            "security_token");
    LOG.info("Executing session refresh command");
    return CliExecutorHandler.execute(
        project, commandRegionList, "Failed to execute oci session authenticate");
  }

  public OciCliResult runAuthenticateSession() {
    throw new UnsupportedOperationException("Project is required to execute OCI commands");
  }

  public OciCliResult runSessionValidation() {
    throw new UnsupportedOperationException("Project is required to execute OCI commands");
  }

  public OciCliResult runRefreshSession() {
    throw new UnsupportedOperationException("Project is required to execute OCI commands");
  }
}
