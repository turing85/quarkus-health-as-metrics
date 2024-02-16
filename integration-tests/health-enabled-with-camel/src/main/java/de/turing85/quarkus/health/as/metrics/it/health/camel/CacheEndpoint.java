package de.turing85.quarkus.health.as.metrics.it.health.camel;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import de.turing85.quarkus.health.as.metrics.runtime.Config;
import io.quarkus.cache.CacheInvalidateAll;
import io.smallrye.mutiny.Uni;

@Path("cache")
@Produces(MediaType.TEXT_PLAIN)
public class CacheEndpoint {
  @CacheInvalidateAll(cacheName = Config.CACHE_CHECK_NAME)
  @CacheInvalidateAll(cacheName = Config.CACHE_REGISTRY_NAME)
  @Path("reset")
  @POST
  public Uni<Void> reset() {
    return Uni.createFrom().voidItem();
  }
}
