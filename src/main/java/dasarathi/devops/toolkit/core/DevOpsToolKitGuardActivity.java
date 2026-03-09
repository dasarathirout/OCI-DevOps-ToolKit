package dasarathi.devops.toolkit.core;

import static dasarathi.devops.toolkit.DevOpsToolKitToolWindowFactory.DEVOPS_TOOLKIT_ENABLED_KEY;
import static dasarathi.devops.toolkit.DevOpsToolKitToolWindowFactory.DEVOPS_TOOLKIT_PROJECT_NAMESPACE_KEY;
import static dasarathi.devops.toolkit.DevOpsToolKitToolWindowFactory.DEVOPS_TOOLKIT_PROJECT_NAME_KEY;
import static dasarathi.devops.toolkit.DevOpsToolKitToolWindowFactory.DEVOPS_TOOLKIT_REPOSITORY_NAME_KEY;
import static dasarathi.devops.toolkit.core.DevOpsToolKitConstants.DEVOPS_TOOLKIT_WINDOW;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DevOpsToolKitGuardActivity implements ProjectActivity {
  private static final Logger LOG = Logger.getInstance(DevOpsToolKitGuardActivity.class);
  private static final int MAX_PARENT_LOOKUP_DEPTH = 5;
  private static final String UNKNOWN_REPOSITORY = "Unknown Repository Information";
  private static final String REQUIRED_REMOTE =
      "https://oci.private.devops.scmservice.us-phoenix-1.oci.oracleiaas.com";
  private static final Pattern OCI_REMOTE_PATTERN =
      Pattern.compile(
          "https://oci\\.private\\.devops\\.scmservice\\.[^/]+\\.oci\\.oracleiaas\\.com/namespaces/([^/]+)/projects/([^/]+)/repositories/([^/\\s]+)");

  @Nullable
  @Override
  public Object execute(
      @NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
    updateAvailability(project);
    return Unit.INSTANCE;
  }

  public static boolean isTargetGitRepository(Project project) {
    return findGitConfig(project)
        .flatMap(DevOpsToolKitGuardActivity::extractOciRepoInfo)
        .isPresent();
  }

  private static Optional<Path> findGitConfig(Project project) {
    String basePath = project.getBasePath();
    if (basePath == null || basePath.isBlank()) {
      return java.util.Optional.empty();
    }

    Path current = Path.of(basePath).toAbsolutePath();
    for (int i = 0;
        i < MAX_PARENT_LOOKUP_DEPTH && current != null;
        i++, current = current.getParent()) {
      Path gitConfig = current.resolve(".git").resolve("config");
      if (Files.exists(gitConfig)) {
        return java.util.Optional.of(gitConfig);
      }
    }
    return Optional.empty();
  }

  private static Optional<OciRepoInfo> extractOciRepoInfo(Path gitConfig) {
    try {
      String config = Files.readString(gitConfig);
      if (!config.contains(REQUIRED_REMOTE)) {
        return Optional.empty();
      }

      Matcher matcher = OCI_REMOTE_PATTERN.matcher(config);
      if (!matcher.find()) {
        return Optional.empty();
      }

      return Optional.of(new OciRepoInfo(matcher.group(1), matcher.group(2), matcher.group(3)));
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  private void updateAvailability(Project project) {
    Optional<OciRepoInfo> repoInfo =
        findGitConfig(project).flatMap(DevOpsToolKitGuardActivity::extractOciRepoInfo);
    boolean isEnabled = repoInfo.isPresent();

    project.putUserData(DEVOPS_TOOLKIT_ENABLED_KEY, isEnabled);
    project.putUserData(
        DEVOPS_TOOLKIT_PROJECT_NAMESPACE_KEY,
        repoInfo.map(OciRepoInfo::namespace).orElse(UNKNOWN_REPOSITORY));
    project.putUserData(
        DEVOPS_TOOLKIT_PROJECT_NAME_KEY,
        repoInfo.map(OciRepoInfo::projectName).orElse(UNKNOWN_REPOSITORY));
    project.putUserData(
        DEVOPS_TOOLKIT_REPOSITORY_NAME_KEY,
        repoInfo.map(OciRepoInfo::repositoryName).orElse(UNKNOWN_REPOSITORY));

    setToolWindowAvailability(project, isEnabled);
    logAvailability(isEnabled);
  }

  private void setToolWindowAvailability(Project project, boolean enabled) {
    ToolWindow toolWindow =
        ToolWindowManager.getInstance(project).getToolWindow(DEVOPS_TOOLKIT_WINDOW);
    if (toolWindow != null) toolWindow.setAvailable(enabled, null);
  }

  private void logAvailability(boolean enabled) {
    if (enabled) {
      LOG.info("OCI DevOps Repository detected. Enabling DevOpsToolKit plugin features...");
    } else {
      LOG.info("External repo detected. DevOpsToolKit plugin features disabled.");
    }
  }

  private record OciRepoInfo(String namespace, String projectName, String repositoryName) {}
}
