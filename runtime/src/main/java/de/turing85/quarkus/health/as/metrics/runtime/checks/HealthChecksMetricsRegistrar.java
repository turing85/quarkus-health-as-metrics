package de.turing85.quarkus.health.as.metrics.runtime.checks;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;

@Singleton
public class HealthChecksMetricsRegistrar {
  private final MeterRegistry registry;
  private final Instance<HealthCheck> healthChecks;
  private final Instance<HealthResponseDataMapper<?>> dataMappers;

  HealthChecksMetricsRegistrar(MeterRegistry registry, @Any Instance<HealthCheck> healthChecks,
      @Any Instance<HealthResponseDataMapper<?>> dataMappers) {
    this.registry = registry;
    this.healthChecks = healthChecks;
    this.dataMappers = dataMappers;
  }

  void registerHealthChecks(@Observes StartupEvent ignored) {
    // @formatter:off
    healthChecks.stream()
        .forEach(healthCheck -> registerHealthCheck(
            healthCheck,
            healthCheck.call().getName(),
            this::healthCheckToIntForUp,
            this::healthCheckToIntForDown,
            registry,
            dataMappers));
    // @formatter:on
  }

  private void registerHealthCheck(HealthCheck check, String name,
      ToDoubleFunction<HealthCheck> upMapper, ToDoubleFunction<HealthCheck> downMapper,
      MeterRegistry registry, Instance<HealthResponseDataMapper<?>> dataMappers) {
    registerHealthCheck(check, name, "UP", upMapper, registry);
    registerHealthCheck(check, name, "DOWN", downMapper, registry);
    registerCheckData(check, name, registry, dataMappers);
  }

  private static void registerHealthCheck(HealthCheck check, String name, String status,
      ToDoubleFunction<HealthCheck> mapper, MeterRegistry registry) {
    // @formatter:off
    Gauge.builder(Config.INDIVIDUAL_CHECK_NAME, check, mapper)
        .tag(Config.TAG_CHECK, name)
        .tag(Config.TAG_STATUS, status)
        .strongReference(true)
        .register(registry);
    // @formatter:on
  }

  @CacheResult(cacheName = Config.CACHE_CHECK_NAME)
  public HealthCheckResponse fetchHealthCheckData(@CacheKey HealthCheck check) {
    return check.call();
  }

  private int healthCheckToIntForUp(HealthCheck check) {
    if (fetchHealthCheckData(check).getStatus() == HealthCheckResponse.Status.UP) {
      return 1;
    } else {
      return 0;
    }
  }

  private int healthCheckToIntForDown(HealthCheck check) {
    if (fetchHealthCheckData(check).getStatus() == HealthCheckResponse.Status.DOWN) {
      return 1;
    } else {
      return 0;
    }
  }

  private void registerCheckData(HealthCheck check, String name, MeterRegistry registry,
      Instance<HealthResponseDataMapper<?>> dataMappers) {
    Map<String, Object> checkData = fetchHealthCheckData(check).getData().orElse(Map.of());
    Map<String, Boolean> isUnmapped = new HashMap<>();
    for (HealthResponseDataMapper<?> dataMapper : dataMappers) {
      // @formatter:off
      checkData.entrySet().stream()
          .filter(entry -> isUnmapped.getOrDefault(entry.getKey(), true))
          .filter(entry -> dataMapper.keyFilterPattern().matcher(entry.getKey()).matches())
          .filter(entry -> dataMapper.mappableType().isInstance(entry.getValue()))
          .filter(entry -> dataMapper.valueMappable(entry.getValue()))

          // We know that isUnmapped.get(entry.getKey()) is null, thus
          // isUnmapped.put(entry.getKey(), false) will return null
          // We use this trick to avoid calling ".peek(...)" on the stream.
          .filter(entry -> Objects.isNull(isUnmapped.put(entry.getKey(), false)))

          .map(entry -> createGaugeBuildersForCheck(name, entry.getKey(), check, dataMapper))
          .flatMap(Collection::stream)
          .forEach(builder -> builder.register(registry));
      // @formatter:on
    }
  }

  private static List<Gauge.Builder<HealthCheck>> createGaugeBuildersForCheck(String checkName,
      String dataName, HealthCheck check, HealthResponseDataMapper<?> mapper) {
    String checkDataName = "%s-%s".formatted(checkName, dataName);
    return List.of(
        createGaugeMapperForCheck(checkDataName, check, "UP", mapper.checkUpMapper(dataName)),
        createGaugeMapperForCheck(checkDataName, check, "DOWN", mapper.checkDownMapper(dataName)));
  }

  private static Gauge.Builder<HealthCheck> createGaugeMapperForCheck(String checkName,
      HealthCheck check, String status, ToDoubleFunction<HealthCheck> mapper) {
    // @formatter:off
    return Gauge.builder(Config.INDIVIDUAL_CHECK_NAME, check, mapper)
        .tag(Config.TAG_CHECK, checkName)
        .tag(Config.TAG_STATUS, status)
        .strongReference(true);
    // @formatter:on
  }
}
