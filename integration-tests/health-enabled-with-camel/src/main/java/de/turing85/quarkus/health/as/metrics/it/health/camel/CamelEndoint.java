package de.turing85.quarkus.health.as.metrics.it.health.camel;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.smallrye.mutiny.Uni;
import lombok.RequiredArgsConstructor;
import org.apache.camel.CamelContext;

@Path("camel")
@Produces(MediaType.TEXT_PLAIN)
@RequiredArgsConstructor
public class CamelEndoint {
  private final CamelContext context;

  @Path("resume")
  @POST
  public Uni<Void> resume() {
    // @formatter:off
    return Uni
        .createFrom().voidItem()
        .onItem().invoke(context::resume);
    // @formatter:on
  }

  @Path("suspend")
  @POST
  public Uni<Void> suspend() {
    // @formatter:off
    return Uni
        .createFrom().voidItem()
        .onItem().invoke(context::suspend);
    // @formatter:on
  }
}
