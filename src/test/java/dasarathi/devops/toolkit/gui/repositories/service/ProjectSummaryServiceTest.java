package dasarathi.devops.toolkit.gui.repositories.service;

import static dasarathi.devops.toolkit.core.DevOpsToolKitConstants.MESSAGE_TEST_DISABLED;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class ProjectSummaryServiceTest {

  @Test
  @Disabled(MESSAGE_TEST_DISABLED)
  void getGetProjectTest() {
    ProjectSummaryService projectSummaryService = new ProjectSummaryService();
    assertNotNull(projectSummaryService);
    // TO DO
  }
}
