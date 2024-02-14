package de.turing85.quarkus.health.as.metrics.it.health.disabled;

import jakarta.ws.rs.core.Response;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.hamcrest.Matchers.*;

@QuarkusTest
class HealthMetricsTest {
  @ParameterizedTest
  // @formatter:off
  @CsvSource(
      delimiter = ';',
      value = {
          "application_status{group=\"health\"",
          "application_status{group=\"live\"",
          "application_status{group=\"ready\"",
          "application_status{group=\"startup\"",
          "application_status{group=\"well\"",

          "application_status{group=\"foo\"",
          "application_status{group=\"bar\"",

          "application_health_check{check=\"custom\"",

          "application_health_check{check=\"custom-inner1\"",
          "application_health_check{check=\"custom-inner2\"",
          "application_health_check{check=\"custom-inner3\"",
          "application_health_check{check=\"custom-inner4\"",
      })
  // @formatter:on
  void metricDoesNotContain(String line) {
    // THEN
    assertMetricsDoesNotContain(line);
  }

  private static void assertMetricsDoesNotContain(String line) {
    // @formatter:off
    RestAssured
        .when().get("q/metrics/")
        .then()
            .statusCode(is(Response.Status.OK.getStatusCode()))
            .body(not(containsString(line)));
    // @formatter:on
  }
}
