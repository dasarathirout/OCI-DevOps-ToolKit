package dasarathi.devops.toolkit.gui.repositories;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.oracle.bmc.devops.model.DiffChunk;
import com.oracle.bmc.devops.model.DiffLineDetails;
import com.oracle.bmc.devops.model.DiffSection;
import com.oracle.bmc.devops.model.FileDiffResponse;
import com.oracle.bmc.devops.model.PrincipalDetails;
import com.oracle.bmc.devops.model.PullRequestCommentSummary;
import com.oracle.bmc.devops.model.PullRequestFileChangeSummary;
import com.oracle.bmc.devops.model.PullRequestSummary;
import java.util.List;
import org.junit.jupiter.api.Test;

class PullRequestPresentationTest {

  @Test
  void formatPullRequestSummaryDetailsUsesAvailableSummaryFields() {
    PullRequestSummary summary =
        PullRequestSummary.builder()
            .id("pr-123")
            .displayName("Add fallback review details")
            .destinationBranch("main")
            .createdBy(PrincipalDetails.builder().principalName("dev.user").build())
            .build();

    String details = PullRequestPresentation.formatPullRequestSummaryDetails(summary);

    assertTrue(details.contains("ID: pr-123"));
    assertTrue(details.contains("Title: Add fallback review details"));
    assertTrue(details.contains("Destination Branch: main"));
    assertTrue(details.contains("Author: dev.user"));
  }

  @Test
  void formatPullRequestFileChangesPrefersRealFileChangeData() {
    PullRequestFileChangeSummary fileChange =
        PullRequestFileChangeSummary.builder()
            .oldPath("src/old/File.java")
            .newPath("src/main/File.java")
            .changeType("MODIFIED")
            .addedLinesCount(12)
            .deletedLinesCount(3)
            .hasConflicts(Boolean.TRUE)
            .build();

    String diffSummary =
        PullRequestPresentation.formatPullRequestFileChanges(
            List.of(fileChange), List.of(), "fallback");

    assertTrue(diffSummary.contains("Changed files: 1"));
    assertTrue(diffSummary.contains("src/old/File.java -> src/main/File.java"));
    assertTrue(diffSummary.contains("Type: MODIFIED"));
    assertTrue(diffSummary.contains("Lines: +12 / -3"));
    assertTrue(diffSummary.contains("Conflicts: yes"));
  }

  @Test
  void formatPullRequestFileChangeSummaryUsesStructuredRowText() {
    PullRequestFileChangeSummary fileChange =
        PullRequestFileChangeSummary.builder()
            .newPath("src/main/File.java")
            .changeType("MODIFIED")
            .addedLinesCount(7)
            .deletedLinesCount(2)
            .build();

    String row = PullRequestPresentation.formatPullRequestFileChangeSummary(fileChange);

    assertTrue(row.contains("src/main/File.java"));
    assertTrue(row.contains("MODIFIED  +7 / -2"));
  }

  @Test
  void formatCommentDerivedDiffSummaryGroupsCommentContextByFile() {
    PullRequestCommentSummary comment =
        PullRequestCommentSummary.builder()
            .filePath("src/main/File.java")
            .lineNumber(42)
            .data("Needs a null check")
            .build();

    String detail =
        PullRequestPresentation.formatCommentDerivedDiffSummary(
            "src/main/File.java", List.of(comment));

    assertTrue(detail.contains("File: src/main/File.java"));
    assertTrue(detail.contains("Context source: pull request comments"));
    assertTrue(detail.contains("Line: 42"));
    assertTrue(detail.contains("Needs a null check"));
  }

  @Test
  void formatRepoFileDiffRendersPatchText() {
    FileDiffResponse diffResponse =
        FileDiffResponse.builder()
            .oldPath("src/old/File.java")
            .newPath("src/main/File.java")
            .changes(
                List.of(
                    DiffChunk.builder()
                        .baseLine(10)
                        .baseSpan(2)
                        .targetLine(10)
                        .targetSpan(3)
                        .diffSections(
                            List.of(
                                DiffSection.builder()
                                    .type("default")
                                    .lines(
                                        List.of(
                                            DiffLineDetails.builder()
                                                .baseLine(10)
                                                .targetLine(10)
                                                .lineContent(" context line")
                                                .build(),
                                            DiffLineDetails.builder()
                                                .baseLine(null)
                                                .targetLine(11)
                                                .lineContent(" added line")
                                                .build(),
                                            DiffLineDetails.builder()
                                                .baseLine(11)
                                                .targetLine(null)
                                                .lineContent(" removed line")
                                                .build()))
                                    .build()))
                        .build()))
            .build();

    String patch = PullRequestPresentation.formatRepoFileDiff(diffResponse);

    assertTrue(patch.contains("--- src/old/File.java"));
    assertTrue(patch.contains("+++ src/main/File.java"));
    assertTrue(patch.contains("@@ -10,2 +10,3 @@"));
    assertTrue(patch.contains("  context line"));
    assertTrue(patch.contains("+ added line"));
    assertTrue(patch.contains("- removed line"));
  }

  @Test
  void formatPullRequestFileChangesFallsBackToCommentPaths() {
    PullRequestCommentSummary comment =
        PullRequestCommentSummary.builder().filePath("src/test/FileTest.java").build();

    String diffSummary =
        PullRequestPresentation.formatPullRequestFileChanges(
            List.of(), List.of(comment), "fallback");

    assertTrue(diffSummary.contains("src/test/FileTest.java"));
  }

  @Test
  void formatPullRequestCommentSummaryIncludesFileLineAndText() {
    PullRequestCommentSummary comment =
        PullRequestCommentSummary.builder()
            .filePath("src/main/File.java")
            .lineNumber(42)
            .data("Needs a null check")
            .build();

    String row = PullRequestPresentation.formatPullRequestCommentSummary(comment);

    assertTrue(row.contains("src/main/File.java : 42"));
    assertTrue(row.contains("Needs a null check"));
  }
}
