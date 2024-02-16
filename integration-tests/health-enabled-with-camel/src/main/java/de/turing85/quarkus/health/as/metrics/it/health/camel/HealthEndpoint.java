package de.turing85.quarkus.health.as.metrics.it.health.camel;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import de.turing85.quarkus.health.as.metrics.it.commons.CustomHealthCheck;
import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.health.Liveness;

@Path("health")
class HealthEndpoint {
  private final CustomHealthCheck customHealthCheck;

  HealthEndpoint(@Liveness CustomHealthCheck customHealthCheck) {
    this.customHealthCheck = customHealthCheck;
  }

  @POST
  @Path("up")
  public Uni<Void> up() {
    // @formatter:off
    return Uni
        .createFrom().voidItem()
        .onItem().invoke(customHealthCheck::healthy);
    // @formatter:on
  }

  @POST
  @Path("down")
  public Uni<Void> down() {
    // @formatter:off
    return Uni
        .createFrom().voidItem()
        .onItem().invoke(customHealthCheck::unhealthy);
    // @formatter:on
  }
}
