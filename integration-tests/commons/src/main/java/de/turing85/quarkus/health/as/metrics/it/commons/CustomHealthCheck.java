package de.turing85.quarkus.health.as.metrics.it.commons;

import io.smallrye.health.api.HealthGroup;
import io.smallrye.health.api.Wellness;
import lombok.AccessLevel;
import lombok.Getter;
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
@Getter
@Setter(AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class CustomHealthCheck implements HealthCheck {
  private boolean isHealthy = true;

  @Override
  public HealthCheckResponse call() {
    return HealthCheckResponse.named("custom").status(isHealthy()).build();
  }

  public void healthy() {
    setHealthy(true);
  }

  public void unhealthy() {
    setHealthy(false);
  }
}
