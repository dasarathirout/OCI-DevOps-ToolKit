package dasarathi.devops.toolkit.gui.repositories.service;

import static dasarathi.devops.toolkit.core.DevOpsToolKitConstants.MESSAGE_TEST_DISABLED;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.intellij.openapi.project.Project;
import dasarathi.devops.toolkit.gui.repositories.pullrequest.PullRequestSummaryService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class PullRequestSummaryServiceTest {

  @Mock Project mockProject;

  @Test
  @Disabled(MESSAGE_TEST_DISABLED)
  void getListPullRequestsSuccessTest() {
    PullRequestSummaryService getPullRequestSummaryList = new PullRequestSummaryService();
    assertNotNull(getPullRequestSummaryList);
    // TO DO
  }
}
