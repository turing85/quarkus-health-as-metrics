package de.turing85.quarkus.health.as.metrics.it.health.enabled;

import jakarta.ws.rs.core.Response;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

@QuarkusTest
class HealthMetricsTest {
  @ParameterizedTest
  // @formatter:off
  @CsvSource(
      delimiter = ';',
      value = {
          "application_status{group=\"health\",status=\"UP\"} 1.0",
          "application_status{group=\"live\",status=\"UP\"} 1.0",
          "application_status{group=\"ready\",status=\"UP\"} 1.0",
          "application_status{group=\"startup\",status=\"UP\"} 1.0",
          "application_status{group=\"well\",status=\"UP\"} 1.0",

          "application_status{group=\"health\",status=\"DOWN\"} 0.0",
          "application_status{group=\"live\",status=\"DOWN\"} 0.0",
          "application_status{group=\"ready\",status=\"DOWN\"} 0.0",
          "application_status{group=\"startup\",status=\"DOWN\"} 0.0",
          "application_status{group=\"well\",status=\"DOWN\"} 0.0",

          "application_status{group=\"foo\",status=\"UP\"} 1.0",
          "application_status{group=\"bar\",status=\"UP\"} 1.0",

          "application_status{group=\"foo\",status=\"DOWN\"} 0.0",
          "application_status{group=\"bar\",status=\"DOWN\"} 0.0",

          "application_health_check{check=\"custom\",status=\"UP\"} 1.0",

          "application_health_check{check=\"custom\",status=\"DOWN\"} 0.0",

          "application_health_check{check=\"custom-inner1\",status=\"UP\"} 1.0",
          "application_health_check{check=\"custom-inner2\",status=\"UP\"} 1.0",
          "application_health_check{check=\"custom-inner3\",status=\"UP\"} 1.0",
          "application_health_check{check=\"custom-inner4\",status=\"UP\"} 1.0",

          "application_health_check{check=\"custom-inner1\",status=\"DOWN\"} 0.0",
          "application_health_check{check=\"custom-inner2\",status=\"DOWN\"} 0.0",
          "application_health_check{check=\"custom-inner3\",status=\"DOWN\"} 0.0",
          "application_health_check{check=\"custom-inner4\",status=\"DOWN\"} 0.0",
      })
  // @formatter:on
  void whenUpThenMetricsContains(String line) {
    // GIVEN
    // @formatter:off
    RestAssured
        .when().post("/cache/reset")
        .then().statusCode(is(Response.Status.NO_CONTENT.getStatusCode()));

    // WHEN
    RestAssured
        .when().post("health/up")
        .then().statusCode(is(Response.Status.NO_CONTENT.getStatusCode()));
    // @formatter:on

    // THEN
    assertMetricsContains(line);
  }

  @ParameterizedTest
  // @formatter:off
  @CsvSource(
      delimiter = ';',
      value = {
          "application_status{group=\"health\",status=\"UP\"} 0.0",
          "application_status{group=\"live\",status=\"UP\"} 0.0",
          "application_status{group=\"ready\",status=\"UP\"} 0.0",
          "application_status{group=\"startup\",status=\"UP\"} 0.0",
          "application_status{group=\"well\",status=\"UP\"} 0.0",

          "application_status{group=\"health\",status=\"DOWN\"} 1.0",
          "application_status{group=\"live\",status=\"DOWN\"} 1.0",
          "application_status{group=\"ready\",status=\"DOWN\"} 1.0",
          "application_status{group=\"startup\",status=\"DOWN\"} 1.0",
          "application_status{group=\"well\",status=\"DOWN\"} 1.0",

          "application_status{group=\"foo\",status=\"UP\"} 0.0",
          "application_status{group=\"bar\",status=\"UP\"} 0.0",

          "application_status{group=\"foo\",status=\"DOWN\"} 1.0",
          "application_status{group=\"bar\",status=\"DOWN\"} 1.0",

          "application_health_check{check=\"custom\",status=\"UP\"} 0.0",

          "application_health_check{check=\"custom\",status=\"DOWN\"} 1.0",

          "application_health_check{check=\"custom-inner1\",status=\"UP\"} 0.0",
          "application_health_check{check=\"custom-inner2\",status=\"UP\"} 0.0",
          "application_health_check{check=\"custom-inner3\",status=\"UP\"} 0.0",
          "application_health_check{check=\"custom-inner4\",status=\"UP\"} 0.0",

          "application_health_check{check=\"custom-inner1\",status=\"DOWN\"} 1.0",
          "application_health_check{check=\"custom-inner2\",status=\"DOWN\"} 1.0",
          "application_health_check{check=\"custom-inner3\",status=\"DOWN\"} 1.0",
          "application_health_check{check=\"custom-inner4\",status=\"DOWN\"} 1.0",
      })
  // @formatter:on
  void whenDownThenMetricsContain(String line) {
    // GIVEN
    // @formatter:off
    RestAssured
        .when().post("/cache/reset")
        .then().statusCode(is(Response.Status.NO_CONTENT.getStatusCode()));

    // WHEN
    RestAssured
        .when().post("health/down")
        .then().statusCode(is(Response.Status.NO_CONTENT.getStatusCode()));
    // @formatter:on

    // THEN
    assertMetricsContains(line);
  }

  private static void assertMetricsContains(String line) {
    // @formatter:off
    RestAssured
        .when().get("q/metrics/")
        .then()
            .statusCode(is(Response.Status.OK.getStatusCode()))
            .body(containsString(line));
    // @formatter:on
  }
}
