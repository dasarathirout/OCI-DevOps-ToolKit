package dasarathi.devops.toolkit.gui.repositories.pullrequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.oracle.bmc.devops.model.FileDiffResponse;
import java.util.List;
import org.junit.jupiter.api.Test;

class RepoFileDiffServiceTest {

  @Test
  void parseUnifiedDiffMapsPatchToFileDiffResponse() {
    FileDiffResponse diffResponse =
        RepoFileDiffService.parseUnifiedDiff(
            List.of(
                "diff --git a/src/main/App.java b/src/main/App.java",
                "--- a/src/main/App.java",
                "+++ b/src/main/App.java",
                "@@ -10,2 +10,3 @@",
                " context line",
                "-removed line",
                "+added line",
                "+another added line"),
            "src/main/App.java");

    assertEquals("src/main/App.java", diffResponse.getOldPath());
    assertEquals("src/main/App.java", diffResponse.getNewPath());
    assertEquals(1, diffResponse.getChanges().size());
    assertEquals(10, diffResponse.getChanges().getFirst().getBaseLine());
    assertEquals(2, diffResponse.getChanges().getFirst().getBaseSpan());
    assertEquals(10, diffResponse.getChanges().getFirst().getTargetLine());
    assertEquals(3, diffResponse.getChanges().getFirst().getTargetSpan());
    assertEquals(
        4, diffResponse.getChanges().getFirst().getDiffSections().getFirst().getLines().size());
  }
}
