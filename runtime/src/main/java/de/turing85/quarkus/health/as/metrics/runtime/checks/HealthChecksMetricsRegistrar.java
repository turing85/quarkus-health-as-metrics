package de.turing85.quarkus.health.as.metrics.runtime.checks;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.ToDoubleFunction;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;

import de.turing85.quarkus.health.as.metrics.runtime.checks.datamapper.HealthResponseDataMapper;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.runtime.StartupEvent;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;

public class HealthChecksMetricsRegistrar {
  private static final String INDIVIDUAL_CHECK_NAME = "application.health-check";
  private static final String TAG_CHECK = "check";
  private static final String TAG_STATUS = "status";

  // @formatter:off
  void register(
      @Observes StartupEvent ignored,
      MeterRegistry registry,
      @Any Instance<HealthCheck> healthChecks,
      @Any Instance<HealthResponseDataMapper<?>> dataMappers) {
    registerHealthChecks(registry, healthChecks, dataMappers);
  }
  // @formatter:on

  private static void registerHealthChecks(MeterRegistry registry,
      Instance<HealthCheck> healthChecks, Instance<HealthResponseDataMapper<?>> dataMappers) {
    // @formatter:off
    healthChecks.stream()
        .forEach(healthCheck -> registerHealthCheck(
            healthCheck,
            healthCheck.call().getName(),
            HealthChecksMetricsRegistrar::healthCheckToIntForUp,
            HealthChecksMetricsRegistrar::healthCheckToIntForDown,
            registry,
            dataMappers));
    // @formatter:on
  }

  private static void registerHealthCheck(HealthCheck check, String name,
      ToDoubleFunction<HealthCheck> upMapper, ToDoubleFunction<HealthCheck> downMapper,
      MeterRegistry registry, Instance<HealthResponseDataMapper<?>> dataMappers) {
    registerHealthCheck(check, name, "UP", upMapper, registry);
    registerHealthCheck(check, name, "DOWN", downMapper, registry);
    Map<String, Object> checkData = check.call().getData().orElse(Map.of());
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

          .map(entry -> createGaugeBuilders(name, entry.getKey(), check, dataMapper))
          .flatMap(List::stream)
          .forEach(builder -> builder.register(registry));
      // @formatter:on
    }
  }

  private static void registerHealthCheck(HealthCheck check, String name, String status,
      ToDoubleFunction<HealthCheck> mapper, MeterRegistry registry) {
    // @formatter:off
    Gauge.builder(INDIVIDUAL_CHECK_NAME, check, mapper)
        .tag(TAG_CHECK, name)
        .tag(TAG_STATUS, status)
        .strongReference(true)
        .register(registry);
    // @formatter:on
  }

  private static int healthCheckToIntForUp(HealthCheck check) {
    if (check.call().getStatus() == HealthCheckResponse.Status.UP) {
      return 1;
    } else {
      return 0;
    }
  }

  private static int healthCheckToIntForDown(HealthCheck check) {
    if (check.call().getStatus() == HealthCheckResponse.Status.DOWN) {
      return 1;
    } else {
      return 0;
    }
  }

  private static List<Gauge.Builder<HealthCheck>> createGaugeBuilders(String checkName,
      String dataName, HealthCheck check, HealthResponseDataMapper<?> mapper) {
    String checkDataName = "%s-%s".formatted(checkName, dataName);
    return List.of(createGaugeMapper(checkDataName, check, "UP", mapper.upMapper(dataName)),
        createGaugeMapper(checkDataName, check, "DOWN", mapper.downMapper(dataName)));
  }

  private static Gauge.Builder<HealthCheck> createGaugeMapper(String checkName, HealthCheck check,
      String status, ToDoubleFunction<HealthCheck> mapper) {
    // @formatter:off
    return Gauge.builder(INDIVIDUAL_CHECK_NAME, check, mapper)
        .tag(TAG_CHECK, checkName)
        .tag(TAG_STATUS, status)
        .strongReference(true);
    // @formatter:on
  }
}
