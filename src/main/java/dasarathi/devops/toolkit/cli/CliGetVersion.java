package dasarathi.devops.toolkit.cli;

import static dasarathi.devops.toolkit.DevOpsToolKitToolWindowFactory.DEVOPS_TOOLKIT_REPOSITORY_OCI_CLIV_KEY;

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

public class CliGetVersion {
  /**
   * Fetch OCI CLI version asynchronously and deliver the output to the provided consumer on the
   * EDT.
   */
  private static final Logger LOG = Logger.getInstance(CliGetVersion.class);

  public void fetchVersion(@NotNull Project project, @NotNull Consumer<OciCliResult> onCompleted) {
    ProgressManager.getInstance()
        .run(
            new Task.Backgroundable(project, "Running OCI version command", true) {
              @Override
              public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                OciCliResult result = runOciVersionCommand(project);
                ApplicationManager.getApplication().invokeLater(() -> onCompleted.accept(result));
              }
            });
  }

  /** Execute `oci --version` and return stdout/stderr text wrapped in {@link OciCliResult}. */
  public OciCliResult runOciVersionCommand(@NotNull Project project) {
    LOG.info("runOciVersionCommand");
    OciCliResult ociCliResult =
        CliExecutorHandler.execute(
            project, List.of("oci", "--version"), "Failed to execute oci --version");
    if (ociCliResult.success()) {
      String version = ociCliResult.output().isEmpty() ? null : ociCliResult.output().getFirst();
      project.putUserData(DEVOPS_TOOLKIT_REPOSITORY_OCI_CLIV_KEY, version);
      return ociCliResult;
    }
    if (ociCliResult.errors().isEmpty()) {
      String message =
          ociCliResult.exitCode() == -1 ? "OCI-CLI Not Found" : "OCI-CLI Command Failed";
      return new OciCliResult(
          false, ociCliResult.exitCode(), ociCliResult.output(), List.of(message));
    }
    return ociCliResult;
  }
}
