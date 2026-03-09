package dasarathi.devops.toolkit.gui.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.oracle.bmc.devops.model.PullRequest;
import com.oracle.bmc.devops.model.PullRequestSummary;
import org.junit.jupiter.api.Test;

class RepositoryPanelHandlerTest {

  @Test
  void getStatusValueMapsOpenLifecycleDetailsToOpened() {
    PullRequestSummary summary =
        PullRequestSummary.builder().lifecycleDetails(PullRequest.LifecycleDetails.Open).build();

    assertEquals("OPENED", RepositoryPanelHandler.getStatusValue(summary));
  }

  @Test
  void getStatusValueMapsConflictLifecycleDetailsToOpened() {
    PullRequestSummary summary =
        PullRequestSummary.builder()
            .lifecycleDetails(PullRequest.LifecycleDetails.Conflict)
            .build();

    assertEquals("OPENED", RepositoryPanelHandler.getStatusValue(summary));
  }

  @Test
  void getStatusValueMapsClosedLifecycleDetailsToClosed() {
    PullRequestSummary summary =
        PullRequestSummary.builder().lifecycleDetails(PullRequest.LifecycleDetails.Closed).build();

    assertEquals("CLOSED", RepositoryPanelHandler.getStatusValue(summary));
  }

  @Test
  void getStatusValueMapsMergedLifecycleDetailsToMerged() {
    PullRequestSummary summary =
        PullRequestSummary.builder().lifecycleDetails(PullRequest.LifecycleDetails.Merged).build();

    assertEquals("MERGED", RepositoryPanelHandler.getStatusValue(summary));
  }

  @Test
  void getStatusValueMapsMergingLifecycleDetailsToMerged() {
    PullRequestSummary summary =
        PullRequestSummary.builder().lifecycleDetails(PullRequest.LifecycleDetails.Merging).build();

    assertEquals("MERGED", RepositoryPanelHandler.getStatusValue(summary));
  }
}
