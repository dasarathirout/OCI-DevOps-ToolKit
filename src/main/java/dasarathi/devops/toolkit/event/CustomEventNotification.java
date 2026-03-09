package dasarathi.devops.toolkit.event;

import static dasarathi.devops.toolkit.core.DevOpsToolKitConstants.GROUP_DEVOPS_TOOLKIT_NOTIFICATION;
import static dasarathi.devops.toolkit.core.DevOpsToolKitConstants.TITLE_NOTIFICATION_INFO;
import static dasarathi.devops.toolkit.core.DevOpsToolKitConstants.TITLE_NOTIFICATION_PROCESS_FAILED;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;

public class CustomEventNotification {
  private CustomEventNotification() {
    /* NOOP */
  }

  /*
   * POPUP Windows
   * **/
  public static void notifyError(Project project, String content) {
    NotificationGroupManager.getInstance()
        .getNotificationGroup(GROUP_DEVOPS_TOOLKIT_NOTIFICATION)
        .createNotification(TITLE_NOTIFICATION_PROCESS_FAILED, content, NotificationType.ERROR)
        .notify(project);
  }

  public static void notifyInfo(Project project, String content) {
    NotificationGroupManager.getInstance()
        .getNotificationGroup(GROUP_DEVOPS_TOOLKIT_NOTIFICATION)
        .createNotification(TITLE_NOTIFICATION_INFO, content, NotificationType.INFORMATION)
        .notify(project);
  }

  /*
   * Status Bar
   * ***/
  public static void showStatus(Project project, String message) {
    StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
    if (statusBar != null) {
      statusBar.setInfo(message);
    }
  }
}
