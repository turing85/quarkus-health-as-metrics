package de.turing85.quarkus.health.as.metrics.runtime.registries;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.ToDoubleFunction;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Singleton;

import de.turing85.quarkus.health.as.metrics.runtime.Config;
import de.turing85.quarkus.health.as.metrics.runtime.datamappers.HealthResponseDataMapper;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.cache.CacheKey;
import io.quarkus.cache.CacheResult;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.health.registry.HealthRegistries;
import io.smallrye.health.registry.HealthRegistryImpl;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.jboss.logging.Logger;

@Singleton
public class HealthRegistriesMetricsRegistrar {
  private static final Logger LOGGER = Logger.getLogger(HealthRegistriesMetricsRegistrar.class);

  private final MeterRegistry registry;
  private final Instance<HealthRegistries> healthRegistries;
  private final Instance<HealthResponseDataMapper<?>> dataMappers;

  HealthRegistriesMetricsRegistrar(MeterRegistry registry,
      @Any Instance<HealthRegistries> healthRegistries,
      @Any Instance<HealthResponseDataMapper<?>> dataMappers) {
    this.registry = registry;
    this.healthRegistries = healthRegistries;
    this.dataMappers = dataMappers;
  }

  void registerHealthRegistries(@Observes StartupEvent ignored) {
    // @formatter:off
    healthRegistries.stream()
        .map(healthRegistry -> List.of(
            healthRegistry.getLivenessRegistry(),
            healthRegistry.getReadinessRegistry(),
            healthRegistry.getStartupRegistry(),
            healthRegistry.getWellnessRegistry()))
        .flatMap(Collection::stream)
        .filter(HealthRegistryImpl.class::isInstance)
        .map(HealthRegistryImpl.class::cast)
        .forEach(this::registerHealthRegistry);
    // @formatter:on
  }

  private void registerHealthRegistry(HealthRegistryImpl healthRegistry) {
    fetchRegistryData(healthRegistry)
        .forEach(response -> registerHealthCheckResponse(response, healthRegistry));
  }

  @CacheResult(cacheName = Config.CACHE_REGISTRY_NAME)
  public Collection<HealthCheckResponse> fetchRegistryData(
      @CacheKey HealthRegistryImpl healthRegistry) {
    List<HealthCheckResponse> responses = new ArrayList<>();
    for (var check : healthRegistry.getChecks(Map.of())) {
      try {
        responses.add(check.subscribe().asCompletionStage().toCompletableFuture().get());
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        LOGGER.warn("Received interruption during check execution", e);
      } catch (ExecutionException e) {
        LOGGER.warn("Received execution exception during check execution", e);
      }
    }
    return responses;
  }

  private void registerHealthCheckResponse(HealthCheckResponse healthResponse,
      HealthRegistryImpl healthRegistry) {
    Map<String, Boolean> isUnmapped = new HashMap<>();
    // @formatter:off
    List<HealthResponseDataMapper<?>> statusMappers = dataMappers.stream()
        .filter(mapper -> mapper.mappableType().equals(HealthCheckResponse.Status.class))
        .toList();
    for (HealthResponseDataMapper<?> dataMapper : statusMappers) {
      Optional.of(healthResponse)
          .filter(response -> isUnmapped.getOrDefault(response.getName(), true))
          .filter(response -> dataMapper.keyFilterPattern().matcher(response.getName()).matches())

          // We know that isUnmapped.get(entry.getKey()) is null, thus
          // isUnmapped.put(entry.getKey(), false) will return null
          // We use this trick to avoid calling ".peek(...)" on the stream.
          .filter(response -> Objects.isNull(isUnmapped.put(response.getName(), false)))

          .map(response -> createGaugeBuildersForRegistry(
              response.getName(),
              healthRegistry,
              dataMapper))
          .stream().flatMap(Collection::stream)
          .forEach(builder -> builder.register(registry));
      }
    // @formatter:on
  }

  private static List<Gauge.Builder<HealthRegistryImpl>> createGaugeBuildersForRegistry(
      String registryName, HealthRegistryImpl registry, HealthResponseDataMapper<?> mapper) {
    return List.of(
        createGaugeMapperForRegistry(registryName, registry, "UP", mapper.registryUpMapper()),
        createGaugeMapperForRegistry(registryName, registry, "DOWN", mapper.registryDownMapper()));
  }

  private static Gauge.Builder<HealthRegistryImpl> createGaugeMapperForRegistry(String checkName,
      HealthRegistryImpl registry, String status, ToDoubleFunction<HealthRegistryImpl> mapper) {
    // @formatter:off
    return Gauge.builder(Config.INDIVIDUAL_CHECK_NAME, registry, mapper)
        .tag(Config.TAG_CHECK, checkName)
        .tag(Config.TAG_STATUS, status)
        .strongReference(true);
    // @formatter:on
  }
}
