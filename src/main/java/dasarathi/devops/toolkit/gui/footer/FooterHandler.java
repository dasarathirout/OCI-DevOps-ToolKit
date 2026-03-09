package dasarathi.devops.toolkit.gui.footer;

import static dasarathi.devops.toolkit.DevOpsToolKitToolWindowFactory.DEVOPS_TOOLKIT_REPOSITORY_OCI_CLIV_KEY;
import static dasarathi.devops.toolkit.DevOpsToolKitToolWindowFactory.DEVOPS_TOOLKIT_REPOSITORY_OCI_CLI_KEY;
import static dasarathi.devops.toolkit.event.CustomEventNotification.notifyError;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import dasarathi.devops.toolkit.cli.CliExecutorHandler.OciCliResult;
import dasarathi.devops.toolkit.cli.CliGetVersion;
import dasarathi.devops.toolkit.cli.CliSessionAuthenticate;
import dasarathi.devops.toolkit.core.DevOpsToolKitUtil;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.Icon;
import javax.swing.Timer;
import org.jetbrains.annotations.NotNull;

public final class FooterHandler {
  private static final Logger LOG = Logger.getInstance(FooterHandler.class);
  private static final String FOOTER_MSG_TIK = "✓";
  private static final String CLI_VERSION_UNKNOWN = "CLI v? ⚠";
  private static final String SESSION_TIME_UNKNOWN = "Session ? ⚠";
  private static final String SESSION_EXPIRED = "Session Expired ⚠";
  private static final String SESSION_ACTIVE = "Session Active ✓";
  private static final int FOOTER_REFRESH_INTERVAL_MILLIS = 30_000;
  private static final long SESSION_REFRESH_THRESHOLD_MILLIS = 5 * 60 * 1000L;

  private final Project currentProject;
  private final CliGetVersion versionService;
  private final CliSessionAuthenticate sessionAuthenticate;
  private final AtomicBoolean sessionCommandInProgress;

  public FooterHandler(@NotNull Project currentProject) {
    this.currentProject = currentProject;
    this.versionService = new CliGetVersion();
    this.sessionAuthenticate = new CliSessionAuthenticate();
    this.sessionCommandInProgress = new AtomicBoolean(false);
  }

  static String cliVersionUnknownText() {
    return CLI_VERSION_UNKNOWN;
  }

  public Timer createFooterRefreshTimer(@NotNull FooterPanel footerPanel) {
    Timer timer =
        new Timer(
            FOOTER_REFRESH_INTERVAL_MILLIS, e -> updateSessionStateFromConfig(footerPanel, false));
    timer.setRepeats(true);
    return timer;
  }

  public void refreshFooter(@NotNull FooterPanel footerPanel) {
    updateSessionStateFromConfig(footerPanel, false);
    String cachedVersion = currentProject.getUserData(DEVOPS_TOOLKIT_REPOSITORY_OCI_CLIV_KEY);
    if (cachedVersion != null && !cachedVersion.isBlank()) {
      footerPanel.updateVersionText("CLI v" + cachedVersion.trim() + " " + FOOTER_MSG_TIK);
      return;
    }
    versionService.fetchVersion(
        currentProject, cliResult -> updateFooterInfos(footerPanel, cliResult));
  }

  public void openSettings(@NotNull FooterPanel footerPanel) {
    OciCliSettingsDialog dialog = new OciCliSettingsDialog(currentProject);
    if (!dialog.showAndGet()) {
      return;
    }

    currentProject.putUserData(DEVOPS_TOOLKIT_REPOSITORY_OCI_CLI_KEY, null);
    currentProject.putUserData(DEVOPS_TOOLKIT_REPOSITORY_OCI_CLIV_KEY, null);
    refreshFooter(footerPanel);
  }

  public void startFooterRefreshTimer(@NotNull Timer timer) {
    if (!timer.isRunning()) {
      timer.start();
    }
  }

  private void updateFooterInfos(
      @NotNull FooterPanel footerPanel, @NotNull OciCliResult cliResult) {
    runOnApplicationUIThread(
        () -> {
          try {
            LOG.info("Update Footer Info" + cliResult);
            footerPanel.updateVersionText(formatVersion(cliResult));
            updateSessionStateFromConfig(footerPanel, true);
          } catch (Exception ex) {
            LOG.warn("Failed to refresh footer info", ex);
            footerPanel.updateVersionText(CLI_VERSION_UNKNOWN);
            notifyError(currentProject, ex.getMessage());
          }
        });
  }

