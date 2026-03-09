package dasarathi.devops.toolkit.cli;

import static dasarathi.devops.toolkit.DevOpsToolKitToolWindowFactory.DEVOPS_TOOLKIT_REPOSITORY_OCI_CLIV_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

import com.intellij.openapi.project.Project;
import dasarathi.devops.toolkit.cli.CliExecutorHandler.OciCliResult;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CliGetVersionTest {

  private final CliGetVersion getVersion = new CliGetVersion();

  @Mock private Project project;

  @Test
  void testRunOciVersionCommandSuccess() {
    OciCliResult expectedResult = new OciCliResult(true, 0, List.of("3.63.0"), List.of());
    try (MockedStatic<CliExecutorHandler> mockedExecutor = mockStatic(CliExecutorHandler.class)) {
      mockedExecutor
          .when(
              () ->
                  CliExecutorHandler.execute(
                      project, List.of("oci", "--version"), "Failed to execute oci --version"))
          .thenReturn(expectedResult);
      OciCliResult result = getVersion.runOciVersionCommand(project);
      assertNotNull(result, "OCI Version result must not be null");
      assertTrue(result.success(), "OCI CLI command should succeed");
      assertEquals(List.of("3.63.0"), result.output(), "OCI CLI output should match");
      verify(project).putUserData(DEVOPS_TOOLKIT_REPOSITORY_OCI_CLIV_KEY, "3.63.0");
    }
  }
}
