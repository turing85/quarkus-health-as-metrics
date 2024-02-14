package de.turing85.quarkus.health.as.metrics.it.commons;

import io.smallrye.health.api.HealthGroup;
import io.smallrye.health.api.Wellness;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;
import org.eclipse.microprofile.health.Readiness;
import org.eclipse.microprofile.health.Startup;

@HealthGroup("foo")
@HealthGroup("bar")
@Liveness
@Readiness
@Startup
@Wellness
@Setter(AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class CustomHealthCheck implements HealthCheck {
  private boolean isHealthy = true;

  @Override
  public HealthCheckResponse call() {
    // @formatter:off
    return HealthCheckResponse
        .named("custom")
        .status(isHealthy())
        .withData("inner1", isHealthy() ? "UP" : "DOWN")
        .withData("inner2", isHealthy() ? "READY" : "NOT READY")
        .withData("inner3", isHealthy())
        .withData("inner4", isHealthy() ? 1337L : 0L)
        .withData("inner5", isHealthy() ? "FOO" : "BAR")
        .build();
    // @formatter:on
  }

  public boolean isHealthy() {
    return isHealthy;
  }

  public void healthy() {
    isHealthy(true);
  }

  public void unhealthy() {
    isHealthy(false);
  }
}
