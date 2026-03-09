package dasarathi.devops.toolkit.gui.repositories.service;

import static dasarathi.devops.toolkit.core.DevOpsToolKitConstants.MESSAGE_TEST_DISABLED;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class RepositorySummaryServiceTest {

  @Test
  @Disabled(MESSAGE_TEST_DISABLED)
  void getRepositorySummaryListTest() {
    ProjectSummaryService projectSummaryService = new ProjectSummaryService();
    assertNotNull(projectSummaryService);
    // TO DO
  }
}
