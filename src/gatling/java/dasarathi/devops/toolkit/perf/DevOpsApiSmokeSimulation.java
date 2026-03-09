package dasarathi.devops.toolkit.perf;

import static io.gatling.javaapi.core.CoreDsl.atOnceUsers;
import static io.gatling.javaapi.core.CoreDsl.global;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

import io.gatling.javaapi.core.Assertion;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

public class DevOpsApiSmokeSimulation extends Simulation {
  private static final String BASE_URL =
      SimulationProperties.stringProperty("baseUrl", "https://api-ecomm.gatling.io");
  private static final String SMOKE_PATH = SimulationProperties.stringProperty("smokePath", "/session");
  private static final int VIRTUAL_USERS = SimulationProperties.intProperty("vu", 1);
  private static final long P95_RESPONSE_TIME_MILLIS =
      SimulationProperties.longProperty("p95Millis", 1500L);

  private static final HttpProtocolBuilder HTTP_PROTOCOL =
      SimulationProperties.applyOptionalAuthorization(
          http.baseUrl(BASE_URL)
              .acceptHeader("application/json")
              .userAgentHeader("OCI-DevOps-ToolKit-Gatling"));

  private static final ScenarioBuilder SCENARIO =
      scenario("OCI DevOps ToolKit API Smoke")
          .exec(http("Smoke endpoint").get(SMOKE_PATH).check(status().is(200)));

  private static final Assertion NO_FAILED_REQUESTS = global().failedRequests().count().lt(1L);
  private static final Assertion P95_UNDER_THRESHOLD =
      global().responseTime().percentile3().lt(P95_RESPONSE_TIME_MILLIS);

  {
    setUp(SCENARIO.injectOpen(atOnceUsers(VIRTUAL_USERS)))
        .assertions(NO_FAILED_REQUESTS, P95_UNDER_THRESHOLD)
        .protocols(HTTP_PROTOCOL);
  }
}
