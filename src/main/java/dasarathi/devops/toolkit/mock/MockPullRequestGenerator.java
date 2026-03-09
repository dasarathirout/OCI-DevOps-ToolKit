package dasarathi.devops.toolkit.mock;

import com.oracle.bmc.devops.model.PrincipalDetails;
import com.oracle.bmc.devops.model.PullRequest;
import com.oracle.bmc.devops.model.PullRequestCollection;
import com.oracle.bmc.devops.model.PullRequestSummary;
import com.oracle.bmc.devops.responses.ListPullRequestsResponse;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MockPullRequestGenerator {
  private MockPullRequestGenerator() {
    /* NOOP */
  }

  private static final Random RANDOM = new Random(24032026L);

  private static final String DEFAULT_COMPARTMENT_ID =
      "ocid1.compartment.oc1..aaaaaaaafs2uigzlcc6h6hx5gc6pm2qdn7bxgjt22llyivlktoc6gbdvqxva";
  private static final String DEFAULT_NAMESPACE = "axuxirvibvvo";
  private static final String DEFAULT_PROJECT_NAME = "OHAI_CLINICAL";
  private static final String DEFAULT_BRANCH_MASTER = "master";
  private static final String DEFAULT_BRANCH_MAIN = "main";

  private record MockRepository(
      String repositoryName,
      String compartmentId,
      String namespace,
      String projectName,
      String defaultBranch,
      PullRequest.LifecycleState lifecycleState) {}

  private static final List<MockRepository> MOCK_REPOSITORIES =
      List.of(
          new MockRepository(
              "notejobs-ui",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "wiremock-state-extension",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "agentic-healthcare-agent",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "agentic-healthcare-ui",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "ai-sdlc",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "mcp-moonbird",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MAIN,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "barcode-scanning-scripts",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "Questionnaire-Common-Utils",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "decision-support-ui",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "clinical-device-data-ui",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "clinical-device-data-service",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "safetyrounding-ui",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "safetyrounding-service",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "ohpp-common",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "ohai-som-devops-tools",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MAIN,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "common-service",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "ehrc-infra-rag-poc",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "health-expert-agentic-service",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "semantic-index-pre-ingestion-automation",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "admin-console-ui",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "coding-summary-ui",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "coding-worklist-ui",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "coding-caam-ui",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "coding-ui",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "order-cosign-proposals-messaging-ui",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "iam-itadmin-service",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "needs-attention-service",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "video-visits-wca-webapp",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "mcp-service-proxy",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MAIN,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "clinical-vision-observations-service",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "claims-agent",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "dischargeplanningcaad-ui",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "ohai-components-ui",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "caad-coding-e2e-tests",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "anatomic-pathology-service",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "orders-weekly-ops-tools",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "medadmin-architecture-toolkit",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "patient-advisory-service",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "radiology-ui",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "ehrc-healthcheck",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "charge-skg-germany-service",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "decision-support-broker-service",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "ehrc-prior-auth-caad-service",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "transfusion-administration-ui",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "ehrc-devops-mcp-agent",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MAIN,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "transfusion-administration-service",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "anatomic-pathology-ui",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "oehr-search-service",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "prenatal-visit-ui",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "form-router-service",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "rehab-service",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "sf-ui-plugin-shepherd",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "ehrc-shared-agents",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "encounter-linking-service",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "prenatal-visit-service",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "handoff-service",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "regulatorysubmission-ui",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "regulatorysubmission-service",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "claim-status-management-service",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "ehrc-helloworld-gdk",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "fhir-order-management-service",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "iam-user-scim-facade-service",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "dicom-viewer-ui",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "developer-agent",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "mcp-code-search",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "order-proposal-service",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "healthos-poc",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "mcp-kb-search",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "eligibility-service",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "ehrc-device-telemetry-ingestor",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "ehrc-device-telemetry-streaming",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "pregnancy-ui",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "pregnancy-service",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "dicom-config-service",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "cardiology-service",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "ehi-export-service",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "ccda-validator-lib",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "ehrm-spog",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "charge-skg-service",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "charge-item-definition-service",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "diagnostic-worklist-ui",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "ohpas-common",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "patient-care-pathway-ui",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "needs-attention-conversational-ui",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "order-action-event-service",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "policy-lsp",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "api-recorder-replay-mock-server",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "ehrc-orders-common-library",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "order-related-attributes",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "oar-i18n-test-suite",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MAIN,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "prompt-config-plugins",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "multum-research-agent",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "order-scheduling",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "specimen-service",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "patient-portal-legacy-service",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "claim-management-service-mock",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "bill-estimation-ui",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "dsi-reference-library-ui",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "guarantor-billing-ui",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active),
          new MockRepository(
              "ehrc_devops_tool",
              DEFAULT_COMPARTMENT_ID,
              DEFAULT_NAMESPACE,
              DEFAULT_PROJECT_NAME,
              DEFAULT_BRANCH_MASTER,
              PullRequest.LifecycleState.Active));

  public static List<PullRequestSummary> mockedListPullRequestSummaryList() {
    return ListPullRequestsResponse.builder()
        .pullRequestCollection(buildMockCollection())
        .build()
        .getPullRequestCollection()
        .getItems();
  }

  private static PullRequestCollection buildMockCollection() {
    List<PullRequestSummary> items = new ArrayList<>(MOCK_REPOSITORIES.size());
    for (int index = 0; index < MOCK_REPOSITORIES.size(); index++) {
      items.add(createMockSummary(index, MOCK_REPOSITORIES.get(index)));
    }
    return PullRequestCollection.builder().items(items).build();
  }

  private static PullRequestSummary createMockSummary(int index, MockRepository repository) {
    Date created = dateDaysAgo((index % 27) + 1);
    Date updated = dateDaysAgo(index % 11);

    int ticket = randomFourDigits(index * 17 + 11);
    int repoIdDigits = randomFourDigits(index * 29 + 7);

    String prefixedRepoName = "mock-" + repository.repositoryName();
    String maskedRepoId = "ocid1.devopsrepository.oc1.phx." + repoIdDigits;
    String maskedPullRequestId = "ocid1.pullrequest.oc1.phx." + randomFourDigits(index * 31 + 3);
    String principalId = "ocid1.user.oc1.." + randomFourDigits(index * 13 + 5);
    String principalName = "developer" + String.format("%02d", (index % 97) + 1) + "@oracle.com";

    String sourceBranch = "feature/MOCK-" + ticket + "/" + normalizeForBranch(prefixedRepoName);
    String destinationBranch =
        repository.defaultBranch() == null || repository.defaultBranch().isBlank()
            ? DEFAULT_BRANCH_MASTER
            : repository.defaultBranch();

    Map<String, String> freeformTags = new HashMap<>();
    freeformTags.put("ticket", "MOCK-" + ticket);
    freeformTags.put("repository", prefixedRepoName);
    freeformTags.put("namespace", repository.namespace());
    freeformTags.put("project", repository.projectName());

    Map<String, Map<String, Object>> definedTags = new HashMap<>();
    Map<String, Object> operationsTag = new HashMap<>();
    operationsTag.put("repositoryName", prefixedRepoName);
    operationsTag.put("ticket", "MOCK-" + ticket);
    operationsTag.put("owner", principalName);
    definedTags.put("Operations", operationsTag);

    Map<String, Map<String, Object>> systemTags = new HashMap<>();
    Map<String, Object> oracleTag = new HashMap<>();
    oracleTag.put("createdBy", "MockGenerator");
    oracleTag.put("createdOn", created.toString());
    systemTags.put("Oracle-Tags", oracleTag);

    String title =
        "Improve " + prefixedRepoName + " workflows for release readiness and service resilience";

    return PullRequestSummary.builder()
        .id(maskedPullRequestId)
        .displayName(
            "PR-" + String.format("%04d", index + 1001) + " [MOCK-" + ticket + "] " + title)
        .compartmentId(repository.compartmentId())
        .description(buildDescription(prefixedRepoName))
        .repositoryId(maskedRepoId)
        .sourceRepositoryId(maskedRepoId)
        .sourceBranch(sourceBranch)
        .destinationBranch(destinationBranch)
        .sourceCommitIdAtTermination("c0ffee" + String.format("%034d", index + 1))
        .mergeBaseCommitIdAtTermination("deadbe" + String.format("%034d", index + 1))
        .timeCreated(created)
        .timeUpdated(updated)
        .lifecycleState(repository.lifecycleState())
        .lifecycleDetails(PullRequest.LifecycleDetails.Open)
        .totalComments((index % 17) + 1)
        .totalReviewers((index % 5) + 1)
        .createdBy(
            PrincipalDetails.builder()
                .principalId(principalId)
                .principalName(principalName)
                .principalType(PrincipalDetails.PrincipalType.User)
                .principalState(PrincipalDetails.PrincipalState.Active)
                .build())
        .freeformTags(freeformTags)
        .definedTags(definedTags)
        .systemTags(systemTags)
        .build();
  }

  private static String buildDescription(String prefixedRepoName) {
    return "Delivers stable enhancements for "
        + prefixedRepoName
        + " workflows with better validation, logging, and reliability";
  }

  private static int randomFourDigits(int salt) {
    int seeded = Math.abs((RANDOM.nextInt(9000) + 1000 + salt) % 9000);
    return seeded + 1000;
  }

  private static String normalizeForBranch(String value) {
    return value.toLowerCase().replaceAll("[^a-z0-9-]+", "-");
  }

  private static Date dateDaysAgo(int days) {
    Calendar calendar = Calendar.getInstance();
    calendar.add(Calendar.DAY_OF_YEAR, -days);
    return calendar.getTime();
  }
}