  private void updateSessionStateFromConfig(
      @NotNull FooterPanel footerPanel, boolean autoRefreshSession) {
    try {
      Date dateNow = new Date();
      Date expirationDate = DevOpsToolKitUtil.sessionTokenExpirationDate();
      boolean activeSession = expirationDate != null && expirationDate.after(dateNow);
      if (activeSession) {
        if (shouldRefreshSession(expirationDate, dateNow)) {
          refreshSecurityTokenSession(footerPanel);
          return;
        }
        footerPanel.applySessionDisplay(formatSessionInfo(expirationDate, dateNow), true);
      } else {
        footerPanel.applySessionDisplay(SESSION_EXPIRED, false);
        if (autoRefreshSession) {
          refreshSessionAuthentication(footerPanel);
        }
      }
    } catch (Exception ex) {
      if (shouldCreateNewSession(ex)) {
        LOG.info("Session profile/token missing. Creating a new OCI session.", ex);
        footerPanel.applySessionDisplay(SESSION_EXPIRED, false);
        refreshSessionAuthentication(footerPanel);
        return;
      }
      LOG.warn("Failed to update footer timer", ex);
      notifyError(currentProject, ex.getMessage());
      footerPanel.applySessionDisplay(SESSION_TIME_UNKNOWN, false);
    }
  }

  private String formatSessionInfo(@NotNull Date expirationDate, @NotNull Date dateNow) {
    long remainingMillis = expirationDate.getTime() - dateNow.getTime();
    String sessionInfo = Math.max(0, remainingMillis / 60000) + "m";
    return "Remaining: " + sessionInfo + " " + FOOTER_MSG_TIK;
  }

  private boolean shouldRefreshSession(@NotNull Date expirationDate, @NotNull Date dateNow) {
    long remainingMillis = expirationDate.getTime() - dateNow.getTime();
    return remainingMillis <= SESSION_REFRESH_THRESHOLD_MILLIS;
  }

  private String formatVersion(@NotNull OciCliResult cliResult) {
    List<String> output = cliResult.output();
    if (output == null || output.isEmpty()) {
      return CLI_VERSION_UNKNOWN;
    }
    return "CLI v" + output.getFirst().trim() + " " + FOOTER_MSG_TIK;
  }

  private void refreshSessionAuthentication(@NotNull FooterPanel footerPanel) {
    if (!sessionCommandInProgress.compareAndSet(false, true)) {
      LOG.info("OCI session authentication already in progress");
      return;
    }
    try {
      sessionAuthenticate.fetchAuthenticateSession(
          currentProject, cliResult -> updateSessionAuthentication(footerPanel, cliResult));
    } catch (Exception ex) {
      sessionCommandInProgress.set(false);
      notifyError(currentProject, ex.getMessage());
      LOG.warn("Failed to refresh Session Authentication", ex);
    }
  }

  private void refreshSecurityTokenSession(@NotNull FooterPanel footerPanel) {
    if (!sessionCommandInProgress.compareAndSet(false, true)) {
      LOG.info("OCI session refresh already in progress");
      return;
    }
    try {
      sessionAuthenticate.refreshSessionAuthenticate(
          currentProject, cliResult -> updateSessionAuthentication(footerPanel, cliResult));
    } catch (Exception ex) {
      sessionCommandInProgress.set(false);
      notifyError(currentProject, ex.getMessage());
      LOG.warn("Failed to refresh security token session", ex);
    }
  }

  private void updateSessionAuthentication(
      @NotNull FooterPanel footerPanel, @NotNull OciCliResult cliResult) {
    runOnApplicationUIThread(
        () -> {
          try {
            LOG.info("Update OCI session authenticate profile" + cliResult);
            if (!cliResult.success() || hasErrors(cliResult)) {
              footerPanel.applySessionDisplay(SESSION_EXPIRED, false);
              notifyError(currentProject, formatErrors(cliResult.errors()));
              return;
            }

            footerPanel.applySessionDisplay(SESSION_ACTIVE, true);
            refreshFooter(footerPanel);
          } catch (Exception ex) {
            LOG.warn("OCI session authenticate profile failed", ex);
          } finally {
            sessionCommandInProgress.set(false);
          }
        });
  }

  private boolean shouldCreateNewSession(@NotNull Throwable throwable) {
    return containsMessage(throwable, "OCI profile not found:")
        || containsMessage(throwable, "Security token file is not readable:")
        || containsMessage(throwable, "Security token file is not configured");
  }

  private boolean containsMessage(@NotNull Throwable throwable, @NotNull String expectedMessage) {
    Throwable current = throwable;
    while (current != null) {
      String message = current.getMessage();
      if (message != null && message.contains(expectedMessage)) {
        return true;
      }
      current = current.getCause();
    }
    return false;
  }

  private boolean hasErrors(@NotNull OciCliResult cliResult) {
    List<String> errors = cliResult.errors();
    return errors != null && !errors.isEmpty();
  }

  private String formatErrors(List<String> errors) {
    return errors == null || errors.isEmpty()
        ? SESSION_EXPIRED
        : String.join(System.lineSeparator(), errors);
  }

  public Icon resolveSessionButtonIcon(boolean activeSession) {
    return activeSession ? AllIcons.Actions.Refresh : AllIcons.Actions.Exit;
  }

  private void runOnApplicationUIThread(@NotNull Runnable runnable) {
    ApplicationManager.getApplication().invokeLater(runnable);
  }
}
