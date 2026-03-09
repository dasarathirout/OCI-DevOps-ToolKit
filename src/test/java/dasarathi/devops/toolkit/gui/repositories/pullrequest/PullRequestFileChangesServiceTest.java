package dasarathi.devops.toolkit.gui.repositories.pullrequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.oracle.bmc.devops.model.PullRequestFileChangeSummary;
import java.util.List;
import org.junit.jupiter.api.Test;

class PullRequestFileChangesServiceTest {

  @Test
  void parseGitDiffOutputMapsNameStatusAndLineCounts() {
    List<PullRequestFileChangeSummary> fileChanges =
        PullRequestFileChangesService.parseGitDiffOutput(
            List.of(
                "M\tsrc/main/App.java",
                "A\tsrc/main/NewFile.java",
                "D\tsrc/main/OldFile.java",
                "R100\tsrc/main/Before.java\tsrc/main/After.java"),
            List.of(
                "10\t2\tsrc/main/App.java",
                "4\t0\tsrc/main/NewFile.java",
                "0\t8\tsrc/main/OldFile.java",
                "1\t1\tsrc/main/After.java"));

    assertEquals(4, fileChanges.size());
    assertEquals("MODIFIED", fileChanges.get(0).getChangeType());
    assertEquals("src/main/App.java", fileChanges.get(0).getNewPath());
    assertEquals(10, fileChanges.get(0).getAddedLinesCount());
    assertEquals(2, fileChanges.get(0).getDeletedLinesCount());
    assertEquals("ADDED", fileChanges.get(1).getChangeType());
    assertEquals("DELETED", fileChanges.get(2).getChangeType());
    assertEquals("RENAMED", fileChanges.get(3).getChangeType());
    assertEquals("src/main/Before.java", fileChanges.get(3).getOldPath());
    assertEquals("src/main/After.java", fileChanges.get(3).getNewPath());
  }
}
