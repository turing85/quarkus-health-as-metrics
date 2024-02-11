package de.turing85.quarkus.health.as.metrics.runtime;

import java.util.Objects;
import java.util.Set;
import java.util.function.ToDoubleFunction;

import jakarta.enterprise.event.Observes;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.common.annotation.Identifier;
import io.smallrye.health.SmallRyeHealth;
import io.smallrye.health.SmallRyeHealthReporter;

public class HealthMetricsRegistrar {
  public static final String CUSTOM_HEALTH_GROUPS_BEAN_NAME = "customHealthGroups";
  private static final String CHECK_NAME = "application.status";
  private static final String TAG_GROUP = "group";
  private static final String TAG_STATUS = "status";

  // @formatter:off
  void register(
      @Observes StartupEvent ignored,
      MeterRegistry registry,
      SmallRyeHealthReporter healthReporter,

      @Identifier(CUSTOM_HEALTH_GROUPS_BEAN_NAME)
      @SuppressWarnings("CdiInjectionPointsInspection")
      Set<String> customGroups) {
    registerDefaultGroups(registry, healthReporter);
    registerCustomGroups(registry, healthReporter, customGroups);
  }
  // @formatter:on

  private static void registerDefaultGroups(MeterRegistry registry,
      SmallRyeHealthReporter healthReporter) {
    registerHealthMetricsToRegistry(healthReporter, registry);
    registerLivenessMetricsToRegistry(healthReporter, registry);
    registerReadinessMetricsToRegistry(healthReporter, registry);
    registerStartupMetricsToRegistry(healthReporter, registry);
    registerWellnessMetricsToRegistry(healthReporter, registry);
  }

  private static void registerCustomGroups(MeterRegistry registry,
      SmallRyeHealthReporter healthReporter, Set<String> additionalHealthGroups) {
    // @formatter:off
    additionalHealthGroups
        .forEach(group -> registerCustomGroup(healthReporter, group, registry));
    // @formatter:on
  }

  private static void registerHealthMetricsToRegistry(SmallRyeHealthReporter reporter,
      MeterRegistry registry) {
    // @formatter:off
    registerReporters(
        reporter,
        "health",
        r -> statusToIntForUp(r.getHealth()),
        r -> statusToIntForDown(r.getHealth()),
        registry);
    // @formatter:on
  }

  private static void registerLivenessMetricsToRegistry(SmallRyeHealthReporter reporter,
      MeterRegistry registry) {
    // @formatter:off
    registerReporters(
        reporter,
        "live",
        r -> statusToIntForUp(r.getLiveness()),
        r -> statusToIntForDown(r.getLiveness()),
        registry);
    // @formatter:on
  }

  private static void registerReadinessMetricsToRegistry(SmallRyeHealthReporter reporter,
      MeterRegistry registry) {
    // @formatter:off
    registerReporters(
        reporter,
        "ready",
        r -> statusToIntForUp(r.getReadiness()),
        r -> statusToIntForDown(r.getReadiness()),
        registry);
    // @formatter:on
  }

  private static void registerStartupMetricsToRegistry(SmallRyeHealthReporter reporter,
      MeterRegistry registry) {
    // @formatter:off
    registerReporters(
        reporter,
        "startup",
        r -> statusToIntForUp(r.getStartup()),
        r -> statusToIntForDown(r.getStartup()),
        registry);
    // @formatter:on
  }

  private static void registerWellnessMetricsToRegistry(SmallRyeHealthReporter reporter,
      MeterRegistry registry) {
    // @formatter:off
    registerReporters(
        reporter,
        "well",
        r -> statusToIntForUp(r.getWellness()),
        r -> statusToIntForDown(r.getWellness()),
        registry);
    // @formatter:on
  }

  private static void registerCustomGroup(SmallRyeHealthReporter reporter, String groupName,
      MeterRegistry registry) {
    if (Objects.nonNull(reporter.getHealthGroup(groupName))) {
      // @formatter:off
      registerReporters(
          reporter,
          groupName,
          r -> statusToIntForUp(r.getHealthGroup(groupName)),
          r -> statusToIntForDown(r.getHealthGroup(groupName)),
          registry);
      // @formatter:on
    }
  }

  private static int statusToIntForUp(SmallRyeHealth health) {
    return health.isDown() ? 0 : 1;
  }

  private static int statusToIntForDown(SmallRyeHealth health) {
    return health.isDown() ? 1 : 0;
  }

  private static void registerReporters(SmallRyeHealthReporter reporter, String name,
      ToDoubleFunction<SmallRyeHealthReporter> upMapper,
      ToDoubleFunction<SmallRyeHealthReporter> downMapper, MeterRegistry registry) {
    registerReporter(reporter, name, "UP", upMapper, registry);
    registerReporter(reporter, name, "DOWN", downMapper, registry);
  }

  private static void registerReporter(SmallRyeHealthReporter reporter, String name,
      String statusTag, ToDoubleFunction<SmallRyeHealthReporter> mapper, MeterRegistry registry) {
    // @formatter:off
    Gauge.builder(CHECK_NAME, reporter, mapper)
        .tag(TAG_GROUP, name)
        .tag(TAG_STATUS, statusTag)
        .strongReference(true)
        .register(registry);
    // @formatter:on
  }
}
