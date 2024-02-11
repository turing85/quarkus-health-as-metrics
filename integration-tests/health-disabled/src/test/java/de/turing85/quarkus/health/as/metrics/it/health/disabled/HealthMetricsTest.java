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
          "application_status{group=\"health\",status=\"UP\"}",
          "application_status{group=\"live\",status=\"UP\"}",
          "application_status{group=\"ready\",status=\"UP\"}",
          "application_status{group=\"startup\",status=\"UP\"}",
          "application_status{group=\"well\",status=\"UP\"}",
          "application_status{group=\"foo\",status=\"UP\"}",
          "application_status{group=\"bar\",status=\"UP\"}",
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
