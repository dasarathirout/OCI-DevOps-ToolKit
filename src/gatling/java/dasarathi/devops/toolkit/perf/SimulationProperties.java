package dasarathi.devops.toolkit.perf;

import io.gatling.javaapi.http.HttpProtocolBuilder;

final class SimulationProperties {
  private static final String AUTH_HEADER_PROPERTY = "authHeader";

  private SimulationProperties() {}

  static String stringProperty(String name, String defaultValue) {
    String value = System.getProperty(name);
    if (value == null || value.isBlank()) {
      return defaultValue;
    }
    return value.trim();
  }

  static int intProperty(String name, int defaultValue) {
    String value = System.getProperty(name);
    if (value == null || value.isBlank()) {
      return defaultValue;
    }
    return Integer.parseInt(value.trim());
  }

  static long longProperty(String name, long defaultValue) {
    String value = System.getProperty(name);
    if (value == null || value.isBlank()) {
      return defaultValue;
    }
    return Long.parseLong(value.trim());
  }

  static double doubleProperty(String name, double defaultValue) {
    String value = System.getProperty(name);
    if (value == null || value.isBlank()) {
      return defaultValue;
    }
    return Double.parseDouble(value.trim());
  }

  static HttpProtocolBuilder applyOptionalAuthorization(HttpProtocolBuilder protocolBuilder) {
    String authHeader = System.getProperty(AUTH_HEADER_PROPERTY);
    if (authHeader == null || authHeader.isBlank()) {
      return protocolBuilder;
    }
    return protocolBuilder.header("Authorization", authHeader.trim());
  }
}
