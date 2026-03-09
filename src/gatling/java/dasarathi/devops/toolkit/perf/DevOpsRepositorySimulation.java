package dasarathi.devops.toolkit.perf;

import static io.gatling.javaapi.core.CoreDsl.atOnceUsers;
import static io.gatling.javaapi.core.CoreDsl.global;
import static io.gatling.javaapi.core.CoreDsl.pause;
import static io.gatling.javaapi.core.CoreDsl.repeat;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

import io.gatling.javaapi.core.Assertion;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import java.time.Duration;

public class DevOpsRepositorySimulation extends Simulation {
  private static final String BASE_URL =
      SimulationProperties.stringProperty("baseUrl", "http://localhost:8080");
  private static final String PROJECTS_PATH =
      SimulationProperties.stringProperty("projectsPath", "/devops/projects");
  private static final String REPOSITORIES_PATH =
      SimulationProperties.stringProperty("repositoriesPath", "/devops/repositories");
  private static final String PULL_REQUESTS_PATH =
      SimulationProperties.stringProperty("pullRequestsPath", "/devops/pull-requests");
  private static final String PULL_REQUEST_DETAIL_PATH =
      SimulationProperties.stringProperty(
          "pullRequestDetailPath", "/devops/pull-requests/demo-pull-request");
  private static final int VIRTUAL_USERS = SimulationProperties.intProperty("vu", 1);
  private static final int ITERATIONS = SimulationProperties.intProperty("iterations", 1);
  private static final long PAUSE_MILLIS = SimulationProperties.longProperty("pauseMillis", 250L);
  private static final double MAX_FAILED_PERCENT =
      SimulationProperties.doubleProperty("maxFailedPercent", 1.0d);
  private static final long P95_RESPONSE_TIME_MILLIS =
      SimulationProperties.longProperty("p95Millis", 2000L);

  private static final HttpProtocolBuilder HTTP_PROTOCOL =
      SimulationProperties.applyOptionalAuthorization(
          http.baseUrl(BASE_URL)
              .acceptHeader("application/json")
              .userAgentHeader("OCI-DevOps-ToolKit-Gatling"));

  private static final ScenarioBuilder SCENARIO =
      scenario("OCI DevOps Repository Flow")
          .repeat(ITERATIONS)
          .on(
              http("List DevOps projects").get(PROJECTS_PATH).check(status().in(200, 204)),
              pause(Duration.ofMillis(PAUSE_MILLIS)),
              http("List repositories").get(REPOSITORIES_PATH).check(status().in(200, 204)),
              pause(Duration.ofMillis(PAUSE_MILLIS)),
              http("List pull requests").get(PULL_REQUESTS_PATH).check(status().in(200, 204)),
              pause(Duration.ofMillis(PAUSE_MILLIS)),
              http("Get pull request detail")
                  .get(PULL_REQUEST_DETAIL_PATH)
                  .check(status().in(200, 204)));

  private static final Assertion FAILED_RATE_UNDER_THRESHOLD =
      global().failedRequests().percent().lte(MAX_FAILED_PERCENT);
  private static final Assertion P95_UNDER_THRESHOLD =
      global().responseTime().percentile3().lt(P95_RESPONSE_TIME_MILLIS);

  {
    setUp(SCENARIO.injectOpen(atOnceUsers(VIRTUAL_USERS)))
        .assertions(FAILED_RATE_UNDER_THRESHOLD, P95_UNDER_THRESHOLD)
        .protocols(HTTP_PROTOCOL);
  }
}
