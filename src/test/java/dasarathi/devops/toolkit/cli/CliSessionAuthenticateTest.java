package dasarathi.devops.toolkit.cli;

import static dasarathi.devops.toolkit.core.DevOpsToolKitConstants.MESSAGE_TEST_DISABLED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import dasarathi.devops.toolkit.gui.repositories.pullrequest.PullRequestSummaryService;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CliSessionAuthenticateTest {
  private static final Logger LOG = Logger.getInstance(PullRequestSummaryService.class);

  private CliSessionAuthenticate authenticateService;

  @Mock ProgressIndicator progressIndicator;
  @Mock Project project;

  @BeforeEach
  void setup() {
    authenticateService = new CliSessionAuthenticate();
  }

  @AfterEach
  void tearDown() {
    authenticateService = null;
  }

  @Test
  void runAuthenticateSessionMockedResponseFile() throws IOException {
    Map<String, Object> mockedResponse = readJsonResource();
    assertNotNull(mockedResponse);
    assertEquals(4, mockedResponse.size());
  }

  @Test
  void authenticateSessionCommandIncludesOcnaSamlIdentityProvider() {
    List<String> command = CliSessionAuthenticate.authenticateSessionCommand();

    assertTrue(command.contains("--identity-provider-name"));
    assertEquals("ocna-saml", command.get(command.indexOf("--identity-provider-name") + 1));
  }

  @Test
  @Disabled(MESSAGE_TEST_DISABLED)
  void ociSessionValidationCLITest() {
    CliExecutorHandler.OciCliResult listRegion = authenticateService.runSessionValidation(project);
    assertNotNull(listRegion);
    assertTrue(listRegion.success());
  }

  private Map<String, Object> readJsonResource() throws IOException {
    String commandResponse = "mocked/responses/ociSessionAuthenticateCommandResponse.json";
    ObjectMapper mapper = new ObjectMapper();
    try (InputStream inputStream =
        getClass().getClassLoader().getResourceAsStream(commandResponse)) {
      if (inputStream == null) {
        LOG.error("Resource not found: {}", commandResponse);
      }
      return mapper.readValue(inputStream, new TypeReference<>() {});
    }
  }
}
